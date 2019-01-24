package geekbrains.java.cloud.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientController {

    private final static String SERVER_ADDR = "localhost";
    private final static int SERVER_PORT = 8189;
    private Socket socket;
    //private InputStream in;
    private OutputStream out;
    private Scanner scanner;

    public void initConnection(){
        try{
            socket = new Socket(SERVER_ADDR, SERVER_PORT);
            //in = socket.getInputStream();
            out = socket.getOutputStream();
            scanner = new Scanner(socket.getInputStream());
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void sendMessage(byte[] arr) throws IOException{
        out.write(arr);
        out.flush();
    }

//    public byte[] readMessage() throws IOException {
//        byte[] arr = new byte[10];
//        in.read(arr);
//        return arr;
//    }

    public byte readByte(){
        return scanner.nextByte();
    }

    public boolean hasNext(){
        return scanner.hasNextByte();
    }

    public void closeConnection(){
        try {
            socket.close();
            out.close();
            //in.close();
            scanner.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }


}
