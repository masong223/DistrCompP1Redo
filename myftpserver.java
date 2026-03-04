import java.io.*;
import java.net.*;

public class myftpserver {
public static void main(String[] args) {
        int nport = Integer.parseInt(args[0]);
        int tport = Integer.parseInt(args[1]);
        
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(nport)) {
                System.out.println("Server started on port " + nport); //Confirmation message for testing
                
                while (true) {
                    Socket nclientSocket = serverSocket.accept();
                    System.out.println("Client connected"); //Confirmation message for testing
                    new Thread(new Client(nclientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error starting server / accepting client connection");
            }
        }).start();
        try (ServerSocket serverSocket = new ServerSocket(tport)) {
            System.out.println("Server started on port " + tport); //Confirmation message for testing
            
            while (true) {
                Socket tclientSocket = serverSocket.accept();
                System.out.println("Client connected"); //Confirmation message for testing
                new Thread(new TerminatePort(tclientSocket)).start();
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
                if (commandFromUser.endsWith("&")) {
                    commandFromUser = commandFromUser.substring(0, commandFromUser.length() - 1); //Removes & from end of command for processing
                }
                String[] commandParts = commandFromUser.split(" "); //Splits command into parts (on whitespace) so we can get command and args
                String command = commandParts[0]; //Gets command from user input
                processString(command, commandParts, byteOut, dataIn);
            }
        } catch (Exception e) {
            //Only happens when client disconnects
            System.err.println("Client disconnected");
        }
    }

    void get(DataOutputStream byteOut, String filename, int commandID) throws IOException{
        File fileToSend = new File (cwd, filename);
        if (!fileToSend.exists()) {
            byteOut.writeUTF("File does not exist");
            byteOut.flush();
            return;
        } //checking if file exists

        byteOut.writeUTF(fileToSend.getName());
        byteOut.flush();       // file name

        long fileSize = fileToSend.length();
        byteOut.writeLong(fileSize); // file size
        int size = (int) fileSize;
        byte[] payload = new byte[1000]; 

        Globals.lockFile(fileToSend.getCanonicalPath());

        try (FileInputStream fileIn = new FileInputStream(fileToSend)) {   
            
            while (size > 0) {
                //If user terminates command, check status and break if false.
                if (Globals.commands.get(commandID).status == false) {
                    byteOut.writeUTF("Terminated");
                    byteOut.flush();
                    break;
                }

               int bytesRead = fileIn.read(payload, 0, Math.min(size, 1000));  // reading between 1000 and bytes left in file          
              if (bytesRead == -1) {
                    break;
                }
                byteOut.write(payload, 0, bytesRead); 
                byteOut.flush();
                size = size - bytesRead;
            }    
        } catch (IOException e) {
            byteOut.writeUTF("Error when turning file into byte array");
            byteOut.flush();
        } finally {
            Globals.unlockFile(fileToSend.getCanonicalPath());
        }

    }

    void put(DataOutputStream byteOut, DataInputStream byteIn, String fileName, int commandID) throws IOException {
        System.out.println("Put command received"); //Testing put command
        long fileSize = byteIn.readLong();
        System.out.println(fileSize);
        File fileToWrite = new File(cwd, fileName);
        byte[] payLoad = new byte [1000];

        Globals.lockFile(fileToWrite.getCanonicalPath()); //Prevents threads from using file

        try (FileOutputStream fileOut = new FileOutputStream(fileToWrite)) {
            while (fileSize > 0) {
                //If user wants to terminate command, check status, delete file, and break if false.
                if (Globals.commands.get(commandID).status == false) {
                    fileOut.close();
                    fileToWrite.delete(); //Deletes file on server side
                    break;
                }
                
                int bytesToRead = (int) Math.min((int)fileSize, 1000);
                byteIn.readFully(payLoad, 0 , bytesToRead);
                fileOut.write(payLoad, 0, bytesToRead);
                fileOut.flush();
                fileSize = fileSize - bytesToRead;
            }
         } catch (IOException e) {
            System.out.println("Server error creating file");
        } finally {
            Globals.unlockFile(fileToWrite.getCanonicalPath()); //Lets other threads use file
        }
    }


