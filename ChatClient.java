import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.channels.*;
import java.nio.charset.*;

public class ChatClient {

    // Variaveis relacionadas com a interface grafica --- * NAO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variaveis relacionadas coma interface grafica

    // Se for necessario adicionar variaveis ao objecto ChatClient, devem
    // ser colocadas aqui
    private Socket clientSocket;
    private String server;
    private int port;

    static private final Charset charset = Charset.forName("UTF-8");
  	static private final CharsetEncoder encoder = charset.newEncoder();

    // Metodo a usar para acrescentar uma string a caixa de texto
    // * NAO MODIFICAR *
    public void printMessage(String message) {
      message = message.replace("\n","");
      String[] tokens = message.split(" ");
      switch(tokens[0]){
        case "MESSAGE":{
          //removing the info from the message
          message = message.replaceFirst("MESSAGE","").replaceFirst(tokens[1],"");
          message = tokens[1] + ":" + message;
          break;
        }
        case "NEWNICK":{
          message = tokens[1] + " changed his nickname to: " + tokens[2];
          break;
        }
        case "JOINED":{
          message = tokens[1] + " joined the room";
          break;
        }
        case "LEFT":{
          message = tokens[1] + " left the room";
          break;
        }
        case "PRIVATE":{
          message = message.replaceFirst("PRIVATE","").replaceFirst(tokens[1],"");
          message = tokens[1] + ":" + message;
          break;
        }
      }
      System.out.println("PRINTING: " + message);
      chatArea.append(message + "\n");
    }

    // Construtor
    public ChatClient(String server, int port) throws IOException {

        // Inicializacao da interface grafica --- * NAO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                   chatBox.setText("");
                }
            }
        });
        // --- Fim da inicializacao da interface grafica

        // Se for necessario adicionar codigo de inicializacao ao
        // construtor, deve ser colocado aqui
        this.server = (InetAddress.getByName(server)).getHostAddress();
        this.port = port;

    }

    // Metodo invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        // PREENCHER AQUI com codigo que envia a mensagem ao servidor
        message = message.trim(); //so it doesn't send extra spaces
        System.out.println("SENDING: " + message);
        DataOutputStream dataToServer = new DataOutputStream(clientSocket.getOutputStream());
        dataToServer.write((message + "\n").getBytes("UTF-8"));
    }

    // Metodo principal do objecto
    public void run() throws IOException {
        // PREENCHER AQUI
        clientSocket = new Socket(server, port);
        new Thread(new Listener()).start();
    }

    // Instancia o ChatClient e arranca-o invocando o seu metodo run()
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }

    class Listener implements Runnable {
      public Listener(){

      }

      public void run(){
        try{
          BufferedReader dataFromServer;
          boolean alive = true;

          while(alive){
            dataFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message = dataFromServer.readLine() + "\n"; //readLine removes \n
            System.out.println("RECEIVED: " + message);

            if(message.compareTo("BYE\n") == 0){
              alive = false;
            }

            printMessage(message);
          }
          clientSocket.close();
          System.exit(0); //to close the window
        } catch(Exception e){
          e.printStackTrace();
        }

      }

    }

}
