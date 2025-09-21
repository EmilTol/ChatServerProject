package gang.gang.service;

import gang.gang.entity.Message;

import java.util.List;

public class MessageService { // Logik til hvilken payload der bliver sendt
    private RoomService roomService;

    public MessageService(RoomService roomService) {
        this.roomService = roomService;
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
}
