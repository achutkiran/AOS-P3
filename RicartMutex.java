import java.util.ArrayList;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.List;

public class RicartMutex implements Mutex {

	private Node nodeRef;
	List<Integer> requestList;

	private boolean requestMade;
	public Object request_lock = new Object();
	public List<Integer> pending_replies;
	public List<Integer> terminate_pending;
	int clock;

	public RicartMutex(Node n) {
		nodeRef = n;
		//Every node will maintain a request list.
		requestList = new ArrayList<>();
		terminate_pending = new ArrayList<>(nodeRef.other_pids);
	}

	//Add pending request messages to list.
	public synchronized void addRequest(Integer nodeId) {
		Node.logMessage("Inside syncronized addRequest");
		requestList.add(nodeId);
		Node.logMessage("Leaving syncronized addRequest");
	}

	public synchronized void releaseRequest() {
		Node.logMessage("Inside syncronized releaseRequest");
		//this is called when the local node is done executing crit section
		Node.logMessage("Trying to lock request in release request.");
		synchronized(request_lock){
			Node.logMessage("Request Locked in release request");
			requestMade = false;
			clock = -1;
			Node.logMessage("Request unlocked in release request");
		}
		Node.logMessage("Leaving syncronized releaseRequest");
	}

	public synchronized void reply_request(Message msg) {
		Node.logMessage("Inside synchronized reply_request");
		//on receiving reply from other nodes.
		//Remember we cant exec crit unless we receive reply messages from all other nodes.
		if(requestMade) {
			pending_replies.remove(new Integer(msg.getSender()));
			nodeRef.displayMessage("Pending replies: "+pending_replies);
			nodeRef.displayMessage("Request queue:"+requestList);
		}
		Node.logMessage("Leaving synchronized reply_request");
	}


	@Override
	public synchronized void deliverMessage(Message msg) {
		Node.logMessage("Inside synchronized deliverMessage.");
		Node.logMessage("Trying to lock request in deliverMessage.");
		synchronized(request_lock) {
			Node.logMessage("Locked reuqest, trying to lock time in release deliverMessage.");
			synchronized(nodeRef.time_lock) {
				Node.logMessage("Request locked in deliverMessage.");
				Node.logMessage("in deliverMessage "+msg.getType());
				nodeRef.localclock.msg_event(msg.getClock());
				if (msg.getType().equals("request")) {
					nodeRef.displayMessage(msg.getType() + ".from..." + msg);
					// Change
					if(requestMade && (clock < msg.clock || (clock == msg.clock && msg.to < msg.from))) {
						if(clock < msg.clock)
							Node.logMessage("Request time of this node is less than request time of the sender node.");
						else if(clock == msg.clock && msg.to < msg.from)
							Node.logMessage("Request time of this node is same as the request time of the sender node but its id is less than sender id.");
						else
							Node.logMessage("It should not be here!!");
						Node.logMessage("Trying to call synchronized add request");
						this.addRequest(msg.from);
					} else {
						Node.logMessage("Trying to call synchronized send_message.");
						nodeRef.send_message(msg.getSender(), "release_reply");
					}
				} else if (msg.getType().equals("release_reply")) {
					nodeRef.displayMessage(msg.getType() + ".from..." + msg);
					Node.logMessage("Trying to call synchronized send_message.");
					this.reply_request(msg);
				} else if(msg.getType().equals("terminate") && nodeRef.finished == true && terminate_pending.isEmpty() != true){
        			//System.out.println(msg.getType());
                    terminate_pending.remove(new Integer(msg.getSender()));
        		} else if(msg.getType().equals("terminate") && nodeRef.finished == true && terminate_pending.isEmpty() == true){
                    nodeRef.canquit = true;
                }
				Node.logMessage("Time unlocked in deliverMessage.");
			}
			Node.logMessage("Request unlocked in deliverMessage.");
		}
		Node.logMessage("Leaving synchronized deliverMessage.");
	}

	public synchronized boolean checkEmpty() {
		return pending_replies.isEmpty();
	}

	@Override
	public void csEnter() {
		Node.logMessage("Inside CS Enter and trying to lock lock request");
		synchronized(request_lock){
			Node.logMessage("Locked Request in CS Enter");
			this.requestMade = true;
			this.pending_replies = new ArrayList<>(nodeRef.other_pids);
			Node.logMessage("Pending replies: "+ this.pending_replies);
			Node.logMessage("Calling synchronized multicastMessage");
			nodeRef.multicastMessage("request");
			Node.logMessage("Releasing request lock in CS Enter");
		}
		
		while(!checkEmpty()) {
		}
		Node.logMessage("Leaving CS Enter");
	}

	@Override
	public void csLeave() {
		Node.logMessage("Inside CS Leave and call synchronized release request");
		this.releaseRequest(); //Remove satisfied critical section request only on this nodes perspective
		Node.logMessage("Call synchronized multicastMessage");
		nodeRef.multicastMessage("release_reply", requestList);
		requestList = new ArrayList<>();
		Node.logMessage("Leaving CS Leave");
	}

	@Override
	public void executeSelfRequestMsg(Message msg) {
		this.clock = msg.clock;
	}
}