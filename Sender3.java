/* Ivan Trendafilov 0837795 */
/* Experiment 1 configuration: 
 * timeout = 50ms
 * 
 * ipfw add pipe 100 in
 * ipfw add pipe 200 out
 * ipfw pipe 100 config delay 10ms plr 0.005 bw 10Mbits/s
 * ipfw pipe 200 config delay 10ms plr 0.005 bw 10Mbits/s
 * 
 * Experiment 2 configuration:
 * timeout = 500ms
 * ipfw add pipe 100 in
 * ipfw add pipe 200 out
 * ipfw pipe 100 config delay 100ms plr 0.005 bw 10Mbits/s
 * ipfw pipe 200 config delay 100ms plr 0.005 bw 10Mbits/s
 * 
 * Experiment 3 configuration:
 * timeout = 5000ms
 * ipfw add pipe 100 in
 * ipfw add pipe 200 out
 * ipfw pipe 100 config delay 1000ms plr 0.005 bw 10Mbits/s
 * ipfw pipe 200 config delay 1000ms plr 0.005 bw 10Mbits/s
 * 
 * 
 */

/**
 * Sender3.java
 * Packet design: |Seq No.|    DATA DATA DATA DATA DATA    |  EOF  |
 * Packet size:   |2 bytes|          1021 bytes            |1 byte |
 * 0 means EOF. That is last packet.
 * 
 * Since Sender3 is essentially an implementation of the FSM from the book,
 * the seqNo here starts from one, unlike in the previous steps
 * 
 * Debug level: 
 * 0 for completely silent. 
 * 1 for start/end messages only.
 * 2 for verbose mode. Fair warning: this slows everything down.
 */

import java.io.*;
import java.net.*;
import java.util.TreeMap;
class Sender3 {
	private static int DEBUG = 1;
	private static int timeout = 50;

	public static void main(String args[]) throws Exception {
		 TreeMap <Short,byte[]> packets = new TreeMap<Short,byte[]>();

		if(args.length != 4) {
			System.out.println("Usage: Sender3 localhost <Port> <Filename> [WindowSize]");
			System.exit(0);
		}
		else {
			long time1,time2 = 0;
			boolean waiting = true;
			if(DEBUG > 0) System.out.println("Sending in progress!");
			short seqNo=0;
			short base, nextseqnum;
			int ackPort;
			int port = Integer.parseInt(args[1]);
			int window = Short.parseShort(args[3]);
			if(port==1) ackPort = port++; 
			else ackPort = port--;
	        FileInputStream fis = new FileInputStream(args[2]); 
			InetAddress IPAddress = InetAddress.getByName(args[0]);
			DatagramSocket clientSocket = new DatagramSocket();
			DatagramSocket serverSocket = new DatagramSocket(ackPort);
			serverSocket.setSoTimeout(timeout);
			clientSocket.setSoTimeout(800);
	        byte[] packet = new byte[1024];
 		    byte[] buf = new byte[1021];
		    byte[] ack = new byte[1024];
		      try {
		    	 for (int readNum; (readNum = fis.read(buf)) != -1;) {
		    		// get sequence number for packet 
			    	seqNo++;
		    		packet[0] = ByteUtils.convertToBytes(seqNo)[0];
		    		packet[1] = ByteUtils.convertToBytes(seqNo)[1];
		    		for(int j=0;j<readNum;j++) {
		    			packet[j+2] = buf[j];
		    		}
		    		if(readNum == 1021) {
						packet[1023] = 1; 
		    		}
		    		else {
		    			packet[1023] = 0;
		    		}
		    		packets.put(seqNo,packet);
					// flush the thing
					buf = new byte[1021];
					packet = new byte[1024];
		            }
		        } catch (IOException ex) { }
		        time1 = System.currentTimeMillis();
		        base = 1;
		        nextseqnum = 1;
		        while(waiting) {
		        	// First, fill the window
		        	while(nextseqnum<base+window) {
		        		if(packets.get(nextseqnum) != null) {
						DatagramPacket sendPacket = new DatagramPacket(packets.get(nextseqnum), packets.get(nextseqnum).length,IPAddress,port);
						clientSocket.send(sendPacket); 
						}
		        		else {
		        			// we have reached the end of file, adjust window accordingly
		        			window = nextseqnum-base - 1;
		        		}
						if(base == nextseqnum) {
							// This sets a timeout and resets the internal SO_TIMEOUT value
							serverSocket.setSoTimeout(timeout);
						}
						if(DEBUG > 1) System.out.println("Sending: "+nextseqnum);
			        	nextseqnum++;
		        	}
		        	try {
					DatagramPacket ackPacket = new DatagramPacket(ack,ack.length);
					serverSocket.receive(ackPacket);
					byte[] ackNo = new byte[2];
					ackNo[0] = ackPacket.getData()[0];
					ackNo[1] = ackPacket.getData()[1];
					base = ByteUtils.convertShortFromBytes(ackNo);
					base++;
					if(ByteUtils.convertShortFromBytes(ackNo) == packets.lastKey()) {
	        			time2 = System.currentTimeMillis();
						break;
					}
					if(base==nextseqnum) {
						// Setting a very large timeout acts as stopping the timer
						// this also implicitly resets the SO_TIMEOUT counter
						serverSocket.setSoTimeout(5000);
					}
					else {
						// setting a new timer. this also implicitly resets the SO_TIMEOUT counter
						serverSocket.setSoTimeout(timeout); 
						}
		        	}
		        	catch(SocketTimeoutException e) {
		        		// timed out. Resend window.
		        		nextseqnum = base;
		        	}
		        }        
		        if(DEBUG > 0) System.out.println("File sent!");
		        File f = new File(args[2]);
		        if(DEBUG > 0) System.out.println("Throughput: "+(f.length()/1024/((float)(time2-time1)/(float)1000))); 
			clientSocket.close();
			serverSocket.close();
		}
	}
}
	