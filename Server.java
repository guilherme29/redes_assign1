import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;


enum State {
  init, outside, inside
}
class User{
  private String nickname = "";
  private State state = State.init;
  private Room room;
  private String message = ""; //i need this to avoid responding to Ctrl + D on netcat

  User(){
    this.state = State.init;
  }

  String getNickname(){
    return this.nickname;
  }
  State getState(){
    return this.state;
  }
  Room getRoom(){
    return this.room;
  }
  String getMessage(){
    return message;
  }

  void setNickname(String nickname){
    this.nickname = nickname;
  }
  void setState(State state){
    this.state = state;
  }
  void setRoom(Room room){
    this.room = room;
  }
  void cleanMessage(){
    this.message = "";
  }
  void addMessage(String message){
    this.message = this.message + message;
  }

  boolean isInRoom(){
    return this.getRoom() != null ? true : false;
  }

}

class Room{
  private String name = "";
  private HashSet<SelectionKey> users = new HashSet<>();

  Room(String name){
    this.name = name;
  }
  HashSet<SelectionKey> getUsers(){
    return users;
  }
  String getName(){
    return name;
  }
  void addUser(SelectionKey key){
    users.add(key);
  }
  void removeUser(SelectionKey key){
    users.remove(key);
  }
  boolean isEmpty(){
    return users.isEmpty();
  }
}

