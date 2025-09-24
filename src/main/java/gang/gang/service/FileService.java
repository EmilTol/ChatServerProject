package gang.gang.service;

import java.io.*;
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
}