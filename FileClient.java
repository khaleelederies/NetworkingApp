import java.net.*;
import java.io.*;
import java.util.*;

public class FileClient {

    private static Socket sock;
    private static String fileName;
    private static BufferedReader stdin;
    private static PrintStream os;
    final static String  dirs = System.getProperty("user.dir");
   static String dirF= dirs.substring(0,dirs.length()-3); // get immediate directory before src folder

    public static void main(String[] args) throws IOException {
        while(true) {
            try {
                //Attempt conneccting to the server
                sock = new Socket("localhost", 5000);
                stdin = new BufferedReader(new InputStreamReader(System.in));
            } catch (Exception e) {
                System.err.println("Cannot connect to the server, try again later.");
                System.exit(1);
            }

            os = new PrintStream(sock.getOutputStream()); // Instantiate the outputStream

            try {
                switch (Integer.parseInt(selectAction())) {
                    case 1:
                        os.println("1");
                        sendFile();
                        continue;
                    case 2:
                        os.println("2");
                        System.out.print("Enter file name: ");
                        fileName = stdin.readLine();
                        os.println(fileName);
                        receiveFile(fileName);
                        continue;
                    case 3:
                        os.println("3");
                    case 4:
                        os.println("4");
                        sock.close();
                        System.exit(1);
                }
            } catch (Exception e) {
                System.err.println("not valid input");
            }

        }

    }
    //Prompt the user to put in request

    public static String selectAction() throws IOException {
        System.out.println("1. Send file.");
        System.out.println("2. Recieve file.");
        System.out.println("3. View files.");
        System.out.println("4. Exit.");
        System.out.print("\nMake selection: ");

        return stdin.readLine();
    }

    //Send files to the server from the "ClientFiles" folder
    public static void sendFile() {
        try {
            System.out.print("Enter file name: ");
            fileName = stdin.readLine();

            File dir=new File(dirF+"/ClientFiles");
            System.out.println(dir);
            File myFile = new File(dir,fileName);
            byte[] mybytearray = new byte[(int) myFile.length()];
            if(!myFile.exists()) {
                System.out.println("File does not exist..");
                return;
            }

            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            //bis.read(mybytearray, 0, mybytearray.length);

            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(mybytearray, 0, mybytearray.length);

            OutputStream os = sock.getOutputStream();

            //Sending file name and file size to the server
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(myFile.getName());
            dos.writeLong(mybytearray.length);
            dos.write(mybytearray, 0, mybytearray.length);
            dos.flush();
            System.out.println("File "+fileName+" sent to Server.");
        } catch (Exception e) {
            System.err.println("Exceptionnnn: "+e);
        }
    }
    // Method to receive files from the server save it to the ClientFiles Folder
    public static void receiveFile(String fileName) {
        try {
            int bytesRead;
            InputStream in = sock.getInputStream();

            DataInputStream clientData = new DataInputStream(in);

            fileName = clientData.readUTF();
            File dir=new File(dirF+"/ClientFiles/"+ fileName);
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
