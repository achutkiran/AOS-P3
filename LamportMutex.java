import java.util.ArrayList;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.List;

public class LamportMutex implements Mutex {
    private Node node_ref;
    List<Message> requestList;

    private boolean request_made;
    public List<Integer> pending_replies;
    public List<Integer> terminate_pending;
    public Object request_lock = new Object();

    public LamportMutex(Node n) {
        node_ref = n;
        //Every node will maintain a request list. 
        requestList = Collections.synchronizedList(new ArrayList<Message>() {
            public synchronized boolean add(Message msg) {
                boolean ret = super.add(msg);
                //The one with lowest timestamp will be satisfied first. 
                Collections.sort(requestList);
                return ret;
            }
        });
        terminate_pending = new ArrayList<>(node_ref.other_pids);
    }

    //Add pending request messages to list.
    public synchronized void queue_request(Message msg) {
        requestList.add(msg);
    }

    public synchronized void releaseRequest(Message msg) {
        //synchronized(request_lock){
            int pid = msg.getSender();
            //the first msg in the list should be the one to be released. otherwise something is wrong 
            //Always remove the first one from requestlist.
            if (requestList.get(0).getSender() == pid) {
                requestList.remove(0);
            }else if (requestList.get(1).getSender() == pid) {
                requestList.remove(1);
            }else{
                for(int i=2;i<requestList.size();i++){
                    if(requestList.get(i).getSender() == pid){
                        requestList.remove(i);
                        break;
                    }
                }
            }
        //}
    }

    //Other nodes will receive a msg where as local node wont and this is triggered as soons as cric exec is done.
    public synchronized void releaseRequest() {
        //this is called when the local node is done executing crit section         
        request_made = false;
        requestList.remove(0);
    }


    public synchronized boolean requestCritSection() {
        //on true, node can enter the crit section, on false node can not. and then node blocks exec till it gets critical section.
        //Only one cricsec req at a time.
        
            if (!request_made) {
                request_made = true;
                //Initialize data strcutres.
                pending_replies = new ArrayList<>(node_ref.other_pids);
                node_ref.multicastMessage("request");
            }
            if(requestList.get(0).getSender() == node_ref.getPid()) {
                //we're highest priority!, now wait for all replies
                if (pending_replies.isEmpty()) {
                    //No more pending replies, execute crit sec
                    return true;
                }
            }
            return false;
        
    }

    public synchronized void reply_request(Message msg) {
        //on receiving reply from other nodes.
        //Remember we cant exec crit unless we receive reply messages from all other nodes.
        //synchronized(request_lock){
            if(request_made) {
                pending_replies.remove(new Integer(msg.getSender()));
                System.out.println("pending replies: "+pending_replies);
                System.out.println("request queue:"+requestList);             
            }
        //}
    }

    @Override
	public synchronized void deliverMessage(Message msg) {
        //synchronized(request_lock){
            //System.out.println("Trying to lock time");
            //synchronized(node_ref.time_lock){
                //System.out.println("Time Locked");
        //System.out.println("Delivering "+msg.getType());
        		node_ref.localclock.msg_event(msg.getClock());
        		if (msg.getType().equals("request")) {
        			System.out.println(msg.getType() + ".from..." + msg);
        			this.queue_request(msg);
        			//Send reply to all requests whatsoever - unoptimized
        			//We are not implementing the optimized version
        			node_ref.send_message(msg.getSender(), "reply");
        		} else if (msg.getType().equals("release")) {
        			System.out.println(msg.getType() + ".from..." + msg);
        			this.releaseRequest(msg); //All other nodes will release the crit exec request.
                    //System.out.println("request queue:"+requestList);
        		} else if (msg.getType().equals("reply")) {
        			System.out.println(msg.getType() + ".from..." + msg);
        			this.reply_request(msg);
        		} else if(msg.getType().equals("terminate") && node_ref.finished == true && terminate_pending.isEmpty() != true){
        			//System.out.println(msg.getType());
                    terminate_pending.remove(new Integer(msg.getSender()));
        		} else if(msg.getType().equals("terminate") && node_ref.finished == true && terminate_pending.isEmpty() == true){
                    node_ref.canquit = true;
                }
                //System.out.println("Delivered "+msg.getType());
                //System.out.println("Trying to unlock time");
            //}
            //System.out.println("Time Unlocked");
        //}
	}

	@Override
    public void csEnter() {
        //synchronized(request_lock){
        System.out.println("In CS Enter");
            while(!this.requestCritSection()) {
                //System.out.println("request queue:"+requestList); 
            }
            System.out.println("Out CS Enter");
        //}
    }

	@Override
    public void csLeave() {
        System.out.println("In CS Leave");
		this.releaseRequest(); //Remove satisfied critical section request only on this nodes perspective
		node_ref.multicastMessage("release");  //Supposed to send release msg to all other nodes.
        System.out.println("Out CS Enter");
	}

	@Override
	public void executeSelfRequestMsg(Message msg) {
		this.queue_request(msg);
	}
}
