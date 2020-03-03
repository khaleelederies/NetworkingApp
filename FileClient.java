import java.net.*;
import java.io.*;
import java.util.*;

public class FileClient {

    private static Socket sock;
    private static String fileName;
    private static BufferedReader stdin;
    private static PrintStream os;
    private static BufferedInputStream bis;
    private static BufferedOutputStream bos;
    private static FileOutputStream fileOut;
    private static ObjectInputStream sIn;
    private static ObjectOutputStream sOut;
    private static String filename;
    private static File file;
    private static int file_size;
    
    final static String  dirs = System.getProperty("user.dir");
    
    

    public static void main(String[] args) throws IOException {
    	
    	try {
            //Attempt to connect to the server and link input / output streams
            sock = new Socket("localhost", 5000);							//using default values for now
            
            //Create inputStream to receive user input from keyboard
            stdin = new BufferedReader(new InputStreamReader(System.in));
            sOut = new ObjectOutputStream(sock.getOutputStream());
            sIn = new ObjectInputStream(sock.getInputStream());
    
        }
        catch (Exception e) {
            System.err.println("Cannot connect to the server, try again later.");
            System.exit(1);
            
        }
                
	    while(true) {
	    	
	        try {
	        	
	        	int currentAction = selectAction();
	        	
	        	//User inputs the function they wish to perform
	        	//This sends a message to the server to notify it of which protocol to use based on function chosen
	            switch (currentAction) {
	            	
	            	//Upload file to server
	                case 1:
	                    sOut.writeInt(currentAction);
	                    sendFile();
	                    continue;
	                
	                //Download file from server
	                case 2:
	                	sOut.writeInt(currentAction);
	                    receiveFile();
	                    continue;
	                    
	                //Request list of available files on server
	                case 3:
	                	sOut.writeInt(currentAction);
	                	printFileList();
	                	continue;
	                
	                //Close socket connection and exit Client program
	                case 4:
	                	sOut.writeInt(currentAction);
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
    public static int selectAction() throws IOException {
    	
        System.out.println("1. Upload file.");
        System.out.println("2. Download file.");
        System.out.println("3. View list of files on server.");
        System.out.println("4. Exit.");
        System.out.print("\nMake selection: ");

        return Integer.parseInt(stdin.readLine());
    }

    //Send files to the server from the "ClientFiles" folder
    public static void sendFile() {
        try {
        	        	
        	//Ask user for file to send
            System.out.print("Enter file name: ");
            fileName = stdin.readLine();

            //Get file specified by user and check if it exists 
            File dir=new File(dirs+"/ClientFiles");
            File myFile = new File(dir,fileName);
            if(!myFile.exists()) {
                System.out.println("File does not exist..");
                System.exit(1);
            }
            
            //If file exists, create input stream to read file
            FileInputStream fis = new FileInputStream(myFile);
            bis = new BufferedInputStream(fis); 
            
            //Extract file name and send to server  
            sOut.writeObject(fileName);
            System.out.println("Preparing file \"" + fileName + "\" to be sent");
            
            //Send size of file
            int file_len = (int) myFile.length();
            sOut.writeInt(file_len);
            
            //Send file to server
            byte[] contents;
            long fileLength = file_len; 
            long current = 0;
             
            while(current!=fileLength){ 
                int size = 10000;
                if(fileLength - current >= size)
                    current += size;    
                else{ 
                    size = (int)(fileLength - current); 
                    current = fileLength;
                } 
                contents = new byte[size]; 
                bis.read(contents, 0, size); 
                sOut.write(contents);
                System.out.print("Sending file ... "+(current*100)/fileLength+"% complete!\n");
            }   
            
            sOut.flush();

            System.out.println("File "+filename+" received from server.");

            System.out.println("File contained at: " + file.toPath());
        }
        catch (Exception e) {
            System.err.println("Exception: "+e);
        }
        
        
    }       
    
    // Method to receive files from the server save it to the ClientFiles Folder
    public static void receiveFile() {
        
        try {
        	
        	//Capture name of desired file from user and send to server
        	System.out.print("Enter file name: ");
            filename = stdin.readLine();
            sOut.writeObject(filename);
            
            //Receive file size from server
            file_size = sIn.readInt();
            
            //Create target for file and file output stream
            file = new File(dirs+"/ClientFiles/"+ filename);	//Save the files received to a folder named ServerFiles
            fileOut = new FileOutputStream(file);
            bos = new BufferedOutputStream(fileOut);
            
            //Receive file from server
            byte[] contents = new byte[10000];

            int bytesRead = 0; 
            
            while((bytesRead=sIn.read(contents))!=-1)
                bos.write(contents, 0, bytesRead); 
            
            fileOut.flush();
            bos.flush();
 
            System.out.println("File "+filename+" received from client.");

            System.out.println("File contained at: " + file.toPath());
            
        }
        catch (Exception e) {
            System.err.println("Client error. Connection closed.");
        }
        
        
    }
    
    public static void printFileList() {
    		
    	System.out.println("List of available files:");
    	
    	try {
    		
    		int nofiles = sIn.readInt();
    		int count = 0;
    		
    		while(count != nofiles) {
    			
    			String fName = (String) sIn.readObject();
    			System.out.print(fName);   			    			
    			
    		}

    		sOut.flush();    		
    		
    		System.out.println("End of file list.");
    		
        }
        catch (Exception e) {
            //System.err.println("Client error. Connection closed.");
        	System.err.println(e);
        }
	
    }
    
}
