/* Ivan Trendafilov 0837795 */
/** Receiver4 
 * 
 * Debug level: 
 * 0 for completely silent. 
 * 1 for start/end messages only.
 * 2 for verbose mode. Fair warning: this slows everything down.
*/
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

class Receiver4 {
public volatile static boolean waiting=true;
public static int DEBUG = 2;
public static void main(String args[]) throws Exception  {
		 TreeMap <Short,byte[]> packetList = new TreeMap<Short,byte[]>();
		 ArrayList <DatagramPacket> ackList = new ArrayList<DatagramPacket>();
		 SortedSet<Short> trackBase=new TreeSet<Short>();
	     if(args.length != 3) {
	    	 System.out.println("Usage: Receiver4 <Port> <Filename> [WindowSize]");
	    	 System.exit(0);
	     }
	     int ackPort, window = Integer.parseInt(args[2]), port = Integer.parseInt(args[0]);
	     short base, SeqNo = 0;
		 if (port==1) ackPort = port++; 
		 else ackPort = port--;	
         DatagramSocket serverSocket = new DatagramSocket(port);
         serverSocket.setReuseAddress(true);
         DatagramSocket clientSocket = new DatagramSocket();
 		 ByteArrayOutputStream bos = new ByteArrayOutputStream();
		 InetAddress IPAddress = InetAddress.getByName("localhost");
		 // a very large timeout. This gets changed later on
         serverSocket.setSoTimeout(1000000);
         if(DEBUG > 0) System.out.println("Waiting for data...");
         byte[] receiveData = new byte[1024];
         boolean waiting=true;
         byte[] buf = new byte[1024];
         byte[] ACK = new byte[5];
         // make all possible ACK packets in advance to boost speed (ArrayList get has O(1) time complexity)
         for(short j=0;j<Short.MAX_VALUE;j++) {
        	 ACK[0] = ByteUtils.convertToBytes(j)[0];
        	 ACK[1] = ByteUtils.convertToBytes(j)[1];
		 	for(int k=0;k<3;k++) {
		 		ACK[k+2] = "ACK".getBytes()[k];
		 	}
		 	DatagramPacket ackPacket = new DatagramPacket(ACK, ACK.length, IPAddress, ackPort);
		 	ackList.add(ackPacket);
		 	trackBase.add((short)j);
		 	ACK = new byte[5];
         }
         trackBase.remove((short)0);
		 base = 1;
		 while (waiting) {
			 try {
			 DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
			 serverSocket.receive(receivePacket);
			 buf = receivePacket.getData();
			 }
			 catch(SocketTimeoutException e) {
				 if(DEBUG > 0) System.out.println("File received!");
				 waiting = false;
				 continue;
			 }
			 serverSocket.setSoTimeout(1000);
			 byte[] seq = new byte[2];
			 seq[0] = buf[0];
			 seq[1] = buf[1];
			 SeqNo = ByteUtils.convertShortFromBytes(seq);
			 if(DEBUG > 1) System.out.println("Received "+SeqNo);
			 if ((SeqNo >= base) && (SeqNo <= (base+window-1))){
				 packetList.put(SeqNo, ByteUtils.subbytes(buf, 2, 1023));
				 clientSocket.send(ackList.get(SeqNo));
				 byte tmp[] = new byte[2];
				 tmp[0] = ackList.get(SeqNo).getData()[0];
				 tmp[1] = ackList.get(SeqNo).getData()[1];
				 if(DEBUG > 1) System.out.println("ACK "+ByteUtils.convertShortFromBytes(tmp));
				 trackBase.remove(SeqNo);
				 if (base == SeqNo) { 
					 base = trackBase.first(); 
				 }
				 continue;
			 }
			 if((SeqNo >= (base - window)) && (SeqNo <= base - 1)) {
				 clientSocket.send(ackList.get(SeqNo));
				 byte tmp[] = new byte[2];
				 tmp[0] = ackList.get(SeqNo).getData()[0];
				 tmp[1] = ackList.get(SeqNo).getData()[1];
				 if(DEBUG > 1) System.out.println("ACK "+ByteUtils.convertShortFromBytes(tmp));
				 continue;
			 }
		 }
		 
         // reconstructs the file
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