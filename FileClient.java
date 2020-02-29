import java.net.*;
import java.io.*;
import java.util.*;

public class FileClient {

    private static Socket sock;
    private static String fileName;
    private static BufferedReader stdin;
    private static PrintStream os;
    final static String  dirs = System.getProperty("user.dir");

    public static void main(String[] args) throws IOException {
    	
    	try {
            //Attempt to connect to the server
            sock = new Socket("localhost", 5000);							//using default values for now
            
            //Create inputStream to receive user input from keyboard
            stdin = new BufferedReader(new InputStreamReader(System.in));	
            
        }
        catch (Exception e) {
            System.err.println("Cannot connect to the server, try again later.");
            System.exit(1);
            
        }
        
        // Instantiate the outputStream to receive data from socket
        os = new PrintStream(sock.getOutputStream());
        
            
	    while(true) {
	    	
	        try {
	        	
	        	//User inputs the function they wish to perform
	        	//This sends a message to the server to notify it of which protocol to use based on function chosen
	            switch (Integer.parseInt(selectAction())) {
	            	
	            	//Upload file to server
	                case 1:
	                    os.println("1");
	                    sendFile();
	                    continue;
	                
	                //Download file from server
	                case 2:
	                    os.println("2");
	                    System.out.print("Enter file name: ");
	                    fileName = stdin.readLine();
	                    os.println(fileName);
	                    receiveFile(fileName);
	                    continue;
	                    
	                //Request list of available files on server
	                case 3:
	                    os.println("3");
	                
	                //Close socket connection and exit Client program
	                case 4:
	                    os.println("4");
	                    sock.close();
	                    System.exit(1);
	                    
	            }
	        }
	        catch (Exception e) {
	            System.err.println("Invalid input.");
	        }
	
        }

    }
    
    //Print user option list
    public static String selectAction() throws IOException {
    	
        System.out.println("1. Upload file.");
        System.out.println("2. Download file.");
        System.out.println("3. View list of files on server.");
        System.out.println("4. Exit.");
        System.out.print("\nMake selection: ");

        return stdin.readLine();
    }

    //Send files to the server from the "ClientFiles" folder
    public static void sendFile() {
        try {
        	
        	//Ask user for file to send
            System.out.print("Enter file name: ");
            fileName = stdin.readLine();

            //Get file specified by user 
            File dir=new File(dirs+"/ClientFiles");
            File myFile = new File(dir,fileName);
            if(!myFile.exists()) {
                System.out.println("File does not exist..");
                System.exit(1);
            }
            
            
            //If file exists, turn length into a bytearray
            byte[] mybytearray = new byte[(int) myFile.length()];
            

            //Sending file name and file size to the server
            DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
            dos.writeUTF(myFile.getName());
            dos.writeLong(mybytearray.length);
            dos.write(mybytearray, 0, mybytearray.length);
            dos.flush();
            
            
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
            //bis.read(mybytearray, 0, mybytearray.length);

            
            
            
            System.out.println("File "+fileName+" sent to Server.");
            
            
            
        } catch (Exception e) {
            System.err.println("Exception: "+e);
        }
    }
    // Method to receive files from the server save it to the ClientFiles Folder
    public static void receiveFile(String fileName) {
        try {
            int bytesRead;
            InputStream in = sock.getInputStream();

            DataInputStream clientData = new DataInputStream(in);

            fileName = clientData.readUTF();
            File dir=new File(dirs+"/ClientFiles/"+ fileName);
            OutputStream output = new FileOutputStream(dir);
            long size = clientData.readLong();
            // Split and write files in bytes/bits
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }

           //Close the inputStream and output stream
            output.close();
            in.close();

            System.out.println("File "+fileName+" received from Server.");
        } catch (IOException ex) {
            System.out.println("Exception: "+ex);
        }

    }
}
