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
    private ObjectInputStream sIn;
    private ObjectOutputStream sOut;
    private FileInputStream fileIn;
    private FileOutputStream fileOut;
    private BufferedOutputStream bos;
    private String filename;
    private File file;
    private int file_size;
    
    final String dirs = System.getProperty("user.dir"); 	//Get Current directory
    String dirF= dirs.substring(0,dirs.length()-3); 		//Get immediate directory before src folder
    
    
    public ServiceClient(Socket client)
    {
        this.clientSocket = client;

    }

    @Override
    public void run() {

        try {
        	
        	//Connect to host and get streams
        	sIn = new ObjectInputStream(this.clientSocket.getInputStream());
        	sOut = new ObjectOutputStream(this.clientSocket.getOutputStream());
        	
            int clientSelection = sIn.readInt();
            
            //Select protocol to use based on input recevied from client
            while (true) {
            	
                switch (clientSelection) {
                	
                	//Receive file from client
                    case 1:
                        receiveFile();
                        continue;
                        
                    //Send file to client    
                    case 2:
                        String outGoingFileName;
                        while ((outGoingFileName = in.readLine()) != null) {
                            sendFile(outGoingFileName);
                        }
                        continue;
                        
                    //Send client a list of available files
                    case 3:
                        listFiles();
                        continue;
                    
                    //Close connection socket with client and end thread     
                    case 4:
                        System.exit(1);
                        break;
                        
                    
                    default:
                        System.out.println("Incorrect command received.");
                        break;
                }

            }

        }
        catch (IOException ex) {
        	System.out.println(ex);
        }
        
    }
    
    
    // Receive File from Client
    public void receiveFile() {
        try {

            //Receive filename from Client
            filename = (String) sIn.readObject();
            
            //Receive file size from Client
            file_size = sIn.readInt();
            
            //Create target for file and file output stream
            file = new File(dirs+"/ServerFiles/"+ filename);	//Save the files received to a folder named ServerFiles
            fileOut = new FileOutputStream(file);
            
            //Create buffered output
            bos = new BufferedOutputStream(fileOut);
            
            byte[] buffer = null;
            int total_read_len = 0;
            
            //Receving file loop
            while( sIn.readBoolean() ){
                buffer = (byte[]) sIn.readObject();
                total_read_len += buffer.length;
                bos.write(buffer);

                System.out.println("Receive: " + (float)total_read_len/file_size*100 + "%");
            }

            System.out.println("File "+filename+" received from client.");

            bos.close();
            fileOut.close();
            
            System.out.println("File contained at: " + file.toPath());
            
            //add code to delete file if errors occured during upload
            
        }
        catch (Exception e) {
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
