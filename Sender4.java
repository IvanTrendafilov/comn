/* Ivan Trendafilov 0837795 */
/* Experiment configuration: 
 * timeout = 500 ms
 * 
 * ipfw add pipe 100 in
 * ipfw add pipe 200 out
 * ipfw pipe 100 config delay 100ms plr 0.005 bw 10Mbits/s
 * ipfw pipe 200 config delay 100ms plr 0.005 bw 10Mbits/s
 * 
 */
/**
 * Sender4.java
 * Packet design: |Seq No.|    DATA DATA DATA DATA DATA    |  EOF  |
 * Packet size:   |2 bytes|          1021 bytes            |1 byte |
 * 0 means EOF. That is last packet.
 * 
 * Sends packets within a given window and starts a Timer thread attached to each packet.
 * If packet is acknowledged, the Timer thread exists. If it is not, the timer thread calls
 * a timeout and retransmits the packet.
 * ACK Thread receives ACKs and processes them. If an ACK is within its window, the ACK Thread
 * adds it to the list of ACKed packets (which cleanly kills the timer thread with that seq. no.)
 * and removes it from the set of unACKed packets. If the ACK is equal to the base of the window, the base is 
 * set to the first element of the unACKed packet sorted set. (This triggers a new send)
 * 
 * Debug level: 
 * 0 for completely silent. 
 * 1 for start/end messages only.
 * 2 for verbose mode. Fair warning: this slows everything down.
 */

import java.io.*;
import java.net.*;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
class Sender4 {
	public static short base;
	public static int DEBUG = 1;
	public volatile static boolean waiting = true;
	public static short window;
	final public static int timeout = 500;
	public static DatagramSocket clientSocket;
	public static SortedSet<Short> unACKedSeqNo = new TreeSet<Short>();
	public static TreeMap <Short,DatagramPacket> packets = new TreeMap<Short,DatagramPacket>();
	
	public static void main(String args[]) throws Exception {
		
		if(args.length != 4) {
			System.out.println("Usage: Sender4 localhost <Port> <Filename> [WindowSize]");
			System.exit(0);
		}
		else {
			long time1,time2 = 0;
			if(DEBUG > 0) System.out.println("Sending in progress!");
			short seqNo=0, nextseqnum;
			int ackPort, port = Integer.parseInt(args[1]);
			window = Short.parseShort(args[3]);
			// set a port for ACKs
			if (port==1) ackPort = port++; 
			else ackPort = port--;
			// read file
	        FileInputStream fis = new FileInputStream(args[2]); 
			InetAddress IPAddress = InetAddress.getByName(args[0]);
			clientSocket = new DatagramSocket();
			// buffers
	        byte[] packet = new byte[1024];
 		    byte[] fileBuffer = new byte[1021];
		      try {
		    	 for (int readNum; (readNum = fis.read(fileBuffer)) != -1;) {
		    		// adds a sequence number to the packet 
			    	seqNo++;
		    		packet[0] = ByteUtils.convertToBytes(seqNo)[0];
		    		packet[1] = ByteUtils.convertToBytes(seqNo)[1];
		    		for(int j=0;j<readNum;j++) {
		    			packet[j+2] = fileBuffer[j];
		    		}
		    		// adds EOF byte to the end of the packet
		    		if(readNum == 1021) {
						packet[1023] = 1; 
		    		}
		    		else {
		    			packet[1023] = 0;
		    		}
		    		DatagramPacket pck = new DatagramPacket(packet, packet.length,IPAddress,port);
		    		// keep the file in memory for rapid access
		    		packets.put(seqNo,pck);
		    		// keep track of all unACKed sequence numbers
		    		unACKedSeqNo.add(seqNo);
					// flush the buffers
					fileBuffer = new byte[1021];
					packet = new byte[1024];
		            }
		        } catch (IOException ex) { }
		        time1 = System.currentTimeMillis();
		        // prepare & start ACKThread to receive & process ACKs
		        Thread ackThread = new Thread(new ACKThread(ackPort, packets.lastKey(), timeout));
		        ackThread.start();
		        ackThread.setPriority(Thread.MAX_PRIORITY-1);
		        // initial state
		        base = 1;
		        nextseqnum = 1;
		        time1 = System.currentTimeMillis();
		        while(waiting) {
		        	// if next sequence number is within window, send
		        	while(nextseqnum<base+window) {
		        		if(packets.get(nextseqnum) != null) {
		        			clientSocket.send(packets.get(nextseqnum));
		        			// start a Timer thread for this packet
		        			new Thread(new Timer(nextseqnum)).start();
		        			if(DEBUG > 1) System.out.println("Active timer threads: "+(Thread.activeCount() - 3));
		        		}
		        		nextseqnum++;
		        	}
		        }
		        time2=System.currentTimeMillis();
		        if(DEBUG > 0) {
		        	System.out.println("File sent!");
		        	File f = new File(args[2]);
			        System.out.println("Throughput: "+(f.length()/1024/((float)(time2-time1)/(float)1000))); }
		        }
			clientSocket.close();
		}
	/* Synchronized getters / setters for inter-Thread communication. 
	 * (this is to prevent variables/lists left in in inconsistent state)
	 */
	
	public static synchronized void remSeq(short seq) {
		unACKedSeqNo.remove(seq);
	}
	public static synchronized boolean isSeqEmpty() {
		return unACKedSeqNo.isEmpty();
	}
	public static synchronized short seqFirst() {
		return unACKedSeqNo.first();
	}
	public static synchronized void setBase(short _base) {
		base = _base;
	}
	public static synchronized short getBase() {
		return base;
	}
	public static synchronized void sendPacket(DatagramPacket p) {
		try {
			clientSocket.send(p);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	}
	