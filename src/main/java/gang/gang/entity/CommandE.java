package gang.gang.entity;

public enum CommandE {
    SEND_FILE("/sendfile"),
    DIRECTMESSAGE("/dm"),
    USERS("/users"),
    JOIN_ROOM("/join");

    private final String command;
    CommandE(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }


}
