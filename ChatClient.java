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
    private SocketChannel socketChannel;
    static private final Charset charset = Charset.forName("UTF8");
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
        try {
          writeText("Connecting to " + server + ":" + port + "......");
          socketChannel = SocketChannel.open();
          socketChannel.connect(new InetSocketAddress(server, port));
          if(socketChannel.isConnected()){
            writeText("Successfully connected!");
          }
        } catch(IOException e){
          writeText(e.getMessage());
        }
    }


    // Metodo invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        // PREENCHER AQUI com codigo que envia a mensagem ao servidor
        ByteBuffer buf = encoder.encode(CharBuffer.wrap(message));
        while(buf.hasRemaining()) {
          socketChannel.write(buf);
        }

    }


    // Metodo principal do objecto
    public void run() throws IOException {
        // PREENCHER AQUI
        chatArea.append("/nick to define a nickname");
        // TODO

        int bytesRead = 0;
        while(bytesRead >= 0) {
          ByteBuffer buf = ByteBuffer.allocate(16384); //2^14
          bytesRead = socketChannel.read(buf);
          writeText(buf.toString());  ///////////////////////////////////////
        }
    }


    // Instancia o ChatClient e arranca-o invocando o seu metodo run()
    // * NAO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }

    private void writeText(String text) {
      chatArea.setText(chatArea.getText() + text + "\n");
    }

}
