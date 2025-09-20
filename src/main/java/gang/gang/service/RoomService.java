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

    public boolean addClientToRoom(String roomName, ClientHandler clientHandler) {
        //listen for brugere i rummet
        List<ClientHandler> clients = rooms.get(roomName);
        if (clients != null && clients.size() < maxRoomSize) {
            //gemmer brugers clientHandler objekt, skal have adgang til socket for at sende beskeder
            clients.add(clientHandler);

            //gemmer kun username, f.eks. for at se liste af alle online
            onlineUsers.put(clientHandler.getUser().getUsername(), clientHandler.getUser());
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