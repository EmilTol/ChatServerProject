package gang.gang.service;

import gang.gang.entity.Command;
import gang.gang.entity.Message;
import gang.gang.entity.MessageType;
import gang.gang.service.EmojiService;
import gang.gang.net.Client;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

public class CommandService {

    private final Client client;
    private final EmojiService emojiService;

    public CommandService(Client client, EmojiService emojiService) {
        this.client = client;
        this.emojiService = emojiService;
    }

    public void execute(String commandInput) { // Er metoden som skal holde styr på om det er en kommando eller ikke
        Optional<Command> foundCommand = Command.fromString(commandInput); //Prøver at matche input med en kommando
        if (foundCommand.isPresent()) { // tjekker og køre hvis vi kender kommandoen
            run(foundCommand.get(), commandInput);
        } else {
            System.out.println("Ugyldig kommando.");
        }
    }

    private void run(Command command, String fullInput) { // Sender kommandoen til den korrekte logik
        switch (command) {
            case SEND_FILE:
                String filePath = fullInput.substring(command.getCommand().length()).trim(); //Adskiller kommandoen fra filstien
                handleSendFile(filePath); // Kalder metoden for at sende filen
                break;

            case EMOJI:
                System.out.println("Nuværende Emojis");
                emojiService.getAllEmojis();
                break;

            default:
                System.out.println("Kommandoen '" + command.getCommand() + "' er ikke implementeret endnu.");
                break;
        }
    }

    private void handleSendFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) { // Tjekker om filen findes
            System.out.println("Fejl: Filen findes ikke: " + filePath);
            return;
        }

        // Husker filstien, indtil serveren giver os lov til at sende den, når port er etableret
        client.setPendingFilePath(filePath);

        // Opretter en anmoding om at sende filen, med dens data.
        Message fileRequest = new Message(
                client.getUser().getUsername(),
                LocalDateTime.now(),
                MessageType.FILE_TRANSFER,
                file.getName() + "|" + file.length()
        );
        client.sendProtocolMessage(fileRequest); // Sender anmodningen til serveren
        System.out.println("Anmodning om at sende filen '" + file.getName() + "' er sendt til serveren.");
    }
}