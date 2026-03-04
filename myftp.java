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
                if (inputToServer.endsWith("&")) {
                    String ToServer = inputToServer.substring(0, inputToServer.length() - 1).strip();
                    new Thread(() -> {
                        try {
                        processString(nsocket, tsocket, nIn, nOut, tIn, tOut, ToServer, scanner);
                    } catch (IOException e) {
                        System.out.println("Error processing command in background thread");
                    }
                    }).start();
                    continue;
                }
                processString(nsocket, tsocket, nIn, nOut, tIn, tOut, inputToServer, scanner);
            }
        } catch (IOException e) {
            System.out.println("Error connecting to server");
        }
    }

    private static void processString(Socket nsocket, Socket tsocket, DataInputStream nIn, DataOutputStream nOut, DataInputStream tIn, DataOutputStream tOut, String inputToServer, Scanner scanner) throws IOException {
        if (inputToServer.equals("quit")) {
            nsocket.close();
            tsocket.close();
            scanner.close();
        } else if (inputToServer.equals("terminate")) {
            // terminate command uses the tport
            tOut.writeUTF(inputToServer);
            tOut.flush();
            String responseFromServer = tIn.readUTF();
            System.out.println(responseFromServer);
        } else if (inputToServer.startsWith("get")) {
            nOut.writeUTF(inputToServer); // maybe
            nOut.flush();
            int commandIdFromServer = nIn.readInt(); // GETS COMMAND ID FROM SERVER BEFORE RESPONDING TO COMMAND
            System.out.println(commandIdFromServer); // Testing response from server

            String serverResponse = nIn.readUTF();
            if (serverResponse.equals("File does not exist")) {
                System.out.println(serverResponse);
            }
            String filename = serverResponse; // file name if it exists
            System.out.println(filename); // Testing response from server

            long fileSize = nIn.readLong(); // file size
            System.out.println(fileSize);
            byte[] payload = new byte[1000];

            // Testing file path for get command
            try (FileOutputStream fileOut = new FileOutputStream(filename)) {
                while (fileSize > 0) {
                    int bytesToRead = (int) Math.min(fileSize, payload.length);
                    nIn.readFully(payload, 0, bytesToRead); // getting data from server
                    fileOut.write(payload, 0, bytesToRead); // writing data to file
                    fileSize -= bytesToRead;
                }

                fileOut.flush();

            } catch (IOException e) {
                System.out.println("Error writing file ");
            } // get
        } else if (inputToServer.startsWith("put")) {
            System.out.println(inputToServer);
            String[] commandParts = inputToServer.split(" ");
            String fileName = commandParts[1];

            File fileToSend = new File(fileName);
            long fileSize = fileToSend.length();
            if (!fileToSend.exists()) {
                System.out.println("File does not exist");
            } // file exists
            nOut.writeUTF(inputToServer);
            nOut.flush();
            int commandIdFromServer = nIn.readInt(); // GETS COMMAND ID FROM SERVER BEFORE RESPONDING TO COMMAND
            System.out.println(commandIdFromServer); // Testing response from server

            nOut.writeLong(fileToSend.length());
            nOut.flush();
            byte[] payLoad = new byte[1000];

            try (FileInputStream fileIn = new FileInputStream(fileName)) {
                while (fileSize > 0) {
                    int bytesToRead = (int) Math.min(payLoad.length, fileSize);
                    int bytesRead = fileIn.read(payLoad, 0, bytesToRead); // reading data from file
                    if (bytesRead == -1) {
                        break; // End of file reached
                    }
                    nOut.write(payLoad, 0, bytesToRead); // sending data to server
                    nOut.flush();
                    fileSize -= bytesRead;
                }
            } catch (IOException e) {
                System.out.println("Error client reading file");
            }
        } else { // If command is not quit, put, or get
            nOut.writeUTF(inputToServer);
            nOut.flush();
            String responseFromServer = nIn.readUTF();
            System.out.println(responseFromServer);
        }
    }
}
