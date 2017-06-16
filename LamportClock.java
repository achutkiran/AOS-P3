import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

//Implementing lamport's logical clock. 
public class LamportClock {
    private int d  = 1;
    private int clock = 0;
    public LamportClock() {}
    public LamportClock(int d) {
        this.d = d;
    }

    //Increment on local event
    public synchronized void local_event(){ 
        //System.out.println(Thread.currentThread().getName());
        clock += d; 
    }

    //On msg event
    public synchronized void msg_event(int msg_clock) {
        this.local_event();
        if (msg_clock + d >= this.clock) {
            clock = msg_clock + d;
        }
    }
    public int increment(){
        return d;
    }
    public int peek(){ 
        return clock; 
    }
}
