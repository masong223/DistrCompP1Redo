import java.io.*;
import java.net.*;
import java.util.Scanner;

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
        out.writeUTF(inputToServer);
        out.flush();
        String response = in.readUTF();
        System.out.println(response);
    }

    } catch (IOException e) {
        e.printStackTrace();
        System.out.println("Error connecting to server");
    }
}