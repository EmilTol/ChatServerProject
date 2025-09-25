package gang.gang.service;

import gang.gang.net.Client;

import java.util.HashMap;
import java.util.Map;

public class UserService {
    private final Map<String, ClientHandler> connectedUsers = new HashMap<>();

    public synchronized void addUser(String username, ClientHandler clientHandler) {
        connectedUsers.put(username, clientHandler);
    }

    public synchronized void removeUser(String username) {
        connectedUsers.remove(username);
    }

    public ClientHandler getClientHandler(String username) {
        return connectedUsers.get(username);
    }

    public boolean userExists(String username) {
        return connectedUsers.containsKey(username);
    }


}
