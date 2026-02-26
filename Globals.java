import java.util.HashMap;
import java.util.HashSet;

public class Globals{
    public static class CommandStatus {
        public volatile boolean status;

        public CommandStatus() {
            this.status = true; // true means command is running, false means command is terminated
        }
    }
    public static final HashMap<Integer, CommandStatus> commands = new HashMap<>();
    public static final HashSet<String> filesInUse = new HashSet<>(); //Set (no duplicates) to track files currently being accessed by threads
    //true = running, false = terminated/finished
    public static int id = 0;
    public static final Object lock = new Object(); //Lock for synchronizing access to commands hashmap and id variable

    //Removes race conditions by ensuring that only one thread can access the file at a time
    public synchronized void lockFile(String filename) {
        while (filesInUse.contains(filename)) { //Checks if the desired file is currently being used by another thread
            try {
                wait(); //If it is, wait until it becomes available
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        filesInUse.add(filename); //Once the file is available, add it to the set of files in use
    }

    //Allows other threads to access this file once the current thread is finished
    public synchronized void unlockFile(String filename) {
        filesInUse.remove(filename); //Make the file available to threads
        notifyAll(); //Notify any waiting threads that a file has been unlocked, threads come out of wait to try and get it
    }
}