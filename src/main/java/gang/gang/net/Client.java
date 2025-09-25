package gang.gang.net;
import gang.gang.entity.Emoji;
import gang.gang.entity.Message;
import gang.gang.entity.MessageType;
import gang.gang.entity.User;
import gang.gang.protocol.Parser;
import gang.gang.service.CommandService;
import gang.gang.service.EmojiService;
import gang.gang.service.RoomService;


import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Scanner;

public class Client {

    private Socket socket;
    private BufferedReader bufferReader;
    private BufferedWriter bufferWriter;
    private User user;
    private CommandService commandService;
    private String pendingFilePath;
    private EmojiService emojiService;
    private String pendingDownloadFilePath;


    public Client(Socket socket, User user) {
        try {
            this.socket = socket;
            this.user = user;


            this.bufferWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Emoji emoji = new Emoji();
            this.emojiService = new EmojiService(emoji);
            this.commandService = new CommandService(this, new EmojiService(emoji));


        } catch (IOException e) {
            closeEverything(socket,bufferReader,bufferWriter);
        }
    }

    public User getUser() {
        return user;
    }
    public Socket getSocket() {
        return socket;
    }
    public void setPendingFilePath(String path) {
        this.pendingFilePath = path;
    }

    public void sendMessage() {
        try {
            Scanner scanner = new Scanner(System.in);
            //sender username til ClientHandler
            bufferWriter.write(user.getUsername());
            bufferWriter.newLine();
            bufferWriter.flush();

            String chosenRoom = scanner.nextLine(); // rå input
            bufferWriter.write(chosenRoom);
            bufferWriter.newLine();
            bufferWriter.flush();


            while (socket.isConnected()) {
                String input = scanner.nextLine();
                if (input.startsWith("/")) {
                    commandService.execute(input);
                } else if (input.startsWith(":") && input.endsWith(":")) {
                    String emojiPayload = emojiService.getChosenEmoji(input); // returnerer selve emoji’en eller ❓
                    //OKAY den her er her, fordi nogle ting konverter min null til en string der hedder Null så laver tjek her i stedet for clientHandler
                    if (emojiPayload != null) {
                        Message message = new Message(user.getUsername(), LocalDateTime.now(), MessageType.EMOJI, emojiPayload);
                        sendProtocolMessage(message);
                    }
                }else {
                    Message message = new Message(user.getUsername(), LocalDateTime.now(), MessageType.TEXT, input);
                    sendProtocolMessage(message);
                }
            }
        } catch (IOException e) {
            closeEverything(socket,bufferReader,bufferWriter);
        }
    }

    public void sendProtocolMessage(Message message) { //Hjælpe metode til at sende beskeder
        try {

            // Omdanner det strukturerede Message-objekt til en simpel tekststreng (f.eks. "bruger|tid|TEKST|hej").
            String formattedMessage = Parser.formatToProtocol(message);
            // Skriver den formaterede streng til vores output-buffer.
            bufferWriter.write(formattedMessage);
            // Tilføjer en ny linje, så serverens readLine() ved, at beskeden er færdig.
            bufferWriter.newLine();
            // Tømmer bufferen og sikrer, at beskeden sendes over netværket med det samme.
            bufferWriter.flush();
        } catch (IOException e) {
            System.out.println("Kunne ikke sende besked");
            closeEverything(socket, bufferReader, bufferWriter);
        }
    }

