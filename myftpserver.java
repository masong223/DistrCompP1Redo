import java.io.*;
import java.net.*;

public class myftpserver {
public static void main(String[] args) {
    int port = Integer.parseInt(args[0]);
    
    try (ServerSocket serverSocket = new ServerSocket(port)) {
        System.out.println("Server started on port " + port); //Confirmation message for testing
        
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected"); //Confirmation message for testing
            new Thread(new Client(clientSocket)).start();
        }
    } catch (IOException e) {
        e.printStackTrace();
        System.out.println("Error starting server / accepting client connection");
    }
    }
}

//Allows each client to have their own thread, own cwd
class Client extends Thread {
    private Socket socket;
    private File cwd;
    
    public Client(Socket socket) {
        this.socket = socket; //Allows the input/output streams to be used without hassle of passing socket as args
        this.cwd = new File(System.getProperty("user.dir")); //Gets directory from system
    }

    @Override
    public void run() {
        try {
            InputStream in = socket.getInputStream();
            DataInputStream dataIn = new DataInputStream(in); //Allows for easy reading of files without buffer issues
            OutputStream out = socket.getOutputStream();
            DataOutputStream byteOut = new DataOutputStream(out);
            System.out.println("Connected to I/O Streams"); //Testing client connection and stream setup
            
            
            while (true) {
                String commandFromUser = dataIn.readUTF(); //Reads command from client
                String[] commandParts = commandFromUser.split(" "); //Splits command into parts (on whitespace) so we can get command and args
                String command = commandParts[0]; //Gets command from user input

                if (command.equals("get")) {
                    get();
                } else if (command.equals("put")) {
                    put();
                } else if (command.equals("delete")) {
                    delete();
                } else if (command.equals("ls")) {
                    ls();
                } else if (command.equals("cd")) {
                    cd();
                } else if (command.equals("mkdir")) {
                    mkdir();
                } else if (command.equals("pwd")) {
                    pwd(byteOut);
                } else if (command.equals("quit")) {
                    break; //Exit program
                } else {
                    byteOut.writeUTF("Invalid command");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error with client connection / COMMANDS");
        }
    }
    void get() {

    }
    void put() {

    }
    void delete() {

    }
    void ls() {

    }
    void cd() {

    }
    void mkdir() {

    }
    void pwd(DataOutputStream byteOut) throws IOException {
        byteOut.writeUTF(cwd.getCanonicalPath());
        byteOut.flush();
    }
    void quit() {

    }
}
