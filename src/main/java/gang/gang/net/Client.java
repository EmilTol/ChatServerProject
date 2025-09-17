package gang.gang.net;
import gang.gang.protocol.Parser;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private Socket socket;
    private BufferedReader bufferReader;
    private BufferedWriter bufferWriter;
    private String username;

    public Client(Socket socket, String username) {
        try {
            this.socket = socket;
            this.bufferWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;
        } catch (IOException e) {
            closeEverything(socket,bufferReader,bufferWriter);
        }
    }
    public void sendMessage() {
        try {
            //sender username til ClientHandler
            bufferWriter.write(username);
            bufferWriter.newLine();
            bufferWriter.flush();

            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()) {
                String messageToSend = scanner.nextLine();
                //ville nok være her vores logik fra message klasse ville blive brugt
                //sender den besked brugeren har indtastet til ClientHandler som så Broadcaster til de andre brugere
                bufferWriter.write(messageToSend); // Formatere det et andet sted
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msgFromGroupChat;

                while (socket.isConnected()) {
                    try {
                        //printer hvad der bliver sendt fra serveren, broadcastMessage metoden
                        msgFromGroupChat = bufferReader.readLine();
                        System.out.println(msgFromGroupChat);
                    } catch (IOException e) {
                        closeEverything(socket,bufferReader,bufferWriter);
                    }
                }
            }
            //starter den så den kører, noget alla thread.start i serveren
        }).start();
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
        Socket socket = new Socket ("localhost", 1234);
        Client client = new Client(socket,username);
        //begge er "blocking operations" fordi de har infinite while-loops, men de er hver deres thread så ja :D
        client.listenForMessage();
        client.sendMessage();
    }

}

