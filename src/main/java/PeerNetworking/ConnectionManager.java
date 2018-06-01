package PeerNetworking;

import QueryObjects.STUNRegistration;
import QueryObjects.UserData;
import com.google.gson.Gson;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

//Singleton - tracks open connections and sockets
//  used to provide peer connection with port used to establish IP and port on
//  STUN server and passed to various windows to properly close connections
//  on program exit
public class ConnectionManager {
    private static ConnectionManager manager = null;
    public synchronized static ConnectionManager getConnectionManager(UserData user)throws IOException{
        if (manager == null) manager = new ConnectionManager(user);
        return manager;
    }

    private static final String API_TOKEN =  "fXtas7yB2HcIVoCyyQ78";
    //Remote: "hwsrv-265507.hostwindsdns.com"
    private static final String STUN_ADDRESS = "hwsrv-265507.hostwindsdns.com";
    //private static final String STUN_ADDRESS = "localhost";
    private static final int STUN_PORT = 15000;
    private static final int STUN_TIMEOUT = 10*1000;
    private static Gson gson = new Gson();
    private UserData user = null;
    private ArrayList<Socket> openSockets = new ArrayList<>();
    private int nextPort = 59870;
    private Socket nextSocket = null;


    public ConnectionManager(UserData usr){
        user = usr;
    }

    public synchronized int getNextSocket(){
        try {
            findNextSocket();
        }
        catch(SocketException e){
            e.printStackTrace();
            return -1;
        }
        return nextPort;
    }

    private void getNewPort(){
        nextPort++;
    }

    //will need to be modified to send JWT with user
    public int connectToStun(){
        boolean badPort = true;
        do{
            try {
                nextSocket = new Socket();
                nextSocket.setReuseAddress(true);
                nextSocket.bind(new InetSocketAddress(nextPort));
                nextSocket.connect(new InetSocketAddress(STUN_ADDRESS, STUN_PORT), STUN_TIMEOUT);
                STUNRegistration validation = new STUNRegistration(user, API_TOKEN, nextPort);
                String message = gson.toJson(validation);
                sendMessage(message);
//                String response = getMessage(); //? response from stun server ?
////                String response = "nope";
//                System.out.printf("RESPONSE FROM STUN: %s\n", response);
                badPort = false;
                nextSocket.close();
            } catch (IOException e) {
                System.out.println("Could not connect to STUN server on port incrementing");
                getNewPort();
            }
        }while(badPort);
        return nextPort;
    }

    private void findNextSocket() throws SocketException {
        nextSocket = new Socket();
        if (nextPort > 65535) throw new SocketException();
        while (!nextSocket.isConnected()) {
            try {
                nextSocket.setReuseAddress(true);
                nextSocket.bind(new InetSocketAddress(nextPort));
                nextSocket.connect(new InetSocketAddress(STUN_ADDRESS, STUN_PORT), STUN_TIMEOUT);
                STUNRegistration validation = new STUNRegistration(user, API_TOKEN, nextPort);
                String json = gson.toJson(validation);
                System.out.println(json);
                sendMessage(json);
                String res = "";

                try {
                    res = getMessage();
                }
                catch(SocketTimeoutException e){
                    e.printStackTrace();
                    System.out.println("Socket receive timeout");
                    nextSocket.close();
                    return;
                }
                nextSocket.close();
                System.out.println("Response:" + res);
                System.out.println("Port: " + nextPort);
                break;
            }
            catch(IOException e){
                nextPort++;
            }
        }
    }

    private String getMessage() throws IOException, SocketTimeoutException {
        BufferedReader bufferedReader = getBuffer();
        try {
            return bufferedReader.readLine();
        }
        catch(SocketTimeoutException e){
            throw e;
        }
    }


    private BufferedReader getBuffer() throws IOException{
        InputStream inputStream = nextSocket.getInputStream();
        return new BufferedReader((new InputStreamReader(inputStream)));
    }


    private void sendMessage(String message) throws IOException {
        PrintWriter out = writeToBuffer();
        out.println(message);
    }

    private PrintWriter writeToBuffer() throws IOException{
        OutputStream out = nextSocket.getOutputStream();
        return new PrintWriter(out, true);
    }
}
