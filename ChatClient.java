import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.*;
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
    static private final Charset charset = Charset.forName("UTF-8");
  	static private final CharsetEncoder encoder = charset.newEncoder();

    // Metodo a usar para acrescentar uma string a caixa de texto
    // * NAO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
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


    }


    // Metodo invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        // PREENCHER AQUI com codigo que envia a mensagem ao servidor
        DataOutputStream dataToServer = new DataOutputStream(clientSocket.getOutputStream());
        dataToServer.write((message).getBytes("UTF-8"));
    }


    // Metodo principal do objecto
    public void run() throws IOException {
        // PREENCHER AQUI
        //chatArea.append("/nick to define a nickname\n");
        clientSocket = new Socket(server, port);
        new Thread(new Listener()).start();
    }


    // Instancia o ChatClient e arranca-o invocando o seu metodo run()
    // * NAO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
}

private class Listener implements Runnable {
  public Listener(){

  }

  public void run(){
    try{
      BufferedReader dataFromServer;
      boolean alive = true;

      while(alive){
        dataFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String message = dataFromServer.readLine() + "\n"; //readLine removes \n

        if(message.compareTo("BYE\n") == 0){
          alive = false;
        }

        printMessage(message);
      }
      clientSocket.close();
      System.exit(0); //to close the window
    } catch(Exception e){

    }
    
  }

}