public class ChatServer
{
  // A pre-allocated buffer for the received data
  static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );

  // Decoder for incoming text -- assume UTF-8
  static private final Charset charset = Charset.forName("UTF8");
  static private final CharsetDecoder decoder = charset.newDecoder();
  static private final CharsetEncoder encoder = charset.newEncoder();

  static private HashSet<Room> rooms = new HashSet<>();
  static private HashSet<SelectionKey> users = new HashSet<>();

  static private Selector selector;

  static public void main( String args[] ) throws Exception {
    // Parse port from command line
    int port = Integer.parseInt( args[0] );

    try {
      // Instead of creating a ServerSocket, create a ServerSocketChannel
      ServerSocketChannel ssc = ServerSocketChannel.open();

      // Set it to non-blocking, so we can use select
      ssc.configureBlocking( false );

      // Get the Socket connected to this channel, and bind it to the
      // listening port
      ServerSocket ss = ssc.socket();
      InetSocketAddress isa = new InetSocketAddress( port );
      ss.bind( isa );

      // Create a new Selector for selecting
      /*Selector*/ selector = Selector.open();

      // Register the ServerSocketChannel, so we can listen for incoming
      // connections
      ssc.register( selector, SelectionKey.OP_ACCEPT );
      //System.out.println( "Listening on port " + port );

      while (true) {
        // See if we've had any activity -- either an incoming connection,
        // or incoming data on an existing connection
        int num = selector.select();

        // If we don't have any activity, loop around and wait again
        if (num == 0) {
          continue;
        }

        // Get the keys corresponding to the activity that has been
        // detected, and process them one by one
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
          // Get a key representing one of bits of I/O activity
          SelectionKey key = it.next();

          // What kind of activity is it?
          if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {

            // It's an incoming connection.  Register this socket with
            // the Selector so we can listen for input on it
            Socket s = ss.accept();
            //System.out.println( "Got connection from "+s );

            // Make sure to make it non-blocking, so we can use a selector
            // on it.
            SocketChannel sc = s.getChannel();
            sc.configureBlocking( false );

            // Register it with the selector, for reading
            sc.register( selector, SelectionKey.OP_READ );


          }
          else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {

            SocketChannel sc = null;

            try {

              // Registering a new user
              if(key.attachment() == null){
                key.attach(new User());
                users.add(key);
              }

              // It's incoming data on a connection -- process it
              boolean ok = processInput( key );
              sc = (SocketChannel) key.channel();

              // If the connection is dead, remove it from the selector
              // and close it
              if (!ok) {
                removeUser(key);
                key.cancel();

                Socket s = null;
                try {
                  s = sc.socket();
                  //System.out.println( "Closing connection to "+s );
                  s.close();
                } catch( IOException ie ) {
                  //System.err.println( "Error closing socket "+s+": "+ie );
                }
              }

            } catch( IOException ie ) {

              // On exception, remove this channel from the selector
              key.cancel();

              try {
                sc.close();
              } catch( IOException ie2 ) {
                //System.out.println( ie2 );
              }

              //System.out.println( "Closed " + sc );
            }
          }
        }

        // We remove the selected keys, because we've dealt with them.
        keys.clear();
      }
    } catch( IOException ie ) {
      //System.err.println( ie );
    }
  }


  // Just read the message from the socket and send it to stdout
  static private boolean processInput(SelectionKey key) throws IOException {
    SocketChannel sc = (SocketChannel) key.channel();
    // Read the message to the buffer
    buffer.clear();
    sc.read( buffer );
    buffer.flip();

    // If no data, close the connection
    if (buffer.limit()==0) {
      return false;
    }

    String message = decoder.decode(buffer).toString();
    //System.out.println("RECEIVED MESSAGE: " + message);

    //checking if it's a \n or a Ctrl+D
    User auxUser = (User) key.attachment();
    message = auxUser.getMessage() + message;
    if(!message.endsWith("\n")){
      auxUser.addMessage(message);
      return true;
    }

    if(message.startsWith("/nick ")){
      String nick = message.substring(6);//6 = "/nick ".length()
      nick = nick.replace("\n", "");
      nick(key, nick);
    }
    else if(message.startsWith("/join ")){
      String room = message.substring(6);//6 = "/join ".length()
      room = room.replace("\n","");
      join(key, room);
    }
    else if(message.startsWith("/leave")){
      leave(key);
    }
    else if(message.startsWith("/bye")){
      bye(key);
    }
    else if(message.startsWith("/priv ")){
      try{
        String aux = message.substring(6);
        String user = "";
        int i;
        for(i=0; aux.charAt(i)!=' '; i++){
          user = user + aux.charAt(i);
        }
        message = aux.substring(i + 1);
        message = message.replace("\n","");
        priv(key, user, message);
      } catch(StringIndexOutOfBoundsException e){
        send(key, "ERROR");
      }
    }
    else if(message.startsWith("/help")){
      help(key);
    }
    else {
      message = message.replace("\n","");
      message(key, message);
    }

    auxUser.cleanMessage();
    return true;
  }


  static private void nick(SelectionKey key, String nick) throws IOException{
    User user = (User) key.attachment();

    //checking for users with the same nickname
    for(SelectionKey aux : users){
      User auxUser = (User) aux.attachment();
      if(auxUser.getNickname().compareTo(nick) == 0){
        send(key, "ERROR");
        return;
      }
    }

    //checking if the user is in a room
    if(user.getState() == State.inside){
      String oldnick = user.getNickname();
      String message = "NEWNICK " + oldnick + " " + nick;
      Room room = user.getRoom();
      sendToOthersInRoom(room, key, message);
    }
    else {
      user.setState(State.outside);
    }
    user.setNickname(nick);
    //key.attach(user);
    send(key, "OK");
  }

  static private void join(SelectionKey key, String room) throws IOException{
    User user = (User) key.attachment();
    if(user.getState() == State.init){
      send(key, "ERROR");
      return;
    }
    Room auxRoom = user.getRoom();
    if(auxRoom != null){
      if(auxRoom.getName().compareTo(room) == 0){
        //if the user is already in the room itself
        send(key, "ERROR");
        return;
      }
    }
    Room roomy = null;
    for(Room aux : rooms){
      if(aux.getName().compareTo(room) == 0){
        roomy = aux;
        break;
      }
    }
    if(user.isInRoom()){
      leave(key);
    }
    if(roomy != null){ //room already exists
      user.setRoom(roomy);
      roomy.addUser(key);
    }
    else { //room doesn't exist
      roomy = new Room(room);
      user.setRoom(roomy);
      roomy.addUser(key);
      rooms.add(roomy);
    }
    send(key, "OK");
    sendToOthersInRoom(roomy, key, "JOINED " + user.getNickname());
    user.setState(State.inside);

  }

  static private void leave(SelectionKey key) throws IOException {
    User user = (User) key.attachment();
    if(user.getState() != State.inside){
      send(key, "ERROR");
      return;
    }
    Room room = user.getRoom();
    room.removeUser(key);
    user.setRoom(null);
    user.setState(State.outside);
    //deleting the room if it becomes empty
    if(room.isEmpty()){
      rooms.remove(room);
    }
    else{
      sendToRoom(room, "LEFT " + user.getNickname());
    }
    send(key, "OK");
  }

  static private void leaveForBye(SelectionKey key) throws IOException {
    User user = (User) key.attachment();
    if(user.getState() != State.inside){
      return;
    }
    Room room = user.getRoom();
    room.removeUser(key);
    user.setRoom(null);
    user.setState(State.outside);
    //deleting the room if it becomes empty
    if(room.isEmpty()){
      rooms.remove(room);
    }
    else{
      sendToRoom(room, "LEFT " + user.getNickname());
    }
  }

  static private void bye(SelectionKey key) throws IOException {
    User user = (User) key.attachment();
    if(user.getState() == State.inside){
      leaveForBye (key);
    }
    send(key, "BYE");
    users.remove(key);
    SocketChannel sc = (SocketChannel) key.channel();
    Socket s = null;
    try {
      s = sc.socket();
      //System.out.println( "Closing connection to "+s );
      s.close();
    } catch(IOException e){
      //System.err.println( "Error closing socket "+s+": "+e );
    }
  }

  static private void removeUser(SelectionKey key) throws IOException{
    //this function is to be used when the connection is lost
    //to remove the user from the "database" and inform other users
    User user = (User) key.attachment();
    if(user.getState() == State.inside){
      Room room = user.getRoom();
      room.removeUser(key);
      //deleting the room if it becomes empty
      if(room.isEmpty()){
        rooms.remove(room);
      }
      else{
        sendToRoom(room, "LEFT " + user.getNickname());
      }
    }
    users.remove(key);
  }

  static private void priv(SelectionKey key, String user, String message) throws IOException{
    for(SelectionKey auxKey : users){
      User recipientUser = (User) auxKey.attachment();

      if(recipientUser.getNickname().compareTo(user) == 0){

        User senderUser = (User) key.attachment();
        send(auxKey , "PRIVATE " + senderUser.getNickname() + " " + message);
        send(key    , "PRIVATE " + senderUser.getNickname() + " " + message);
        return;

      }
    }
    send(key, "ERROR");
  }

  static private void help(SelectionKey key) throws IOException{
    SocketChannel sc = (SocketChannel) key.channel();
    String message =    "/nick nickname      - to add a nickname\n";
    message = message + "/join room          - to join a room\n";
    message = message + "/leave              - to leave the room\n";
    message = message + "/priv user message  - to send a private message\n";
    message = message + "/bye                - to disconnect from the server";
    send(key, message);
  }

  static private void message(SelectionKey key, String message) throws IOException {
    User user = (User) key.attachment();
    if(user.getState() != State.inside){
      send(key, "ERROR");
      return;
    }
    if(message.charAt(0) == '/'){
      message = "/" + message;
    }
    message = "MESSAGE " + user.getNickname() + " " + message;
    sendToRoom(user.getRoom(), message);
  }

  static private void send(SelectionKey key, String message) throws IOException {
    message = message + "\n";
    SocketChannel sc = (SocketChannel) key.channel();
    sc.write(encoder.encode(CharBuffer.wrap(message)));
  }

  static private void sendToRoom(Room room, String message) throws IOException {
    for(SelectionKey user : room.getUsers()){
        send(user, message);
    }
  }

  static private void sendToOthersInRoom(Room room, SelectionKey exception, String message) throws IOException {
    for(SelectionKey user : room.getUsers()){
      if(user == exception){
        continue;
      }
      send(user, message);
    }
  }

}
