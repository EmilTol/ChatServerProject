package gang.gang.protocol;
import gang.gang.entity.Message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Parser {

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public static String formatToProtocol(Message message) {
        return String.join("|" , message.getClientId(), message.getTimestamp().format(timeFormatter), message.getMessageType(), message.getPayload());
    }

    public static Message parseFromProtocol(String protocolString) {
        String[] parts = protocolString.split("\\|", 4); // Vi splitter i 4 dele, vores id, timestamp, type og payload )
        if (parts.length < 4) return null; // Hvis der ikke er 4 dele, returner vi null

        String clientId = parts[0];
        LocalDateTime timestamp = LocalDateTime.parse(parts[1], timeFormatter);
        String messageType = parts[2];
        String payload = parts[3];

        return new Message(clientId, timestamp, messageType, payload);
    }

    public static String formatForDisplay(Message message) {
        String time = message.getTimestamp().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

        switch (message.getMessageType()) {
            case "TEXT":
                return String.format("[%s] [%s] %s: %s", time, message.getMessageType(), message.getClientId(), message.getPayload());
            case "SERVER_INFO":
                return String.format("(:  %s  :)", message.getPayload());
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