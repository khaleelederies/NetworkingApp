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
    static String dirF= dirs.substring(0,dirs.length()-3);



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
            File dir=new File(dirF+"/ClientFiles");
            File myFile = new File(dir,fileName);
            if(!myFile.exists()) {
                System.out.println("File does not exist..");
                System.exit(1);
            }

            //If file exists, create input stream to read file
            bis = new BufferedInputStream(new FileInputStream(myFile));

            //Extract file name and send to server
            sOut.writeObject(fileName);
            System.out.println("Preparing file \"" + fileName + "\" to be sent");



            //Variables used to send file in packets
            int file_len = (int) myFile.length();
            int buff_size = 1024;
            int bytesRead = 0;
            int total_read_len = 0;
            byte[] buffer = new byte[buff_size];

            int file_len_2 = file_len;

            //Send server the size of the file
            sOut.writeInt(file_len);


            //Copy the file from the host and send to server in packets using a loop
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

            System.out.println("File "+fileName+" sent to Server.");


            sOut.flush();
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

            //Receive file size from Client
            file_size = sIn.readInt();

            //Create target for file and file output stream
            file = new File(dirF    +"/ClientFiles/"+ filename);	//Save the files received to a folder named ServerFiles
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

            System.out.println("File "+filename+" received from server.");

            bos.close();
            fileOut.close();

            System.out.println("File contained at: " + file.toPath());

            //add code to delete file if errors occured during upload

        }
        catch (Exception e) {
            System.err.println("Client error. Connection closed.");
        }


    }

    public static void printFileList() {


        try {

            //Receive the number of file names on server file list
            int noFiles = sIn.readInt();
            System.out.println("There are "+noFiles+" available files:");

            //For each file name received, print it to the screen
            for (int i = 0; i < noFiles; i++) {
                String incFName = (String) sIn.readObject();
                System.out.println(i+". "+incFName);

            }

            System.out.println("File list received.");


        } catch (Exception e) {
            System.out.println(e);
        }



    }

}
