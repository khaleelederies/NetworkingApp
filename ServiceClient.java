import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServiceClient implements Runnable {

    private Socket clientSocket;
    private BufferedReader in = null;
    final String dirs = System.getProperty("user.dir"); //Get Current directory
    String dirF= dirs.substring(0,dirs.length()-3); // get immediate directory before src folder
    public ServiceClient(Socket client)
    {

        this.clientSocket = client;
    }

    @Override
    public void run() {

        try {
            in = new BufferedReader(new InputStreamReader(
                    clientSocket.getInputStream()));
            String clientSelection;

            //Handle client input/selection
            while ((clientSelection = in.readLine()) != null) {
                switch (clientSelection) {
                    case "1":
                        receiveFile();
                        continue;
                    case "2":
                        String outGoingFileName;
                        while ((outGoingFileName = in.readLine()) != null) {
                            sendFile(outGoingFileName);
                        }
                        continue;
                    case "3":
                        listFiles();
                        continue;
                    case "4":
                        System.exit(1);

                        break;
                    default:
                        System.out.println("Incorrect command received.");
                        break;
                }

            }

        } catch (IOException ex) {

        }
    }
    // Receive File from Client
    public void receiveFile() {
        try {
            int bytesRead;

            DataInputStream clientData = new DataInputStream(clientSocket.getInputStream());

            String fileName = clientData.readUTF();


            File dir=new File(dirF+"/ServerFiles/"+ fileName); //Save the files received to a folder named ServerFiles
            OutputStream output = new FileOutputStream(dir);
            long size = clientData.readLong();
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }

            //Close Data and output stream
            output.close();
            clientData.close();

            System.out.println("File "+fileName+" received from client.");
        } catch (IOException ex) {
            System.err.println("Client error. Connection closed.");
        }
    }
    // Query the list of files available in the server
    public void listFiles()
    {
        try (Stream<Path> walk = Files.walk(Paths.get(dirF+"/ServerFiles"))) {

            List<String> result = walk.filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());

            result.forEach(System.out::println);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Send Files to the client that exist in the server as requested
    public void sendFile(String fileName) {
        try {
            File dir=new File(dirF+"/ServerFiles");
            File myFile = new File(dir,fileName);  //handle file reading
            System.out.println("is dir"+dir.isDirectory());
            byte[] mybytearray = new byte[(int) myFile.length()];

            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);

            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(mybytearray, 0, mybytearray.length);


            OutputStream os = clientSocket.getOutputStream();  //handle file send over socket

            DataOutputStream dos = new DataOutputStream(os); //Sending file name and file size to the server
            dos.writeUTF(myFile.getName());
            dos.writeLong(mybytearray.length);
            dos.write(mybytearray, 0, mybytearray.length);
            dos.flush();
            System.out.println("File "+fileName+" sent to client.");
        } catch (Exception e) {
            System.err.println("File does not exist!");
        }
    }
}
