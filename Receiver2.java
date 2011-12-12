/* Ivan Trendafilov 0837795 */
/** Receiver2 
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

class Receiver2 {

public static void main(String args[]) throws Exception  {
		 TreeMap <Short,byte[]> packetList = new TreeMap<Short,byte[]>();
	     if(args.length != 2) {
	    	 System.out.println("Usage: Receiver2 <Port> <Filename>");
	    	 System.exit(0);
	     }
	     int port = Integer.parseInt(args[0]);
	     int ackPort;
	     // set ACK port explicitly 
		 if(port==1) ackPort = port++; 
		 else ackPort = port--;
		 // create sockets
         DatagramSocket serverSocket = new DatagramSocket(port);
         DatagramSocket clientSocket = new DatagramSocket();
 		 ByteArrayOutputStream bos = new ByteArrayOutputStream();
		 InetAddress IPAddress = InetAddress.getByName("localhost");
         serverSocket.setSoTimeout(1000000);
         byte[] receiveData = new byte[1024];
         boolean waiting=true;
         short SeqNo = -1;
         System.out.println("Waiting for data...");
         byte[] buf = new byte[1024];
         byte[] ACK = new byte[2+"ACK".getBytes().length];
         while(waiting) {
     		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
     		try {
    		serverSocket.receive(receivePacket); }
     		catch(SocketTimeoutException e) {
     			break;
     		}
    		buf = receivePacket.getData();
    		try {
    			String host = receivePacket.getSocketAddress().toString();
    			host = host.substring(1,host.indexOf(':'));
    			IPAddress = InetAddress.getByName(host); 
    		}
    		catch (UnknownHostException e) {
    			IPAddress = InetAddress.getByName("localhost");
    		}
    		byte[] seq = new byte[2];
    		seq[0] = buf[0];
    		seq[1] = buf[1];
    		SeqNo = ByteUtils.convertShortFromBytes(seq);
    		System.out.println(SeqNo);
    		// have we received this packet before?
    		if(!packetList.containsKey(SeqNo)) {
        		packetList.put(SeqNo, ByteUtils.subbytes(buf, 2, 1023));
    			if(buf[1024-1]==0) {
    				serverSocket.setSoTimeout(100);
    			}
    		}
    		buf = new byte[1024];
    		ACK[0] = ByteUtils.convertToBytes(SeqNo)[0];
    		ACK[1] = ByteUtils.convertToBytes(SeqNo)[1];
    		for(int j=0;j<"ACK".getBytes().length;j++) {
    			ACK[j+2] = "ACK".getBytes()[j];
    		}
  			DatagramPacket ackPacket = new DatagramPacket(ACK, ACK.length,IPAddress,ackPort);
  			clientSocket.send(ackPacket);
        	}

         // write received content to byte array outputstream
         for(short z=0;z<packetList.lastKey()+1;z++) {
     		byte parse[] = packetList.get(z);
     		if(parse!=null) bos.write(parse,0,parse.length);
     		}
         	File filename = new File(args[1]);
            FileOutputStream fos = new FileOutputStream(filename);
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
            fos.write(ByteUtils.subbytes(entireFileWithJunk, 0, lastpos));
            fos.close();
            serverSocket.close();
      }
}