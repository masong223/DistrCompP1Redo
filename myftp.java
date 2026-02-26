import java.io.*;
import java.net.*;
import java.util.Scanner;

public class myftp {
    public static void main(String[] args) {
        String machineName = args[0];
        int nport = Integer.parseInt(args[1]);
        int tport = Integer.parseInt(args[2]);

        try {
            Socket nsocket = new Socket(machineName, nport);
            Socket tsocket = new Socket(machineName, tport);
            DataInputStream nIn = new DataInputStream(nsocket.getInputStream());
            DataOutputStream nOut = new DataOutputStream(nsocket.getOutputStream());
            DataInputStream tIn = new DataInputStream(tsocket.getInputStream());
            DataOutputStream tOut = new DataOutputStream(tsocket.getOutputStream());
            System.out.println("Connected to server and established I/O"); // Testing connection to server
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("myftp> ");
                String inputToServer = scanner.nextLine();
                if (inputToServer.equals("quit")) {
                    nsocket.close();
                    tsocket.close();
                    scanner.close();
                    break;
                } else if (inputToServer.equals("terminate")) {
                    //terminate command uses the tport
                    tOut.writeUTF(inputToServer);
                    tOut.flush();
                    String responseFromServer = tIn.readUTF();
                    System.out.println(responseFromServer);
                }  else if (inputToServer.startsWith("get")) {
                    nOut.writeUTF(inputToServer); // maybe
                    nOut.flush();
                    int commandIdFromServer = nIn.readInt(); //GETS COMMAND ID FROM SERVER BEFORE RESPONDING TO COMMAND
                    System.out.println(commandIdFromServer); // Testing response from server

                    String serverResponse = nIn.readUTF();
                    if (serverResponse.equals("File does not exist")) {
                        System.out.println(serverResponse);
                        continue; //Exit and wait for another command if file doesn't exist
                    }
                    String filename = serverResponse; // file name if it exists 
                    System.out.println(filename); // Testing response from server

                    long fileSize = nIn.readLong(); // file size
                    System.out.println(fileSize);

                    // Testing file path for get command
                    try (FileOutputStream fileOut = new FileOutputStream(filename)) {
                        byte[] payload = new byte[(int) fileSize];
                        nIn.readFully(payload); // getting data from server

                        fileOut.write(payload); // writing data to file
                        fileOut.flush();

                    } catch (IOException e) {
                        System.out.println("Error writing file ");
                    }
                } else if (inputToServer.startsWith("put")) {
                    System.out.println(inputToServer);
                    String[] commandParts = inputToServer.split(" ");
                    String fileName = commandParts[1];

                    File fileToSend = new File(fileName);

                    if (!fileToSend.exists()) {
                        System.out.println("File does not exist");
                        continue;
                    } // file exists
                    nOut.writeUTF(inputToServer);
                    nOut.flush();
                    int commandIdFromServer = nIn.readInt(); //GETS COMMAND ID FROM SERVER BEFORE RESPONDING TO COMMAND
                    System.out.println(commandIdFromServer); // Testing response from server
                    
                    nOut.writeLong(fileToSend.length());
                    nOut.flush();
                    try (FileInputStream fileIn = new FileInputStream(fileName)) {
                        byte[] payLoad = new byte[(int) fileToSend.length()];
                        fileIn.read(payLoad);
                        nOut.write(payLoad);
                        nOut.flush();
                    } catch (IOException e) {
                        System.out.println("Error client reading file");
                    }
                } else { //If command is not quit, put, or get
                    nOut.writeUTF(inputToServer);
                    nOut.flush();
                    String responseFromServer = nIn.readUTF();
                    System.out.println(responseFromServer);
                }
                
            }
        } catch (IOException e) {
            System.out.println("Error connecting to server");
        }
    }
}