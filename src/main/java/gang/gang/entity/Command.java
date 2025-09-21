package gang.gang.entity;

import java.util.Arrays;
import java.util.Optional;

public enum Command {
    SEND_FILE("/sendfile"),
    DIRECTMESSAGE("/dm"),
    USERS("/users"),
    JOIN_ROOM("/join");

    private final String command;
    Command(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public static Optional<Command> fromString(String text){ //Honestly ikke helt sikker...
        return Arrays.stream(values()).filter(cmd -> text.toLowerCase().startsWith(cmd.command)).findFirst();
    }
}
