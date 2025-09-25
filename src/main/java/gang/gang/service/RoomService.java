package gang.gang.service;

import gang.gang.entity.User;
import gang.gang.entity.Room;

import java.util.*;

public class RoomService {
    private Room room;

    public RoomService(Room room) {
        this.room = room;
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
        room.removeClientFromRoom(roomName, clientHandler);
        if (clientHandler.getUser() != null) {
        room.removeUserFromOnline(clientHandler.getUser().getUsername());
        }
    }

    public List<ClientHandler> getClientsFromRoom(String roomName) {
        return room.getRooms(roomName);
    }

    public Map <String, Integer> getRoomStatus() {
        Map<String, Integer> status = new LinkedHashMap<>();
        for (String roomName : room.getRoomsNames()) {
            List<ClientHandler> clients = room.getRooms(roomName);
            status.put(roomName,clients.size());
        }
        return status;

    }
    public int getMaxRoomSize() {
        return room.getMaxRoomSize();
    }
}