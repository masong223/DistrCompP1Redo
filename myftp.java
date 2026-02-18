import java.io.*;
import java.net.*;

public static void main(String[] args) {
    String machineName = args[0];
    int port = Integer.parseInt(args[1]);

    try {
    Socket socket = new Socket(machineName, port);
    DataInputStream in = new DataInputStream(socket.getInputStream());
    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
    System.out.println("Connected to server and established I/O"); //Testing connection to server

    while (true) {
        System.out.print("myftp> ");
        String input = in.readUTF();
        if (input.equals("quit")) {
            break;
        }
        out.writeUTF(input);
    }

    } catch (IOException e) {
        e.printStackTrace();
        System.out.println("Error connecting to server");
    }
}