    void delete(String filePathToDelete, DataOutputStream byteOut) throws IOException {
        File fileToDelete = new File(cwd, filePathToDelete);
        if (fileToDelete.exists()) {
            boolean isDeleted = fileToDelete.delete(); //Method returns boolean after calling
            if (isDeleted) {
                byteOut.writeUTF("File deleted");
            } else {
                byteOut.writeUTF("Failed to delete file");
            }
        } else {
            byteOut.writeUTF("File does not exist"); //Means that the either the system couldnt find the file or it doesnt exist
        }
        byteOut.flush();
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
                byteOut.writeUTF("Directory not changed: " + cwd.getCanonicalPath());
            } else { 
                cwd = dirToGoTo; //Changes cwd to new directory
                byteOut.writeUTF("Directory changed to " + cwd.getCanonicalPath()); 
            }
        } else if (newDirectory.equals(".")) {
            byteOut.writeUTF("Directory not changed: " + cwd.getCanonicalPath()); //Has to send something so the client doesn't hang
            return; //Stays in same directory, no action needed
        }
            else {
                dirToGoTo = new File(cwd, newDirectory); //Creates path to new directory
                if (dirToGoTo.exists() && dirToGoTo.isDirectory()) { //Don't want to cd to a file :(
                    cwd = dirToGoTo; //Changes cwd to new directory
                    cwd = cwd.getCanonicalFile(); //Gets rid of .. and . in the path
                    byteOut.writeUTF("Directory changed to " + cwd.getCanonicalPath()); //Confirmation message FOR TESTING
                } else {
                    byteOut.writeUTF("Directory does not exist");
                }
        }
        byteOut.flush();
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

    private void processString(String command, String[] commandParts, DataOutputStream byteOut, DataInputStream dataIn) throws IOException {
        if (command.equals("get")) {
                int currentID;
                
                 //Adds command to hashmap with unique id and status of running
                synchronized(Globals.lock) {
                    Globals.id++;
                    currentID = Globals.id;
                    Globals.commands.put(currentID, new Globals.CommandStatus());
                }
                byteOut.writeInt(currentID); //sends client ID
                get(byteOut, commandParts[1], currentID);
                Globals.commands.get(currentID).status = false; //Changes status to false after command finishes
                //Passes arg (file to get) to get
            } else if (command.equals("put")) {
                int currentID;
                
                synchronized(Globals.lock) {
                    Globals.id++;
                    currentID = Globals.id;
                    Globals.commands.put(currentID, new Globals.CommandStatus()); //Puts command in hashmap
                }
                byteOut.writeInt(currentID); //sends client ID
                put(byteOut, dataIn, commandParts[1], currentID); //Passes input and output streams to put so it can read file data from client and write file data to client
                Globals.commands.get(currentID).status = false; //Changes status to false after command finishes
            } else if (command.equals("delete")) {
                delete(commandParts[1], byteOut); //Passes arg (file to delete) to delete
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
}

class TerminatePort extends Thread {
    private Socket socket;

    public TerminatePort(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            InputStream in = socket.getInputStream();
            DataInputStream dataIn = new DataInputStream(in);
            OutputStream out = socket.getOutputStream();
            DataOutputStream dataOut = new DataOutputStream(out);
            System.out.println("Connected to I/O Streams for termination port"); //Testing client connection and stream setup
            
            while (true) {
                String inputToServer = dataIn.readUTF();
                if (inputToServer.startsWith("terminate")) {
                    //terminate the specific command
                    String[] commandParts = inputToServer.split(" ");
                    int terminateID = Integer.parseInt(commandParts[1]);
                    synchronized(Globals.lock) {
                        if (Globals.commands.containsKey(terminateID)) {
                            Globals.commands.get(terminateID).status = false; //Changes status of command to false
                        }
                    }
                    dataOut.writeUTF("Terminated command:" + terminateID);
                    dataOut.flush();
                }
            }
        } catch (Exception e) {
            //Only happens when client disconnects
            System.err.println("Client disconnected from termination port");
        }
    }
}
