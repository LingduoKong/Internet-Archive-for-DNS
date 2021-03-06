/**
 * Created by wuyanzhe on 1/25/15.
 */
import java.net.*;
import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;

public class SerialServer extends Thread{
    protected static boolean serverContinue = true;
    protected Socket clientSocket;
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        if(args.length==0) {
            try {
                serverSocket = new ServerSocket(10008);
                try {
                    while (serverContinue) {
                        serverSocket.setSoTimeout(60000);
                        System.out.println("Waiting for Client");
                        try {
                            new SerialServer(serverSocket.accept());
                        } catch (SocketTimeoutException ste) {
                            System.out.println("Timeout Occurred.");
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Accept failed.");
                    System.exit(1);
                }
            } catch (IOException e) {
                System.err.println("Could not listen on port: 10007.");
                System.exit(1);
            } finally {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    System.out.println("Could not close port: 10007");
                    System.exit(1);
                }
            }
        } else {
            if (args[0].equalsIgnoreCase("show")) {
                if (args.length == 2) {
                    //need to implement select query here
                    try {
                        HistoryCheck.historyCheck(args[1]);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    for (IPrecord iPrecord : IPrecord.getIPRecord(args[1])) {
                        System.out.println(iPrecord.toString());
                    }
                }
            }
        }
    }

    private SerialServer (Socket clientSoc){
        this.clientSocket = clientSoc;
        start();
    }

    public void run() {
        System.out.println("New TCP connection thread started.");
        try {
            ObjectOutputStream out = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(
                    clientSocket.getInputStream());

            ArrayList<record> list = null;
            recordPacket packet = null;
            Object object = null;
            try {
                object = in.readObject();
                //packet = (recordPacket) in.readObject();
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
            if (object instanceof String) {
                String query = (String)object;
                //System.out.println(query);
                System.out.println("Server received query "+ query + " from Client");
                String[] parts = query.split(" ");
                ArrayList<String> result = new ArrayList<String>();
                if(parts.length == 5){
                    result = store.get_valid_ip(query);
                }
                if(parts.length == 4){
                    result = store.whocansee(query);
                }
                if(parts.length == 2){
                    result = store.showWebsites();
                }
                for(String s: result){
                    System.out.println(s);
                }
                out.writeObject(result);
                out.flush();
                out.close();
                in.close();
                clientSocket.close();
            } else {
                packet = (recordPacket)object;
                System.out.println("Server received point: " + list + " from Client");
                list = packet.getList();
                if(!store.ClientIPexist(packet.getIP(),packet.getLocation())){
                store.storeClientIPInSQL(packet.getIP(),packet.getLocation());
                }
                store.storeRecordInMySQL(list, packet.getLocation());
                out.flush();
                out.close();
                in.close();
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Server error.");
            System.exit(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
