package gang.gang.service;

import gang.gang.net.Client;

public class CommandService {

    private final Client client;

    public CommandService(Client client) {
        this.client = client;
    }

//    public void handleUserInput(String input) {
//        if (input.startsWith("/sendfile")) {
//            String filepath = input.substring(10).trim();
//            handleSendFile(filepath);
//
//        }
//    }

}
