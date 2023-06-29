import graphing.GraphNode;
import graphing.Graph; 
import node.blockchain.Transaction;
import node.blockchain.defi.DefiTransaction;
import node.communication.Address;
import node.communication.messaging.Message;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import java.io.FileReader;
import java.io.FileWriter; 
import java.io.IOException; 

// import javax.json.*;

/**
 * One shot client that communicates with the network's nodes
 * Usage: <[graph] [query <portNum>] [trans <portNum> <Transaction String Id>]>
 */
public class Client {

    public static void main(String[] args) throws FileNotFoundException {
        int port;
        int numNodes = 0;
        int startingPort = 0;

        try {
            String configFilePath = "src/main/java/config.properties";
            FileInputStream fileInputStream = new FileInputStream(configFilePath);
            Properties prop = new Properties();
            prop.load(fileInputStream);

            numNodes = Integer.parseInt(prop.getProperty("NUM_NODES"));
            startingPort = Integer.parseInt(prop.getProperty("STARTING_PORT"));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JsonArrayBuilder jsonNodes = Json.createArrayBuilder();
        JsonArrayBuilder jsonLinks = Json.createArrayBuilder();

        for (int i = 0; i < numNodes; i++) {
            port = startingPort + i;
            ArrayList<Address> localPeers = queryPeer(port);
            if (localPeers != null) {
                jsonNodes.add(Json.createObjectBuilder().add("id", String.valueOf(port)).add("group", 1));

                for(Address address : localPeers){
                    jsonLinks.add(Json.createObjectBuilder().add("source", String.valueOf(port)).add("target", String.valueOf(address.getPort())).add("value", 2));
                }
            }
        }

        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("nodes", jsonNodes).add("links", jsonLinks);
        JsonObject empJsonObject = jsonObjectBuilder.build();

        OutputStream os = new FileOutputStream("src/main/resources/graph.json");
        JsonWriter jsonWriter = Json.createWriter(os);
        jsonWriter.writeObject(empJsonObject);
        jsonWriter.close();
            
    }

    /**
     * Queries a specified node given its port, assuming localhost
     * @param port
     * @return ArrayList<Address> node's connection list
     */
    private static ArrayList<Address> queryPeer(int port){
        try {
            Socket s = new Socket("localhost", port);
            InputStream in = s.getInputStream();
            ObjectInputStream oin = new ObjectInputStream(in);
            OutputStream out = s.getOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(out);
            Message message = new Message(Message.Request.QUERY_PEERS);
            oout.writeObject(message);
            oout.flush();
            Message messageReceived = (Message) oin.readObject();
            ArrayList<?> localPeers = (ArrayList<?>) messageReceived.getMetadata();
            s.close();
            return (ArrayList<Address>) localPeers;
        } catch (IOException | ClassNotFoundException e) {
            //System.out.println("Error occurred");
        }
        return null;
    }

    private static void submitTransaction(int port, String transaction){
        try {
            Socket s = new Socket("localhost", port);
            OutputStream out = s.getOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(out);
            Message message = new Message(Message.Request.ADD_TRANSACTION, new DefiTransaction("me", "you", 5, null)); 
            oout.writeObject(message);
            oout.flush();
            Thread.sleep(2000);
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
