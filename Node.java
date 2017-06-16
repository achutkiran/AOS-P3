import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Node {
	public class NodeLookup {

		public HashMap<String, String> table;

		public NodeLookup(HashMap<String, String> table) {
			this.table = table;
		}

		public String getIP(int pid) {
			String ip_port = table.get(Integer.toString(pid));
			return ip_port.split(":")[0];
		}

		public int getPort(int pid) {
			String ip_port = table.get(Integer.toString(pid));
			return Integer.parseInt(ip_port.split(":")[1]);
		}
	}

	public Object time_lock = new Object();
	public LamportClock localclock;
	public Mutex mutex;
	public int pid;
	public int port;
	public boolean requested_crit = false;
	public NodeLookup lookup;
	public final List<Integer> other_pids;
	public int count_per_crit = 0;
	public long delay_per_crit = 0;
	public PrintWriter log_writer;
	public long crit_exec = 0;
	public int noofcrits = 0;
	public boolean finished = false;
	public boolean canquit = false;
	//public static boolean isLamport = false;

	//total crit section executions - Stop after what ever specified in config file
	public int crit_executions = 0;
	public static int intdelay = 0;

	//public Node(int pid, String ConfigFile,int p) {
	public Node(int pid, String ConfigFile, int isLamport) {
		this.pid = pid;
		//Extracting the first line of config file
		try (BufferedReader r = new BufferedReader(new FileReader(ConfigFile))) {
			String line = null;
			if ((line = r.readLine()) != null) {
				String[] parts = line.split(" ");
				if (parts.length == 4) {
					int n = Integer.parseInt(parts[0]);
					this.intdelay = Integer.parseInt(parts[1]);
					this.crit_exec = Long.parseLong(parts[2]);
					this.noofcrits = Integer.parseInt(parts[3]);
				}
				//System.out.println("crit_exec" + this.crit_exec);
			}
		} catch (IOException e) {
			System.out.println("cant open file");
		}
		//Extracting the rest of the config file
		this.lookup = new NodeLookup(ConfigReader.getLookup(ConfigFile));
		this.port = lookup.getPort(pid); //Getting the port for this process's node-id
		this.localclock = new LamportClock(ConfigReader.clocks.get(this.pid));
		this.other_pids = new ArrayList<Integer>();
		for (String i : lookup.table.keySet()) {
			int id = Integer.parseInt(i);
			if (id != this.pid) {
				other_pids.add(id);
			}
		}
		//isLamport = true;
		System.out.println("I got:"+isLamport);
		if(isLamport == 1){
			this.mutex = new LamportMutex(this);
			System.out.println("Executing Lamport");
		}
		else{
			this.mutex = new RicartMutex(this);
			System.out.println("Executing Ricart and Agarwala");
		}
	}

	public int getPid() {
		return this.pid;
	}

	public int getPort() {
		return this.port;
	}

	public synchronized void send_message(int receiver, String type) {
		//reply messages are not multicasts so we have to increase the local clock in send_message method
		if (type.equals("reply"))
			this.localclock.local_event();
		//Constructing the message using message builder - contains to whom, timestamp, from whom and type
		Message msg = new Message.MessageBuilder()
				.to(receiver)
				.from(this.pid)
				.clock(this.localclock.peek())
				.type(type).build();

		//If same node is receiver(i.e., receiver is self), the msg needs to be request so to add to list.
		if (receiver == this.pid && type.equals("request")) {
			this.mutex.executeSelfRequestMsg(msg);
		} else {
			//If not send message to appropriate node - sending to all is in a for loop in different function - multicastMessage()
			String receiver_ip = lookup.getIP(receiver); //get ip - dcxx
			int receiver_port = lookup.getPort(receiver); //get port
			try (Socket sock = new Socket(receiver_ip, receiver_port)) { // create a socket
				OutputStream out = sock.getOutputStream(); //Outputstream for sending messages
				ObjectOutputStream outstream = new ObjectOutputStream(out);
				outstream.writeObject(msg); //send the message.
				outstream.close();
				out.close();
				sock.close(); //close the socket
			} catch (IOException ex) {
				System.err.println("can't send message" + ex);
			}
		}
	}

	public void multicastMessage(String type) {
		multicastMessage(type, null);
	}

	public synchronized void multicastMessage(String type, List<Integer> pIds) {
		//Increment clock by one.
		logMessage("Trying to lock Time");
		synchronized(time_lock) {
			logMessage("Time Locked");
			this.localclock.local_event();
			//If request message we need to send that to self also to add it to the queue.
			if (type.equals("request")) {
				//Request message will contain the lamport logical clock time.
				Node.displayMessage("Sending request at " + localclock.peek());
				//Send request message
				send_message(this.pid, type);
			}
			if(pIds == null) {
				pIds = new ArrayList<>();
				for (String pid_str : lookup.table.keySet()) {
					pIds.add(Integer.parseInt(pid_str));
				}
			}
			//Send application/request message to all other nodes.
			for (Integer pid_int : pIds) {
				//we skip self as we already took care of that.
				//Node.displayMessage("Sending Req to "+ pid_int +" at clock:" + localclock.peek());
				if (pid_int != this.pid)
					send_message(pid_int, type);
			}
			logMessage("Time Unlocked");
		}
	}

	public synchronized void executeCS() {
		logMessage("Synchronized Execute CS called");
		writeToOutput("Entering");
		Node.displayMessage("In critical section.");
		this.crit_executions += 1;
		try {
			Thread.sleep(this.crit_exec);
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
		writeToOutput("Leaving");
		logMessage("Synchronized Execute CS finished");
	}

	public synchronized void writeToOutput(String action) {
		try {
			Writer sharedlog_writer = new PrintWriter(new FileWriter("results.out", true));
			sharedlog_writer.append(String.format("%s\t%s\t%s\n", this.pid, action, localclock.peek()));
			sharedlog_writer.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) {
		//boolean proto = false;
		//isLamport = Boolean.parseBoolean(args[2]);
		Node currentNode = new Node(Integer.parseInt(args[0]), args[1], Integer.parseInt(args[2]));
		//Node currentNode = new Node(Integer.parseInt(args[0]), args[1]);
		//Start listener.
		//Thread.currentThread().setName("Main-Thread");
		
		currentNode.displayMessage("Node - "+currentNode.pid);
		Thread listener = new NodeListenerThread(currentNode);
		//listener.setName("Node-Listener");
		listener.start();

		try {
			//Initial delay.
			Thread.sleep(8000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		while (true) {
			displayMessage("No. of CS executed:"+ currentNode.crit_executions);
			Random rand = new Random();
			try {
				Thread.sleep(intdelay);
			} catch (InterruptedException ex) {
				System.err.println(ex);
			}

			int decider = rand.nextInt(100) + 1;
			//Count the no. of crit execs to decide whether or not to execute.
			if(currentNode.canquit){
				break;			
			} else if (currentNode.crit_executions == currentNode.noofcrits) {
				//TODO: Check for breaks.
				displayMessage("All critical sections completed");
				currentNode.finished = true;
				//mutex.terminate_pending = 
				currentNode.multicastMessage("terminate");

			} else if (decider >= 1 && decider <= 10) {
				Node.logMessage("Multicasting application msg >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
				currentNode.multicastMessage("application");
				Node.logMessage("Multicasting application msg <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
			} else {
				long startTime = System.currentTimeMillis();
				//Request Critical section.
				Node.logMessage("Waiting for pending requests >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
				currentNode.mutex.csEnter();
				Node.logMessage("Waiting for pending requests <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
				//Execute critical section.
				logMessage("Calling Execute CS >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
				currentNode.executeCS();
				logMessage("Finished Execute CS <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
				//Leave Critical section.
				Node.logMessage("Leaving CS >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
				currentNode.mutex.csLeave();
				Node.logMessage("Leaving CS <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
			}
		}
		//listener.stop();
		System.out.println("Process Terminated");
		System.exit(0);
	}

	public static void displayMessage(String message) {
		System.out.println(message);
	}

	public static void logMessage(String message) {
		//System.out.println(message);
	}
}