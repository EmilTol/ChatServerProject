package gang.gang.service;

import java.io.*;

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
}