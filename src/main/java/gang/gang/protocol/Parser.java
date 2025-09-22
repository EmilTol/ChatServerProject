package gang.gang.protocol;
import gang.gang.entity.Message;
import gang.gang.entity.MessageType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Parser {

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public static String formatToProtocol(Message message) {
        return String.join("|" , message.getClientId(), message.getTimestamp().format(timeFormatter), message.getMessageType().name(), message.getPayload());
    }

    public static Message parseFromProtocol(String protocolString) {
        String[] parts = protocolString.split("\\|", 4); // Vi splitter i 4 dele, vores id, timestamp, type og payload )
        if (parts.length < 4) return null; // Hvis der ikke er 4 dele, returner vi null

        String clientId = parts[0];
        LocalDateTime timestamp = LocalDateTime.parse(parts[1], timeFormatter);
        MessageType messageType = MessageType.valueOf(parts[2].toUpperCase());
        String payload = parts[3];

        return new Message(clientId, timestamp, messageType, payload);
    }

    public static String formatForDisplay(Message message) { // Formattere vores Message objekt så det står som vi vil have det
        String time = message.getTimestamp().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

        switch (message.getMessageType()) { // Tjekker vores messageType og formaterer den så det passer med typen
            case TEXT: // Normale beskeder
                return String.format("[%s] [%s] %s: %s", time, message.getMessageType(), message.getClientId(), message.getPayload());
            case SERVER_INFO: // Server beskeder, f.eks hvis en person tilslutter
                return String.format("(:  %s  :)", message.getPayload());
            case FILE_TRANSFER: // File overførsel, specielt ved den er at den deler payload i 2, navn og størrelse
                String[] fileInfo = message.getPayload().split("\\|");
                if (fileInfo.length == 2) {
                    String fileName = fileInfo[0];
                    String fileSize = fileInfo[1];
                    return String.format("[%s] [FILE] %s: %s (%s bytes)", time, message.getClientId(), fileName, fileSize);
                } else {
                    return String.format("[%s] [FILE] %s startede en filoverførsel.", time, message.getClientId());
                }
            default:
                return "Ukendt beskedformat";
        }
    }

//    // Tager en bruger besked og formaterer den.
//    public static String formatChatMessage(String username, String message) {
//        String time = LocalDateTime.now().format(timeFormatter);
//        return String.format("[%s] %s: %s", time, username, message);
//    }
//
//   // Tager en server besked og formaterer den.
//    public static String formatServerMessage(String message) {
//        return String.format("(: %s :)", message);
//    }
}