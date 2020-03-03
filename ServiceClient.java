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
    private FileOutputStream fileOut;
    private BufferedInputStream bis;
    private BufferedOutputStream bos;
    private String filename;
    private File file;
    private int file_size;
    
    final String dirs = System.getProperty("user.dir"); 	//Get Current directory
    String dirF= dirs.substring(0,dirs.length()-3); 		
    
    
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
        	        	
            //Select protocol to use based on input received from client
            while (true) {
            	
            	int clientSelection = sIn.readInt();
            	
                switch (clientSelection) {
                	
                	//Receive file from client
                    case 1:
                        receiveFile();
                        continue;
                        
                    //Send file to client    
                    case 2:
                        sendFile();
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
                        //System.out.println("Incorrect command received.");
                    	//System.out.println()
                        continue;
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
            
            //Create target for file and output streams
            file = new File(dirs+"/ServerFiles/"+ filename);	//Save the files received to a folder named ServerFiles
            fileOut = new FileOutputStream(file);
            bos = new BufferedOutputStream(fileOut);
            
            //Receive file from Client
            byte[] contents = new byte[10000];

            int bytesRead = 0; 
            
            while((bytesRead=sIn.read(contents))!=-1)
                bos.write(contents, 0, bytesRead); 
            
            bos.flush();
 
            System.out.println("File "+filename+" received from client.");

            System.out.println("File contained at: " + file.toPath());
                        
        }
        catch (Exception e) {
            //System.err.println("Client error. Connection closed.");
        	System.err.println(e);
        }
    }
    
    // Query the list of files available in the server
    public void listFiles()
    {
    	System.out.println("Start of listfiles method");	
        try {
/*
 * 			Use this to generate new filelist files whenever files are added
 */
        	
/*			Stream<Path> walk = Files.walk(Paths.get(dirs+"/ServerFiles"));
        	
    		System.out.println("Path stream successful");
    		
        	//Create list of file names of available files
            List<String> result = walk.filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());
                             
*/
        	
        	File fileList =new File(dirs+"/ServerFiles/filelist.txt");
        	if(fileList.exists()) {
                System.out.println("File does not exist..");
                System.exit(1);
            }
        	
        	Scanner sf = new Scanner(fileList);
        	
        	//Calculate and send number of files the client is going to receive
        	int files = 0;
        	
        	while (sf.hasNext()) {
				sf.nextLine();
				files++;
				
			}
        	sf.close();
        	sOut.writeInt(files);
        	
        	//Send the file names to the client
        	sf = new Scanner(fileList);
        	
        	while (sf.hasNext()) {
        		sOut.writeObject(sf.nextLine());
			}
        	
        	sf.close();
	
        }
        catch (Exception e) {
            System.err.println("Exception: "+e);
        }
    }
    
    //Send file requested by the client from the server
    public void sendFile() {
   
        try {
        	
        	//Receive filename from client of file to send from server
        	filename = (String) sIn.readObject();
            
            //Get file specified by user and check if it exists 
            File dir=new File(dirs+"/ServerFiles");
            File myFile = new File(dir,filename);
            if(!myFile.exists()) {
                System.out.println("File does not exist..");
                System.exit(1);
            }
            
            //If file exists, create input stream to read file
            FileInputStream fis = new FileInputStream(myFile);
            bis = new BufferedInputStream(fis); 
            
            System.out.println("Preparing file \"" + filename + "\" to be sent"); 
            
            //Send file size to client
            int file_len = (int) myFile.length();
            sOut.writeInt(file_len);
            
            //Send file to client
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
            bis.close();

            System.out.println("File "+filename+" sent successfully.");
            
        }
        catch (Exception e) {
            System.err.println("Exception: "+e);
        }
           
    }
}
