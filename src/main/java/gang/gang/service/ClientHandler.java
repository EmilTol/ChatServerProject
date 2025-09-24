package gang.gang.service;
import gang.gang.entity.Message;
import gang.gang.entity.MessageType;
import gang.gang.entity.User;
import gang.gang.protocol.Parser;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.net.ServerSocket;
import java.net.Socket;

import static java.lang.System.in;

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
    private FileService fileService;

    public ClientHandler(Socket socket, RoomService roomService, MessageService messageService) {
        try {
            //gør at vores socket er ligemed hvad bliver sat til i serveren
            this.socket = socket;
            this.roomService = roomService;
            this.messageService = messageService;
            this.fileService = new FileService();

            //outputStreamWriter er en character stream
            //getOutputStream er en byte stream
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //sætter clientUsername som modtaget input fra bruger (deres første input)
//            String username = bufferedReader.readLine();
            this.user = new User();
//            this.user.setUsername(username);

//            //tilføjer til arraylisten
////            clientHandler.add(this);
//            selectRoom();
//
//            //udskriver en ny bruger et ankommet
////            broadcastMessage("SERVER: " + clientUsername + " has entered the chat!");
//            Message welcomeMessage = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO,
//                    username + " has entered the chat");
//            messageService.sendMessageToRoom(roomName, Parser.formatToProtocol(welcomeMessage), this);
////            broadcastMessage(Parser.formatToProtocol(welcomeMessage));

        // blev nødt til at flytte det væk herfra da det blokerede for at andre kunne tilslutte et rum.

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

//messageService.sendMessageToRoom(roomName, messageFromClient, this);


    @Override
    public void run() {
        try {
            String username = bufferedReader.readLine(); // Læser brugerens input, hvilket er username
            if (username == null) {return;}
            this.user.setUsername(username);
            selectRoom(); //Starter selectRoom metode

            //Opretter og sender en velkomstbesked til brugeren
            Message welcomeMessage = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, user.getUsername() + " has entered the chat");
            messageService.sendMessageToRoom(roomName, Parser.formatToProtocol(welcomeMessage), this);

            String messageFromClient;
            while ((messageFromClient = bufferedReader.readLine()) != null) { //Løkke der køre og forsætter så længe klienten er forbundet og sender beskerder
                Message message = Parser.parseFromProtocol(messageFromClient); // Omdanner beskedet til et struktureret besked objekt
                if ( message == null){
                    System.out.println("Modtog ugyldig besked fra: " + user.getUsername());
                    continue;
                }
                switch (message.getMessageType()) { // Sortere beskeder basseret på type
                    case TEXT:
                        messageService.sendMessageToRoom(roomName, messageFromClient, this);
                        break;
                    case EMOJI:
                        messageService.sendMessageToRoom(roomName, messageFromClient, this);
                        break;
                    case FILE_TRANSFER: // Håndtere filoverførsel i en ny tråd, så det ikke fucker med chatten ( den smed brugeren ud hvis man ikke gør det )
                        new Thread(() -> handleFileTransferRequest(message)).start();
                        break;
                    case FILE_DOWNLOAD:
                        if (message.getPayload().equals("LIST_FILES")) {
                            handleListFilesRequest();
                        } else {
                            new Thread(() -> handleFileDownloadRequest(message)).start();
                        }
                        break;

                    default:
                        System.out.println("Modtog ugyldig besked :( " + message.getMessageType());
                        break;
                }
            }
        }  catch (IOException e) {
            String username = (user != null && user.getUsername() != null) ? user.getUsername() : "en ny klient";
            System.out.println("Forbindelsen til " + username + " blev afbrudt.");
        }  finally {
        // Fjerner klienten fra rummet og sender farvel-besked
        removeClientHandler();
    }


    }

    private void handleFileTransferRequest(Message requestMessage) {
        try (ServerSocket tempServerSocket = new ServerSocket(0)) { // Sygt smart, windows giver os en ledig port da vi skriver 0. Så vi behøver ikke tage stilling til det.
            int port = tempServerSocket.getLocalPort(); // tager den port vi får givet af windows
            String[] fileInfo = requestMessage.getPayload().split("\\|"); // deler vores payload i 2, ja, vi gør det 2 steder, fixer på et andet tidspunkt.
            String fileName = fileInfo[0];
            long fileSize = Long.parseLong(fileInfo[1]);

            // Send en "grønt lys" besked tilbage til KUN den anmodende klient
            Message goAheadMessage = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, "START_UPLOAD::" + port);
            this.sendMessage(Parser.formatToProtocol(goAheadMessage));


            tempServerSocket.setSoTimeout(10000); // 10 sekunders timer, som beskriver hvor længe vi vil vente på klienten forbinder til den nye port.

            try (Socket fileSocket = tempServerSocket.accept()) { // Acceptere den nye forbindelse fra klienten
                System.out.println("Modtager fil '" + fileName + "' på midlertidig port " + port);
                fileService.receiveFile(fileSocket.getInputStream(), fileName, fileSize); // smider ansvaret over til fileService

                Message confirmation = new Message(user.getUsername(), LocalDateTime.now(), MessageType.FILE_TRANSFER, fileName + "|" + fileSize); //Semder til hele rummet at filen er sendt
                messageService.sendMessageToRoom(roomName, Parser.formatToProtocol(confirmation), this);
            }

        } catch (IOException e) {
            System.out.println("Fejl under oprettelse af midlertidig fil-server: " + e.getMessage()); // his noget går galt, f.eks timeout
            Message errorMsg = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, "Filoverførsel fejlede for " + user.getUsername());
            this.sendMessage(Parser.formatToProtocol(errorMsg)); // send besked til brugeren om at det fejlede
        }
    }
    private void handleFileDownloadRequest(Message request) {
        String fileName = request.getPayload();

        if (!fileService.fileExists(fileName)) { // Tjekker om filen findes før vi forsøger at sende den
            Message errorMsg = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, "Fil ikke fundet: " + fileName);
            this.sendMessage(Parser.formatToProtocol(errorMsg)); // Sender fejlbesked kun til den anmodende klient
            return;
        }

        try (ServerSocket tempServerSocket = new ServerSocket(0)) { // Windows giver os en ledig port da vi skriver 0
            int port = tempServerSocket.getLocalPort(); // Tager den port vi får givet af windows
            long fileSize = fileService.getFileSize(fileName);

            // Send en "grønt lys" besked tilbage til kun den anmodende klient
            Message startDownload = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, "START_DOWNLOAD::" + port);
            this.sendMessage(Parser.formatToProtocol(startDownload));

            tempServerSocket.setSoTimeout(10000); // 10 sekunders timer, hvor længe vi venter på klienten forbinder

            try (Socket fileSocket = tempServerSocket.accept()) { // Accepterer den nye forbindelse fra klienten
                System.out.println("Sender fil '" + fileName + "' til " + user.getUsername());
                fileService.sendFile(fileSocket.getOutputStream(), fileName); // Smider ansvaret over til fileService

                // Sender til hele rummet at filen blev downloadet, skal opdatere så vi ikke bruger server_info parser til det
                Message confirmation = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, user.getUsername() + " downloadede " + fileName);
                messageService.sendMessageToRoom(roomName, Parser.formatToProtocol(confirmation), this);
            }

        } catch (IOException e) {
            System.out.println("Fejl under download: " + e.getMessage()); // Hvis noget går galt, f.eks timeout
            Message errorMsg = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, "Download fejlede for " + fileName);
            this.sendMessage(Parser.formatToProtocol(errorMsg)); // Send besked til brugeren om at det fejlede
        }
    }

    private void handleListFilesRequest() {
        List<String> files = fileService.getAvailableFiles(); // Henter alle tilgængelige filer fra FileUploads
        String filesList = String.join(",", files); // Sammensætter filnavnene med kommaer imellem

        // Sender fillisten tilbage til den anmodende klient
        Message filesResponse = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, "FILES_LIST::" + filesList);
        this.sendMessage(Parser.formatToProtocol(filesResponse)); // Kun til denne klient, ikke hele rummet
    }

    public void sendMessage(String message) {

            try {
                    bufferedWriter.write(message);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();

            } catch (IOException e) {
                closeEverything(socket,null,bufferedWriter);
            }

    }
    //bruger bliver fjernet fra arraylist
    public void removeClientHandler() {
        try {
            Message byebyeMessage = new Message("Server", LocalDateTime.now(), MessageType.SERVER_INFO,
                    user.getUsername() + " has left the chat");
            messageService.sendMessageToRoom(roomName, Parser.formatToProtocol(byebyeMessage), this);


            roomService.removeClientFromRoom(roomName, this);


            closeEverything(socket, null, bufferedWriter);

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
