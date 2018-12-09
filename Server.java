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
  String nickname = "";
  State state = State.init;
  Room room;
  String message = ""; //i need this to avoid responding to Ctrl + D on netcat

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

  void setNickname(String nickname){
    this.nickname = nickname;
  }
  void setState(State state){
    this.state = state;
  }
  void setRoom(Room room){
    this.room = room;
  }

  boolean isInRoom(){
    return this.getRoom() != null ? true : false;
  }

}

class Room{
  String name = "";
  HashSet<SelectionKey> users = new HashSet<>();

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
}

public class Server
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
      System.out.println( "Listening on port " + port );

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
            System.out.println( "Got connection from "+s );

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
                key.cancel();

                Socket s = null;
                try {
                  s = sc.socket();
                  System.out.println( "Closing connection to "+s );
                  s.close();
                } catch( IOException ie ) {
                  System.err.println( "Error closing socket "+s+": "+ie );
                }
              }

            } catch( IOException ie ) {

              // On exception, remove this channel from the selector
              key.cancel();

              try {
                sc.close();
              } catch( IOException ie2 ) {
                System.out.println( ie2 );
              }

              System.out.println( "Closed " + sc );
            }
          }
        }

        // We remove the selected keys, because we've dealt with them.
        keys.clear();
      }
    } catch( IOException ie ) {
      System.err.println( ie );
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
    /*
    if(!userMessage.containsKey(sc)){
      userMessage.put(sc,"");
    }
    */

    String message = decoder.decode(buffer).toString();
    System.out.println("RECEIVED MESSAGE: " + message);
    /*
    if(!message.endsWith("\n")){ //to avoid sending messages with Ctrl + D on netcat
      String restOfMessage = userMessage.get(sc);
      restOfMessage = restOfMessage + message;
      return true;
    }
    else{
      String restOfMessage = userMessage.get(sc);
      message = restOfMessage + message;
      //message.trim();
    }
    */
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
      /*
      leave(sc);
      */
    }
    else if(message.startsWith("/bye")){
      /*
      bye(sc);
      */
    }
    else if(message.startsWith("/priv")){
      /*
      String aux = message.substring(6);
      String user = "";
      int i;
      for(i=0; aux.charAt(i)!=' '; i++){
        user = user + aux.charAt(i);
      }
      user = user + "\n";  ////////////TODO arranjar usernames (estao a acabar com \n e nao deviam)
      message = message.substring(i + 1);
      priv(sc, user, message);
      System.out.println("->" + user);
      System.out.println("->" + message);
      System.out.println("->" + users.get(sc));
      */
    }
    else {
      /*
      message(sc, message);
      */
    }

    return true;
  }


  static private void nick(SelectionKey key, String nick) throws IOException{
    User user = (User) key.attachment();

    //checking for users with the same nickname
    for(SelectionKey aux : users){
      User auxUser = (User) aux.attachment();
      if(auxUser.getNickname().compareTo(nick) == 0){
        send(key, "ERROR - nickname already in use");
        return;
      }
    }

    //checking if the user is in a room
    if(user.getState() == State.inside){
      String oldnick = user.getNickname();
      String message = "NEWNICK " + oldnick + " " + nick;
      Room room = user.getRoom();
      //sendToOthers()                                      TODO
      //send()                                              TODO
    }
    else {
      user.setState(State.outside);
    }
    user.setNickname(nick);
    key.attach(user);
    send(key, "OK - your nickname is now: " + nick);
  }

  static private void join(SelectionKey key, String room) throws IOException{
    User user = (User) key.attachment();
    if(user.getState() == State.init){
      send(key, "ERROR - no nickname defined");
      return;
    }
    Room roomy = null;
    for(Room aux : rooms){
      if(aux.getName().compareTo(room) == 0){
        roomy = aux;
        break;
      }
    }
    if(user.isInRoom()){
      //                                                    TODO leave
    }
    if(roomy != null){ //room already exists
      user.setRoom(roomy);
      roomy.addUser(key);
                                                    //  TODO ver se preciso de voltar a dar attach ao user
    }
    else { //room doesn't exist
      roomy = new Room(room);
      roomy.addUser(key);
      rooms.add(roomy);
      send(key, "OK - you're now in " + room);
    }
    sendToOthersInRoom(roomy, key, "JOINED " + user.getNickname());
    user.setState(State.inside);

    /*
    State userState = states.get(sc);
    if(userState == State.init){
      send(sc, "ERROR - no nickname defined");
      return;
    }
    if(!rooms.containsKey(room)){ //if the room doesn't exist yet
      Set<SocketChannel> set = new HashSet<SocketChannel>();
      rooms.put(room, set);
    }

    //removing user from current room
    leave(sc);

    //adding user to other room
    Set<SocketChannel> usersInRoom = rooms.get(room);
    usersInRoom.add(sc);
    send(sc, "OK - you're now in " + room);
    userRoom.put(sc, room); //adding the user to the room
    states.put(sc, State.inside); //changing the state
    rooms.put(room, usersInRoom); //adding this room to the list of rooms

    String welcomeMessage = "JOINED " + users.get(sc);
    sendSetOthers(usersInRoom, sc, welcomeMessage);
    */
  }

  static private void leave(SocketChannel sc) throws IOException {
    /*
    if(states.get(sc) == State.outside){ //in case it's not in a room
      return;
    }

    String room = userRoom.get(sc);
    Set<SocketChannel> set = rooms.get(room);
    set.remove(sc);
    userRoom.remove(sc);

    if(set.size() == 0){ //if the room becomes empty, deletes it
      rooms.remove(room);
    }
    else {
      String goodbyeMessage = "LEFT " + users.get(sc);
      sendSet(set, goodbyeMessage);
    }
    states.put(sc, State.outside);
    */
  }

  static private void bye(SocketChannel sc) throws IOException {
    /*
    if(states.get(sc) == State.inside){ //leaves the room
      leave(sc);
    }
    send(sc, "BYE");

    users.remove(sc);
    states.remove(sc);

    try {
      sc.close();
    } catch(IOException e) {
      System.err.println(e);
    }

    System.out.println( "Closed " + sc );
    */
  }

  static private void priv(SocketChannel sc, String user, String message) throws IOException{
    /*
    SocketChannel destiny = null;
    boolean flag = true;
    Iterator it = users.entrySet().iterator();
    while(it.hasNext()){
      Map.Entry pair = (Map.Entry) it.next();
      if(users.get(pair.getKey()).compareTo(user) == 0){
        destiny = (SocketChannel) pair.getKey();
        flag = false;
        break;
      }
    }
    if(flag){
      send(sc, "ERROR - user not found");
      return;
    }
    message = "PRIV " + users.get(sc) + " " + message;
    send(destiny, message);
    */
  }

  static private void message(SocketChannel sc, String message) throws IOException {
    /*
    if(states.get(sc) != State.inside) { //if user isn't inside a room
      send(sc, "ERROR - you aren't inside a room");
      return;
    }
    String room = userRoom.get(sc);
    Set<SocketChannel> roomSet = rooms.get(room);
    String alfa = "MESSAGE " + users.get(sc) + " " + message;
    sendSet(roomSet, alfa);
    */
  }

  static private void send(SelectionKey key, String message) throws IOException {
    message = message + "\n";
    SocketChannel sc = (SocketChannel) key.channel();
    sc.write(encoder.encode(CharBuffer.wrap(message)));
  }

  static private void sendRoom(Room room, String message) throws IOException {
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
