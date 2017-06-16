import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class NodeListenerThread extends Thread {    
    private Node node_reference; //Object for main Node Class.
    public NodeListenerThread(Node n) {
        this.node_reference = n; //Setting node_reference object to actual Node class.
    }
    //This thread will be online forever, and so it will keep on accepting input messages. 
    @Override
    public void run() {
        try(ServerSocket serv = new ServerSocket(node_reference.getPort())) {
            while(true) {
                try {
                    Socket conn = serv.accept();
                    InputStream in = conn.getInputStream();
                    ObjectInputStream instream = new ObjectInputStream(in);
                    try {
                        Message msg = (Message) instream.readObject();
                        //This will decide what to do with the message received.
                        node_reference.mutex.deliverMessage(msg);
                    } catch (ClassNotFoundException ex) {
                        System.err.println(ex);
                    }
                } catch(IOException ex) {
                    System.err.println(ex);
                }
            }

        } catch(IOException ex) {
            System.err.println("couldn't start the server");
            
        }
    }
}
