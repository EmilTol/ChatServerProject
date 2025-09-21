package gang.gang.service;

import gang.gang.entity.User;

import java.util.*;

public class RoomService {

    private final Map<String, List<ClientHandler>> rooms = new LinkedHashMap<>();
    private final Map<String, User> onlineUsers = new HashMap<>();
    private final int maxRoomSize = 5;

    public RoomService() {
        rooms.put("room1", new ArrayList<>());
        rooms.put("room2", new ArrayList<>());
        rooms.put("room3", new ArrayList<>());
    }

    public synchronized boolean addClientToRoom(String roomName, ClientHandler clientHandler) {
        System.out.println("Debuging check af addClientToRoom ");
        System.out.println("RoomName: " + roomName);
        System.out.println("ClientHandler: " + (clientHandler != null ? clientHandler : "null"));

        //listen for brugere i rummet
        List<ClientHandler> clients = rooms.get(roomName);
        System.out.println("brugere allerede i rummet: " + clients != null ? clients.size() : "null");

        if (clients != null && clients.size() < maxRoomSize) {
            //gemmer brugers clientHandler objekt, skal have adgang til socket for at sende beskeder
            clients.add(clientHandler);
            System.out.println("bruger tilføjet til rummet. room size: " + clients.size());

            if (clientHandler != null && clientHandler.getUser() != null) {
                String username = clientHandler.getUser().getUsername();
                onlineUsers.put(username, clientHandler.getUser());
                System.out.println("  --> onlineUsers opdateret med: " + username);
            } else {
                System.out.println("  [ADVARSEL] clientHandler eller user er null!");
            }
            return true;
        }
        return false;
    }

    public void removeClientFromRoom(String roomName, ClientHandler clientHandler) {
        List<ClientHandler> clients = rooms.get(roomName);
        if (clients != null)
            clients.remove(clientHandler);
        onlineUsers.remove(clientHandler.getUser().getUsername());
    }

    public List<ClientHandler> getClientsFromRoom(String roomName) {
        return rooms.get(roomName);
    }

    public Map <String, Integer> getRoomStatus() {
        Map <String, Integer> status = new LinkedHashMap<>();
        for (String roomName : rooms.keySet()) {
            status.put(roomName, rooms.get(roomName).size());
        }
        return status;
    }
    public int getMaxRoomSize() {
        return maxRoomSize;
    }

}