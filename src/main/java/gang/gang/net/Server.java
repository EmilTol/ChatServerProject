package gang.gang.net;

import gang.gang.service.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket serverSocket;

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }
    public void startServer() {

        try {
            while(!serverSocket.isClosed()) {

                //venter på brugere til connect
                Socket socket = serverSocket.accept();
                System.out.println("A new client has connected :) " + socket.getInetAddress());

                //hvert objekt af denne klasse kommunikere med en bruger
                ClientHandler clientHandler = new ClientHandler(socket);

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
        ServerSocket serverSocket = new ServerSocket(5555);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}