    //en block operation IG, så den får sin egen thread så resten ikke holder stille mens den venter på den her
    //så du stadig selv kan sende beskeder men dens også venter på beskeder fra andre brugere
    public void listenForMessage() {
        Thread thread = new Thread(new Runnable() { // Opførte sig funky, virker nu, men ved ikke hvorfor
            @Override
            public void run() {
                String msgFromGroupChat;

                while (socket.isConnected()) {
                    try {
                        while (socket.isConnected() && (msgFromGroupChat = bufferReader.readLine()) != null) {
                            Message message = Parser.parseFromProtocol(msgFromGroupChat);

                            // Tjekker først, om beskeden er gyldig OG om den er den specielle "grønt lys"-besked.
                            if (message != null && message.getPayload().startsWith("START_UPLOAD::")) {
                                // Deler beskeden op (f.eks. "START_UPLOAD::51234") for at få fat i portnummeret.
                                String[] parts = message.getPayload().split("::");
                                // Omdanner portnummeret fra tekst til et tal.
                                int port = Integer.parseInt(parts[1]);
                                // Starter selve fil-uploadet på den nye, midlertidige forbindelse.
                                uploadFile(port, Client.this.pendingFilePath);
                            } else if (message != null && message.getPayload().startsWith("START_DOWNLOAD::")) { // Stortset same shit som med upload
                                String[] parts = message.getPayload().split("::");
                                int port = Integer.parseInt(parts[1]);
                                downloadFile(port, Client.this.pendingDownloadFilePath);
                            } else if (message != null && message.getPayload().startsWith("FILES_LIST::")) { // Serveren sender os en liste over tilgængelige filer
                                String filesList = message.getPayload().substring("FILES_LIST::".length());
                                if (filesList.isEmpty()) {
                                    System.out.println("Ingen filer fundet");
                                } else {
                                    String[] files = filesList.split(","); // Deler fillisten op ved kommaer
                                    // Udskriver hver fil med et bindesteg foran for at gøre det lidt mere nice xD
                                    for (String file : files) {
                                        System.out.println("- " + file);
                                    }
                                }
                            } else if (message != null) {
                                System.out.println(Parser.formatForDisplay(message));
                            } else {
                                System.out.println(msgFromGroupChat);
                            }
                        }
                    } catch (IOException e) {
                        closeEverything(socket, bufferReader, bufferWriter);
                        System.out.println("Forbindelse mistet");
                        break;
                    }
                }
            }
            //starter den så den kører, noget alla thread.start i serveren
        });
        thread.start();
    }

    private void uploadFile(int port, String filePath) { // Håndtere fil afsendelsen på en ny seperat forbindelse
        if (filePath == null || filePath.isEmpty()) { // Tjekker at vi ved hvilken fil, eller om filepath er empty
            System.out.println("Ingen fil er specificeret til upload.");
            return;
        }
        System.out.println("Forbinder til midlertidig fil port " + port + " for at sende " + filePath);
        try (Socket fileSocket = new Socket(socket.getInetAddress().getHostName(), port); // Opretter en forbindelse til midlertidig fil-port
             OutputStream fileOut = fileSocket.getOutputStream()) {

            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath)); // Læser hele filen fra stien
            fileOut.write(fileBytes);
            System.out.println("Filen er sendt");

        } catch (IOException e) {
            System.out.println("Fejl under fil upload: " + e.getMessage());
        } finally {
            this.pendingFilePath = null;
        }
    }
    private void downloadFile(int port, String fileName) { // Håndtere fil download på en ny seperat forbindelse
        if ( fileName == null || fileName.isEmpty()) { // Tjekker at vi ved hvilken fil, eller om fileName er empty
            System.out.println("ingen fil er specificeret til download");
            return;
        }

        System.out.println("Forbinder til midlertidig fil port " + port + " for at hente " + fileName);

        // Opretter Downloads mappe hvis den ikke findes
        File downloadDir = new File("Downloads");
        if (!downloadDir.exists()) {
            downloadDir.mkdir();
        }

        try (Socket fileSocket = new Socket(socket.getInetAddress().getHostName(), port); // Opretter en forbindelse til midlertidig download-port
             InputStream fileIn = fileSocket.getInputStream(); // Stream til at læse data fra serveren
             FileOutputStream fileOut = new FileOutputStream(new File(downloadDir, fileName))) { // Stream til at skrive til lokal fil

            byte[] buffer = new byte[4096]; // Buffer til at holde data chunks (4KB ad gangen)
            int bytesRead;
            // Læser data fra serveren og skriver til lokal fil, stopper når der ikke er mere data (-1)
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead); // Skriver kun det antal bytes vi faktisk læste
            }
            System.out.println("Fil downloadet: " + fileName);

        } catch (IOException e) {
            System.out.println("Download fejlede: " + e.getMessage());
        } finally {
            this.pendingDownloadFilePath = null; // Rydder op efter download, uanset om det lykkedes eller fejlede
        }
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

    public void setPendingDownloadFilePath(String fileName) {
        this.pendingDownloadFilePath = fileName;
    }

}