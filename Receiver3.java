/* Ivan Trendafilov 0837795 */
/** Receiver3 
 * 
 * Debug level: 
 * 0 for completely silent. 
 * 1 for start/end messages only.
 * 2 for verbose mode. Fair warning: this slows everything down.
*/

import java.io.*;
import java.net.*;
import java.util.ArrayList;

class Receiver3 {

public static int DEBUG = 1;
public static void main(String args[]) throws Exception  {
		 ArrayList <byte[]> packetList = new ArrayList<byte[]>();
	     if(args.length != 2) {
	    	 System.out.println("Usage: Receiver4 <Port> <Filename>");
	    	 System.exit(0);
	     }
	     int port = Integer.parseInt(args[0]);
	     int ackPort;
	     short expectedseqnum,  SeqNo = 0;
		 if (port==1) ackPort = port++; 
		 else ackPort = port--;
         DatagramSocket serverSocket = new DatagramSocket(port);
         DatagramSocket clientSocket = new DatagramSocket();
 		 ByteArrayOutputStream bos = new ByteArrayOutputStream();
		 InetAddress IPAddress = InetAddress.getByName("localhost");
		 // a very large timeout. This gets changed later on
         serverSocket.setSoTimeout(1000000);
         System.out.println("Waiting for data...");
         byte[] receiveData = new byte[1024];
         boolean waiting=true;
         byte[] buf = new byte[1024];
         byte[] ACK = new byte[2+"ACK".getBytes().length];
         // make me the default packet
         expectedseqnum=1;
 		 ACK[0] = ByteUtils.convertToBytes(0)[0];
		 ACK[1] = ByteUtils.convertToBytes(0)[1];
		 for(int j=0;j<"ACK".getBytes().length;j++) {
			 ACK[j+2] = "ACK".getBytes()[j];
		 }
         while(waiting) {
        	try {
     		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    		serverSocket.receive(receivePacket);
    		buf = receivePacket.getData();
    		byte[] seq = new byte[2];
    		seq[0] = buf[0];
    		seq[1] = buf[1];
    		SeqNo = ByteUtils.convertShortFromBytes(seq);
    		if(DEBUG > 1) System.out.println(SeqNo);
    		if(SeqNo != 0) serverSocket.setSoTimeout(3000);
    		}
        	catch(SocketTimeoutException e) {
        		waiting=false;
        	}
        	// if received seq.no. is the expected seq.no.
    		if(SeqNo == expectedseqnum) {
    			packetList.add(ByteUtils.subbytes(buf, 2, 1023));
	    		ACK[0] = ByteUtils.convertToBytes(expectedseqnum)[0];
	    		ACK[1] = ByteUtils.convertToBytes(expectedseqnum)[1];
	    		for(int j=0;j<"ACK".getBytes().length;j++) {
	    			ACK[j+2] = "ACK".getBytes()[j];
	    		}	
      			DatagramPacket ackPacket = new DatagramPacket(ACK, ACK.length,IPAddress,ackPort);
      			clientSocket.send(ackPacket);
      			expectedseqnum++;
    		}
    		// if not, retransmit last ACK
    		else {
      			DatagramPacket ackPacket = new DatagramPacket(ACK, ACK.length,IPAddress,ackPort);
      			clientSocket.send(ackPacket);
    		}
         }
         
         // reconstruct file
         for(short z=0;z<packetList.size();z++) {
     		byte parse[] = packetList.get(z);
     		if(parse!=null) bos.write(parse,0,parse.length);
     		}
         	File filename = new File(args[1]);
            FileOutputStream zz = new FileOutputStream(filename);
            byte[] entireFileWithJunk = bos.toByteArray();
            int lastpos = entireFileWithJunk.length;
            // find what's the last zero
            for(int i=entireFileWithJunk.length-1;i>=0;i--) {
            	if(entireFileWithJunk[i] != 0) {
            		lastpos = i+1;
            		break;
            	}
            }
            // subbyte the string and write it to file
            zz.write(ByteUtils.subbytes(entireFileWithJunk, 0, lastpos));
            zz.close();
            serverSocket.close();
            System.out.println("File sent");
      }
}