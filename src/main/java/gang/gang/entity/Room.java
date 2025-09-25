package gang.gang.entity;

import gang.gang.service.ClientHandler;

import java.util.*;

public class Room {
    private final Map<String, List<ClientHandler>> rooms = new LinkedHashMap<>();
    private final Map<String, User> onlineUsers = new HashMap<>();
    private final int maxRoomSize = 5;

    public Room() {
        rooms.put("room1", new ArrayList<>());
        rooms.put("room2", new ArrayList<>());
        rooms.put("room3", new ArrayList<>());
    }

    public int getMaxRoomSize() {
        return maxRoomSize;
    }

    public List<ClientHandler> getRooms(String roomName) {
        return rooms.getOrDefault(roomName, new ArrayList<>());
    }

    public Set<String> getRoomsNames() {
        return rooms.keySet();
    }

    public void addClientToRoom(String roomName, ClientHandler clientHandler) {
        rooms.computeIfAbsent(roomName, k-> new ArrayList<>()).add(clientHandler);
    }

    public void removeClientFromRoom(String roomName, ClientHandler clientHandler) {
        List<ClientHandler> clients = rooms.get(roomName);
        if (clients != null) {
            clients.remove(clientHandler);
        }
    }

    public void addUserToOnline(User user) {
        onlineUsers.put(user.getUsername(), user);
    }

    public void removeUserFromOnline(String username) {
        onlineUsers.remove(username);
    }

    public Collection<User> getOnlineUsers() {
        return onlineUsers.values();
    }

}
