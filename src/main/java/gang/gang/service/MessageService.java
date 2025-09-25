package gang.gang.service;

import gang.gang.entity.Message;
import gang.gang.entity.MessageType;
import gang.gang.protocol.Parser;

import java.time.LocalDateTime;
import java.util.List;

public class MessageService { // Logik til hvilken payload der bliver sendt
    private RoomService roomService;
    private UserService userService;

    public MessageService(RoomService roomService, UserService userService) {
        this.roomService = roomService;
        this.userService = userService;
    }

    public void sendMessageToRoom(String roomName, String message, ClientHandler clientHandler) {
        List<ClientHandler> clients = roomService.getClientsFromRoom(roomName);

        if (clients != null) {
            for (ClientHandler client : clients) {
                try {
                    client.sendMessage(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendPrivateMessage(String senderUsername, String targetUsername, String message, ClientHandler senderClient) {
        ClientHandler targetClient = userService.getClientHandler(targetUsername);
        if (targetClient != null) {
            targetClient.sendMessage(message);
            Message confirmation = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, "Private message sent to " + targetUsername);
            senderClient.sendMessage(Parser.formatToProtocol(confirmation));
        } else {
            Message error = new Message("SERVER", LocalDateTime.now(), MessageType.SERVER_INFO, "User not found");
            senderClient.sendMessage(Parser.formatToProtocol(error));

        }
    }
}
