/* Ivan Trendafilov 0837795 */
/**
 * Sender2.java
 * Packet design: |Seq No.|    DATA DATA DATA DATA DATA    |  EOF  |
 * Packet size:   |2 bytes|          1021 bytes            |1 byte |
 * 0 means EOF. That is last packet.
 * 
 * Experiment configuration:
 * 
 * ipfw add pipe 100 in
 * ipfw add pipe 200 out
 * ipfw pipe 100 config delay 10ms plr 0.005 bw 10Mbits/s
 * ipfw pipe 200 config delay 10ms plr 0.005 bw 10Mbits/s 
 * */

import java.io.*;
import java.net.*;
import java.util.TreeMap;
class Sender2 {
	public static void main(String args[]) throws Exception {
		 TreeMap <Short,byte[]> packets = new TreeMap<Short,byte[]>();

		if(args.length != 3) {
			System.out.println("Usage: Sender2 localhost <Port> <Filename>");
			System.exit(0);
		}
		else {
			long time1,time2 = 0;
			System.out.println("Sending in progress!");
			int sendCount=0;
			short seqNo=-1;
			int ackPort;
			int port = Integer.parseInt(args[1]);
			if(port==1) ackPort = port++; 
			else ackPort = port--;
	        FileInputStream fis = new FileInputStream(args[2]); 
			InetAddress IPAddress = InetAddress.getByName(args[0]);
			DatagramSocket clientSocket = new DatagramSocket();
			DatagramSocket serverSocket = new DatagramSocket(ackPort);
			serverSocket.setSoTimeout(50);
			clientSocket.setSoTimeout(1000);
	        byte[] packet = new byte[1024];
 		    byte[] buf = new byte[1021];
		    byte[] ack = new byte[1024];
		    int timeountCount = 0; 
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
					// flush this  thing
					buf = new byte[1021];
					packet = new byte[1024];
		            }
		        } catch (IOException ex) { }
		        time1 = System.currentTimeMillis();
		        // iterate over Treemap
		        while (!packets.isEmpty()) {
		        	short s = packets.firstKey();
		        	// send packet
					DatagramPacket sendPacket = new DatagramPacket(packets.get(s), packets.get(s).length,IPAddress,port);
					clientSocket.send(sendPacket);
					sendCount++;
					try {
						// wait for ACK
					DatagramPacket ackPacket = new DatagramPacket(ack,ack.length);
					serverSocket.receive(ackPacket);
					byte[] ackNo = new byte[2];
					ackNo[0] = ackPacket.getData()[0];
					ackNo[1] = ackPacket.getData()[1];
					packets.remove(ByteUtils.convertShortFromBytes(ackNo));
					if(packets.isEmpty()) time2 = System.currentTimeMillis();
					}
					catch(SocketTimeoutException e) { 
						timeountCount++;
						// well, we'll just resend it.
					}
		        }
		        System.out.println("File sent!");
		        System.out.println("Retransmittions: "+timeountCount);
		        File f = new File(args[2]);
		        System.out.println("Throughput: "+(f.length()/1024/((float)(time2-time1)/(float)1000))); 
			clientSocket.close();
			serverSocket.close();
		}
	}
}
	