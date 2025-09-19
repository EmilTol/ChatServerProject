package gang.gang.protocol;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Parser {

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    // Tager en bruger besked og formaterer den.
    public static String formatChatMessage(String username, String message) {
        String time = LocalDateTime.now().format(timeFormatter);
        return String.format("[%s] %s: %s", time, username, message);
    }

   // Tager en server besked og formaterer den.
    public static String formatServerMessage(String message) {
        return String.format(" (: %s :) ", message);
    }
}