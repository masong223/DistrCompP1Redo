import java.io.*;
import java.net.*;
import java.util.Scanner;

public class myftp {
public static void main(String[] args) {
    String machineName = args[0];
    int port = Integer.parseInt(args[1]);

    try {
    Socket socket = new Socket(machineName, port);
    DataInputStream in = new DataInputStream(socket.getInputStream());
    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
    System.out.println("Connected to server and established I/O"); //Testing connection to server
    Scanner scanner = new Scanner(System.in);
    while (true) {
        System.out.print("myftp> ");
        String inputToServer = scanner.nextLine();
        if (inputToServer.equals("quit")) {
            break;
        }
        out.writeUTF(inputToServer); // maybe 
        out.flush(); //

        // Get command
        // Probably If() for get command 
       // String filename = in.readUTF(); //file name
       // System.out.println(filename); //Testing response from server

       // long fileSize = in.readLong(); // file size
        //System.out.println(fileSize); 
        
         //Testing file path for get command
       // try (FileOutputStream fileOut = new FileOutputStream(filename)) {
       //     byte[] payload = new byte[(int) fileSize];
       //     in.readFully(payload); // getting data from server
            
       //     fileOut.write(payload); //writing data to file
        //    fileOut.flush();                       

       // } catch (IOException e) {
       //     System.out.println("Error writing file ");
      //  } // End of get command


      //If Put command
      System.out.println(inputToServer);
      String[] commandParts = inputToServer.split(" ");
      String fileName = commandParts[1];

      File fileToSend = new File(fileName);

        if (!fileToSend.exists()) {
            System.out.println("File does not exist");
            continue;
        } // file exists
        out.writeLong(fileToSend.length());
        out.flush(); 
        try (FileInputStream fileIn = new FileInputStream(fileName)){
            byte[] payLoad = new byte[(int) fileToSend.length()];
            fileIn.read(payLoad);
            out.write(payLoad);
            out.flush();
        } catch (IOException e) {
            System.out.println("Error client reading file");
        }


    }

    } catch (IOException e) {
        e.printStackTrace();
        System.out.println("Error connecting to server");
    }
}
}