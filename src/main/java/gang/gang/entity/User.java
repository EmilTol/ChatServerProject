package gang.gang.entity;

public class User {
    private String username;
    private String password;
    private String ipAddress;

    public User(){}

    public User(String username){
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }


}
