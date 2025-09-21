package gang.gang.service;
import gang.gang.entity.Message;
import gang.gang.entity.MessageType;
import gang.gang.entity.User;
import gang.gang.protocol.Parser;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

public class ClientHandler implements Runnable {

    //holder styr på vores brugere, looper igennem når ny besked sendt så alle modtagere beskeden.
    //static fordi den skal tilhører klassen og ikke hvert objekt af klassen

    public static ArrayList<ClientHandler> clientHandler = new ArrayList<>();

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;

    private User user;
    private String roomName;

    private RoomService roomService;
    private MessageService messageService;

    public ClientHandler(Socket socket, RoomService roomService, MessageService messageService) {
        try {
            //gør at vores socket er ligemed hvad bliver sat til i serveren
            this.socket = socket;
            this.roomService = roomService;
            this.messageService = messageService;

            //outputStreamWriter er en character stream
            //getOutputStream er en byte stream
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //sætter clientUsername som modtaget input fra bruger (deres første input)
            String username = bufferedReader.readLine();
            this.user = new User();
            this.user.setUsername(username);

            //tilføjer til arraylisten
//            clientHandler.add(this);
            selectRoom();

            //udskriver en ny bruger et ankommet
//            broadcastMessage("SERVER: " + clientUsername + " has entered the chat!");
            Message welcomeMessage = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO,
                    username + " has entered the chat");
            messageService.sendMessageToRoom(roomName, Parser.formatToProtocol(welcomeMessage), this);
//            broadcastMessage(Parser.formatToProtocol(welcomeMessage));

        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

        private void selectRoom() throws IOException {
            while (true) {
                bufferedWriter.write("Join a room");
                bufferedWriter.newLine();

                //looper igennem rum og udskriver til bruger
                for (Map.Entry<String,Integer> entry : roomService.getRoomStatus().entrySet()) {
                String roomName = entry.getKey();
                int count = entry.getValue();
                bufferedWriter.write(String.format("%s (%d/%d)",roomName, count, roomService.getMaxRoomSize()));
                bufferedWriter.newLine();
                }

                bufferedWriter.write("Enter room name: ");
                bufferedWriter.newLine();
                bufferedWriter.flush();

                String chosenRoom = bufferedReader.readLine();

                System.out.println("DEBUG: chosenRoom = " + chosenRoom);

                if (chosenRoom != null) chosenRoom = chosenRoom.trim();

                if (roomService.addClientToRoom(chosenRoom, this)) {
                    this.roomName = chosenRoom;
                    break;
                }
                else {
                    bufferedWriter.write("Room is full or does not exist, Choose another.");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }

            }
        }




    @Override
    public void run() {
        String messageFromClient;

        try {
            while ((messageFromClient = bufferedReader.readLine()) != null) {
                messageService.sendMessageToRoom(roomName, messageFromClient, this);
            }
        }  catch (IOException e) {
            System.out.println("just testing");
        }  finally {
        // Fjerner klienten fra rummet og sender farvel-besked
        removeClientHandler();
    }


    }
    public void sendMessage(String message) {

            try {
                    bufferedWriter.write(message);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();

            } catch (IOException e) {
                closeEverything(socket,bufferedReader,bufferedWriter);
            }

    }
    //bruger bliver fjernet fra arraylist
    public void removeClientHandler() {
        try {
            Message byebyeMessage = new Message("Server", LocalDateTime.now(), MessageType.SERVER_INFO,
                    user.getUsername() + " has left the chat");
            messageService.sendMessageToRoom(roomName, Parser.formatToProtocol(byebyeMessage), this);


            roomService.removeClientFromRoom(roomName, this);


            closeEverything(socket, bufferedReader, bufferedWriter);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //bruges til at dele clientHandler instans/objekt af user
    public User getUser() {
        return user;
    }

    //sørger for alting lukker, intet unødvendigt åbent, CUSTOM EXCEPTION HANDLING? i believe
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
