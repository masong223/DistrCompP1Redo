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
                    ls(byteOut);
                } else if (command.equals("cd")) {
                    cd(commandParts[1], byteOut); //Passes arg (directory) to cd
                } else if (command.equals("mkdir")) {
                    mkdir(commandParts[1], byteOut); //Passes arg (directory) to mkdir
                } else if (command.equals("pwd")) {
                    pwd(byteOut);
                } else {
                    byteOut.writeUTF("Invalid command");
                }
            }
        } catch (Exception e) {
            //Only happens when client disconnects
            System.err.println("Client disconnected");
        }
    }
    void get() {

    }
    void put() {

    }
    void delete() {

    }
    void ls(DataOutputStream out) throws IOException {
        File[] filesinCwd = cwd.listFiles(); //Gets list of files in cwd
        StringBuilder message = new StringBuilder(); //Allows us to send one message to client instead of multiple
        for (File file : filesinCwd) {
            if (file.isDirectory()) {
                message.append(file.getName()).append("/ "); //Adds / to end of directories
            } else {
                message.append(file.getName()).append(" ");
            }
        }
        out.writeUTF(message.toString());
        out.flush();
    }
    void cd(String newDirectory, DataOutputStream byteOut) throws IOException {
        File dirToGoTo; 
        if (newDirectory.equals("..")) {
            dirToGoTo = cwd.getParentFile().getCanonicalFile(); //Goes up to parent
            if (dirToGoTo == null) {
                dirToGoTo = cwd.getCanonicalFile(); //If there is no parent, stay in same directory
                byteOut.writeUTF("Directory changed to " + cwd.getCanonicalPath());
            }
        } else if (newDirectory.equals(".")) {
            return; //Stays in same directory, no action needed
        }
            else {
                dirToGoTo = new File(cwd, newDirectory); //Creates path to new directory
                if (dirToGoTo.exists() && dirToGoTo.isDirectory()) {
                    cwd = dirToGoTo; //Changes cwd to new directory
                    cwd = cwd.getCanonicalFile(); //Gets rid of .. and . in the path
                    byteOut.writeUTF("Directory changed to " + cwd.getCanonicalPath()); //Confirmation message FOR TESTING
                } else {
                    byteOut.writeUTF("Directory does not exist");
                }
        }
    }
    void mkdir(String newDirName, DataOutputStream byteOut) throws IOException {
        File newDir = new File(cwd, newDirName); //Creates path to new directory
        if (newDir.exists()) {
            byteOut.writeUTF("Directory already exists");
        } else {
            boolean isCreated = newDir.mkdir();
            if (isCreated) {
                byteOut.writeUTF("Directory created");
            } else {
                byteOut.writeUTF("Failed to create directory");
            }
        }
        byteOut.flush();

    }
    void pwd(DataOutputStream byteOut) throws IOException {
        byteOut.writeUTF(cwd.getCanonicalPath());
        byteOut.flush();
    }
    void quit() {

    }
}
