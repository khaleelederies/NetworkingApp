import java.net.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
public class FileServer {

    private static ServerSocket serverSocket;
    private static Socket clientSocket = null;

    public static void main(String[] args) throws IOException {

        try {
            //get the server online
            serverSocket = new ServerSocket(5000);			//Create serverSocket to handle client connection requests
            System.out.println("Server started.");
        } catch (Exception e) {
            System.err.println("Port already in use.");
            System.exit(1);
        }


        while (true) {
            try {
                clientSocket = serverSocket.accept();
                System.out.println("Accepted connection : " + clientSocket);
                
                //Create threads to handle every client connection
                Thread t = new Thread(new ServiceClient(clientSocket));

                t.start();

            } catch (Exception e) {
                System.err.println("Error in connection attempt.");
            }
        }
    }
}
