package gang.gang.entity;

import java.time.LocalDateTime;

public class Message {
    private String clientId;
//    private String message;
    private LocalDateTime timestamp;
    private String messageType;
    private String payload;

    public Message(String clientId, LocalDateTime timestamp, String messageType, String payload) {
        this.clientId = clientId;
//        this.message = message;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.payload = payload;
    }
    public String getClientId() {
        return clientId;
    }
//    public String getMessage() {
//        return message;
//    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public String getMessageType() {
        return messageType;
    }
    public String getPayload() {
        return payload;
    }


}
