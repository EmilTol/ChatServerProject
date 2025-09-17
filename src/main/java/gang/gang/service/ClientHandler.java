package gang.gang.service;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

    //holder styr på vores brugere, looper igennem når ny besked sendt så alle modtagere beskeden.
    //static fordi den skal tilhører klassen og ikke hvert objekt af klassen
    public static ArrayList<ClientHandler> clientHandler = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;

    public ClientHandler(Socket socket) {
        try {
            //gør at vores socket er ligemed hvad bliver sat til i serveren
            this.socket = socket;
            //outputStreamWriter er en character stream
            //getOutputStream er en byte stream
            this.bufferedWriter = new BufferedWriter ( new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //sætter clientUsername som modtaget input fra bruger (deres første input)
            this.clientUsername = bufferedReader.readLine();
            //tilføjer til arraylisten
            clientHandler.add(this);
            //udskriver en ny bruger et ankommet
            broadcastMessage("SERVER: " + clientUsername + " has entered the chat!");
        } catch (IOException e) {
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        //mens vi er tilsluttet en bruger
        while (socket.isConnected()) {
            try {
                //lytter efter beskeder fra brugere, a blocking operation
                messageFromClient = bufferedReader.readLine();
                broadcastMessage(messageFromClient);
            } catch (IOException e) {
                closeEverything(socket,bufferedReader,bufferedWriter);
                break;
            }
        }
    }
    public void broadcastMessage(String messageToSend) {
        //looper igennem vores arraylist af brugere
        for (ClientHandler clientHandler : clientHandler) {
            try {
                //så ens besked ikke bliver vist for en selv
                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    //sender beskeden
                    clientHandler.bufferedWriter.write(messageToSend);
                    //okay så, den her betyder, jeg færdig med at sende data, ik vent på mere data.
                    //svarer til at trykke på enter
                    clientHandler.bufferedWriter.newLine();
                    //burde bare kunne lave auto.flush
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket,bufferedReader,bufferedWriter);
            }
        }
    }
    //bruger bliver fjernet fra arraylist
    public void removeClientHandler() {
        clientHandler.remove(this);
        broadcastMessage("SERVER: " + clientUsername + " has left the chat!");
    }

    //sørger for alting lukker, intet unødvendigt åbent, CUSTOM EXCEPTION HANDLING? i believe
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
