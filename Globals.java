import java.util.HashMap;

public class Globals{
    public static class CommandStatus {
        public volatile boolean status;

        public CommandStatus() {
            this.status = true; // true means command is running, false means command is terminated
        }
    }
    public static final HashMap<Integer, CommandStatus> commands = new HashMap<>();
    //true = running, false = terminated/finished
    public static int id = 0;
    public static final Object lock = new Object(); //Lock for synchronizing access to commands hashmap and id variable
}