package gang.gang.service;

import gang.gang.entity.Message;
import gang.gang.entity.MessageType;
import gang.gang.protocol.Parser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileService {
    private static final String UPLOAD_DIRECTORY = "FileUploads"; // navnet på mappe hvor filen skal sendes

    //"rå" strøm af bytes som
    public void receiveFile(InputStream inputStream, String fileName, long fileSize) throws IOException {
        File uploadDir = new File(UPLOAD_DIRECTORY);
        if (!uploadDir.exists()) { // Tjekker om mappene findes, hvis ikke opretter vi den
            uploadDir.mkdir();
        }
        File finalFile = new File(uploadDir, fileName); // Opretter fil objektet for den endelige fil i mappen.
        try (FileOutputStream fileOutputStream = new FileOutputStream(finalFile)) { //Åbner en stream til at "overføre" data til filen.
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesRead = 0;
            //Løkken forstætter så længe vi ikke har læst hele filen
            while (totalBytesRead < fileSize && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
        }
    }
    public List <String> getAvailableFiles() {
        File uploadDir = new File(UPLOAD_DIRECTORY);

        if (!uploadDir.exists() || !uploadDir.isDirectory()) { // Tjekker om mappen findes og faktisk er en mappe
            return new ArrayList<>(); // Returnerer tom liste hvis mappen ikke findes
        }
        List<String> files = new ArrayList<>();
        File[] fileArray = uploadDir.listFiles(); // Henter alle filer og mapper i directory
        if (fileArray != null) { // listFiles() kan returnere null hvis der er adgangsproblemer
            for (File file : fileArray) {
                if (file.isFile()) { // Springer mapper over, kun filer
                    files.add(file.getName()); // Tilføjer kun filnavnet, ikke den fulde sti
                }
            }
        }
        return files;
    }

    public void sendFile(OutputStream outputStream, String fileName) throws IOException {
        File file = new File(UPLOAD_DIRECTORY, fileName);
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[4096]; // Buffer til at læse filen
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) { // Læser fra fil og skriver direkte til outputStream, altså vores client
                outputStream.write(buffer, 0, bytesRead); // Skriver kun det antal bytes vi faktisk læste
            }
        }
    }

    public boolean fileExists(String fileName) {
        File file = new File(UPLOAD_DIRECTORY, fileName);
        return file.exists() && file.isFile(); // Tjekker både at den findes og er en fil
    }

    public long getFileSize(String fileName) {
        File file = new File(UPLOAD_DIRECTORY, fileName);
        return file.length(); // Returnerer filstørrelse i bytes
    }

    public void handleFileTransferRequest(Message requestMessage, String username, String roomName, ClientHandler clientHandler, MessageService messageService) {
        try (ServerSocket tempServerSocket = new ServerSocket(0)) { // Sygt smart, windows giver os en ledig port da vi skriver 0. Så vi behøver ikke tage stilling til det.
            int port = tempServerSocket.getLocalPort(); // tager den port vi får givet af windows
            String[] fileInfo = requestMessage.getPayload().split("\\|"); // deler vores payload i 2, ja, vi gør det 2 steder, fixer på et andet tidspunkt.
            String fileName = fileInfo[0];
            long fileSize = Long.parseLong(fileInfo[1]);

            // Send en "grønt lys" besked tilbage til KUN den anmodende klient
            Message goAheadMessage = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, "START_UPLOAD::" + port);
            clientHandler.sendMessage(Parser.formatToProtocol(goAheadMessage));


            tempServerSocket.setSoTimeout(10000); // 10 sekunders timer, som beskriver hvor længe vi vil vente på klienten forbinder til den nye port.

            try (Socket fileSocket = tempServerSocket.accept()) { // Acceptere den nye forbindelse fra klienten
                System.out.println("Modtager fil '" + fileName + "' på midlertidig port " + port);
                receiveFile(fileSocket.getInputStream(), fileName, fileSize); // smider ansvaret over til fileService

                Message confirmation = new Message(username, LocalDateTime.now(), MessageType.FILE_TRANSFER, fileName + "|" + fileSize); //Semder til hele rummet at filen er sendt
                messageService.sendMessageToRoom(roomName, Parser.formatToProtocol(confirmation), clientHandler);
            }

        } catch (IOException e) {
            System.out.println("Fejl under oprettelse af midlertidig fil-server: " + e.getMessage()); // his noget går galt, f.eks timeout
            Message errorMsg = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, "Filoverførsel fejlede for " + username);
            clientHandler.sendMessage(Parser.formatToProtocol(errorMsg)); // send besked til brugeren om at det fejlede
        }
    }

    public void handleFileDownloadRequest(Message request, String username, String roomName, ClientHandler clientHandler, MessageService messageService) {
        String fileName = request.getPayload();

        if (!fileExists(fileName)) { // Tjekker om filen findes før vi forsøger at sende den
            Message errorMsg = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, "Fil ikke fundet: " + fileName);
            clientHandler.sendMessage(Parser.formatToProtocol(errorMsg)); // Sender fejlbesked kun til den anmodende klient
            return;
        }

        try (ServerSocket tempServerSocket = new ServerSocket(0)) { // Windows giver os en ledig port da vi skriver 0
            int port = tempServerSocket.getLocalPort(); // Tager den port vi får givet af windows
            long fileSize = getFileSize(fileName);

            // Send en "grønt lys" besked tilbage til kun den anmodende klient
            Message startDownload = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, "START_DOWNLOAD::" + port);
            clientHandler.sendMessage(Parser.formatToProtocol(startDownload));

            tempServerSocket.setSoTimeout(10000); // 10 sekunders timer, hvor længe vi venter på klienten forbinder

            try (Socket fileSocket = tempServerSocket.accept()) { // Accepterer den nye forbindelse fra klienten
                System.out.println("Sender fil '" + fileName + "' til " + username);
                sendFile(fileSocket.getOutputStream(), fileName); // Smider ansvaret over til fileService

                // Sender til hele rummet at filen blev downloadet, skal opdatere så vi ikke bruger server_info parser til det
                Message confirmation = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, username + " downloadede " + fileName);
                messageService.sendMessageToRoom(roomName, Parser.formatToProtocol(confirmation), clientHandler);
            }

        } catch (IOException e) {
            System.out.println("Fejl under download: " + e.getMessage()); // Hvis noget går galt, f.eks timeout
            Message errorMsg = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, "Download fejlede for " + fileName);
            clientHandler.sendMessage(Parser.formatToProtocol(errorMsg)); // Send besked til brugeren om at det fejlede
        }
    }

    public void handleListFilesRequest(String username, ClientHandler clientHandler) {
        List<String> files = getAvailableFiles(); // Henter alle tilgængelige filer fra FileUploads
        String filesList = String.join(",", files); // Sammensætter filnavnene med kommaer imellem

        // Sender fillisten tilbage til den anmodende klient
        Message filesResponse = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, "FILES_LIST::" + filesList);
        clientHandler.sendMessage(Parser.formatToProtocol(filesResponse)); // Kun til denne klient, ikke hele rummet
    }
}