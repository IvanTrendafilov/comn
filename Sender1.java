/* Ivan Trendafilov 0837795 */
/**
 * Sender1.java
 * Packet design: |Seq No.|    DATA DATA DATA DATA DATA    |  EOF  |
 * Packet size:   |2 bytes|          1021 bytes            |1 byte |
 */

import java.io.*;
import java.net.*;
class Sender1 {
	public static void main(String args[]) throws Exception {

		if(args.length != 3) {
			System.out.println("Usage: Sender1 localhost <Port> <Filename>");
			System.exit(0);
		}
		else {
			short seqNo=-1;
			int port =  Integer.parseInt(args[1]);
	        FileInputStream fis = new FileInputStream(args[2]); 
			InetAddress IPAddress = InetAddress.getByName(args[0]);
			DatagramSocket clientSocket = new DatagramSocket();
	        byte[] packet = new byte[1024];
 		    byte[] buf = new byte[1021];
		      try {
		    	 for (int readNum; (readNum = fis.read(buf)) != -1;) {
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
					DatagramPacket sendPacket = new DatagramPacket(packet, packet.length,IPAddress,port);
					clientSocket.send(sendPacket); 
					// TODO: uncomment this for basic flow control. i.e., if you want to receive a correct image
					// Thread.sleep(10);
					// flush this
					buf = new byte[1021];
					packet = new byte[1024];
		            }
		        } catch (IOException ex) { }
		        System.out.println("File sent!");
			clientSocket.close();
		}
	}
}
	
