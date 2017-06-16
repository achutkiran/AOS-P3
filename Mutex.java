public interface Mutex {

	public void csEnter();
	public void csLeave();
	public void deliverMessage(Message msg);
	public void executeSelfRequestMsg(Message msg);
}
