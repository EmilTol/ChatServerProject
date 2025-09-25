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
        if (clientHandler == null && clientHandler.getUser() == null) {
            System.out.println("ClientHandler eller user er null!");
            return false;
        }
        List<ClientHandler> clients = room.getRooms(roomName);
        System.out.println("brugere allerede i rummet: " + clients != null ? clients.size() : "null");

        if (clients.size() < room.getMaxRoomSize()) {
            room.addClientToRoom(roomName, clientHandler);
            room.addUserToOnline(clientHandler.getUser());
            System.out.println("bruger tilføjet til rummet. room size: " + clients.size());
            return true;
        }
        System.out.println("Rummet er fyldt!");
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