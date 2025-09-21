package gang.gang.net;
import gang.gang.entity.Message;
import gang.gang.entity.MessageType;
import gang.gang.entity.User;
import gang.gang.protocol.Parser;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Scanner;

public class Client {

    private Socket socket;
    private BufferedReader bufferReader;
    private BufferedWriter bufferWriter;
    private User user;

    public Client(Socket socket, User user) {
        try {
            this.socket = socket;
            this.user = user;
            this.bufferWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            closeEverything(socket,bufferReader,bufferWriter);
        }
    }
    public void sendMessage() {
        try {
            Scanner scanner = new Scanner(System.in);
            //sender username til ClientHandler
            bufferWriter.write(user.getUsername());
            bufferWriter.newLine();
            bufferWriter.flush();

//            String chosenRoom = scanner.nextLine(); // rå input
//            bufferWriter.write(chosenRoom);
//            bufferWriter.newLine();
//            bufferWriter.flush();


            while (socket.isConnected()) {
                String messageToSend = scanner.nextLine();

                Message message = new Message(user.getUsername(), LocalDateTime.now(), MessageType.TEXT, messageToSend);

                String formattedMessage = Parser.formatToProtocol(message);
                //ville nok være her vores logik fra message klasse ville blive brugt
                //sender den besked brugeren har indtastet til ClientHandler som så Broadcaster til de andre brugere
                bufferWriter.write(formattedMessage); // Formatere det et andet sted
                bufferWriter.newLine();
                bufferWriter.flush();
            }
        } catch (IOException e) {
            closeEverything(socket,bufferReader,bufferWriter);
        }
    }
    //en block operation IG, så den får sin egen thread så resten ikke holder stille mens den venter på den her
    //så du stadig selv kan sende beskeder men dens også venter på beskeder fra andre brugere
    public void listenForMessage(){
        Thread thread = new Thread(new Runnable() { // Opførte sig funky, virker nu, men ved ikke hvorfor
            @Override
            public void run() {
                String msgFromGroupChat;

                while (socket.isConnected()) {
                    try {
                        while (socket.isConnected() && (msgFromGroupChat = bufferReader.readLine()) != null) {

                            Message message = Parser.parseFromProtocol(msgFromGroupChat);

                            if (message != null) {
                                System.out.println(Parser.formatForDisplay(message));
                            } else {
                                // Hvis ikke parsebar, vis rå tekst (fx room prompts)
                                System.out.println(msgFromGroupChat);
                            }
                        }
                    } catch (IOException e) {
                        closeEverything(socket,bufferReader,bufferWriter);
                        break;
                    }
                }
            }
            //starter den så den kører, noget alla thread.start i serveren
        }); thread.start();
    }
    public void closeEverything(Socket socket, BufferedReader bufferReader, BufferedWriter bufferWriter) {
        try {
            if (bufferReader != null) {
                bufferReader.close();
            }
            if (bufferWriter != null) {
                bufferWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your username for the group chat: ");

        //her vi sætter brugerens username
        String username = scanner.nextLine();
        User user = new User();
        user.setUsername(username);

        Socket socket = new Socket ("localhost", 5556);
        Client client = new Client(socket, user);
        //begge er "blocking operations" fordi de har infinite while-loops, men de er hver deres thread så ja :D
        client.listenForMessage();
        client.sendMessage();
    }

}

