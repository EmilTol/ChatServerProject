package gang.gang.net;

import gang.gang.service.ClientHandler;
import gang.gang.service.MessageService;
import gang.gang.service.RoomService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket serverSocket;
    private Room room = new Room();
    RoomService roomService = new RoomService(room);
    UserService userService = new UserService();
    MessageService messageService = new MessageService(roomService, userService);

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }
    public void startServer() {

        try {
            while(!serverSocket.isClosed()) {
//           Beholder den her for shame mig selv, kom til at lave en ny instans af roomService :,D
//                RoomService roomService = new RoomService();

                //venter på brugere til connect
                Socket socket = serverSocket.accept();
                System.out.println("A new client has connected :) " + socket.getInetAddress());

                //hvert objekt af denne klasse kommunikere med en bruger
                ClientHandler clientHandler = new ClientHandler(socket, roomService, messageService, userService);

                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            closedServerSocket();
        }
    }
    public void closedServerSocket() {
        try {
            if(serverSocket != null) {
                serverSocket.close();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(5556);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}

