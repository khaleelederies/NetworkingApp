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

            int clientSelection;

            //Select protocol to use based on input recevied from client
            while (true) {

                switch (clientSelection = sIn.readInt()) {

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
            file = new File(dirF+"/ServerFiles/"+ filename);	//Save the files received to a folder named ServerFiles
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

            //Create list of file names of available files
            List<String> result = walk.filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());


            //Let client know how many files there are to view
            sOut.writeInt(result.size());

            //Loop through list, sending each file name to the client
            for (Iterator<String> iterator = result.iterator(); iterator.hasNext();) {
                sOut.writeObject(iterator.next());

            }

            System.out.println("File list sent to Client");

            sOut.flush();

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Send file requested by the client from the server
    public void sendFile() {

        try {

            //Receive filename from client of file to send from server
            filename = (String) sIn.readObject();


            //Get file specified by user and check if it exists
            File dir=new File(dirF+"/ServerFiles");
            File myFile = new File(dir,filename);
            if(!myFile.exists()) {
                System.out.println("File does not exist..");
                System.exit(1);
            }

            //If file exists, create input stream to read file
            bis = new BufferedInputStream(new FileInputStream(myFile));

            System.out.println("Preparing file \"" + filename + "\" to be sent");

            //Variables used to send file in packets
            int file_len = (int) myFile.length();
            int buff_size = 1024;
            int bytesRead = 0;
            int total_read_len = 0;
            byte[] buffer = new byte[buff_size];

            int file_len_2 = file_len;

            //Send client the size of the file
            sOut.writeInt(file_len);

            //Copy the file from the host and send to client in packets using a loop
            while( file_len_2 > 0 ){

                if( file_len_2 < buff_size ){
                    buffer = new byte[file_len_2];
                    bytesRead = bis.read(buffer);

                }
                else{
                    bytesRead = bis.read(buffer);

                }

                file_len_2 -= bytesRead;
                total_read_len += bytesRead;
                sOut.writeBoolean(true);
                sOut.writeObject(buffer);
                System.out.println("Sent: " + (float)total_read_len/file_len*100 + "%");
            }

            sOut.writeBoolean(false);

            System.out.println("File "+filename+" sent to Client.");

            sOut.flush();
        }
        catch (Exception e) {
            System.err.println("Exception: "+e);
        }

    }
}
