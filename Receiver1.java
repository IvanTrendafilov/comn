/* Ivan Trendafilov 0837795 */
/** Receiver1 
 *  
*/
import java.io.*;
import java.net.*;

class Receiver1 {

public static void main(String args[]) throws Exception  {
	     if(args.length != 2) {
	    	 System.out.println("Usage: Receiver1 <Port> <Filename>");
	    	 System.exit(0);
	     }
	     int port = Integer.parseInt(args[0]);
         DatagramSocket serverSocket = new DatagramSocket(port);
 		 ByteArrayOutputStream bos = new ByteArrayOutputStream();
         serverSocket.setSoTimeout(0);
         byte[] receiveData = new byte[1024];
         boolean waiting=true;
         System.out.println("Waiting for data...");
         byte[] buf = new byte[(64*1024)-40];
         while(waiting)
            {
        	 	// receive packet
            		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try {
            		serverSocket.receive(receivePacket);
            		buf = receivePacket.getData(); }
			catch(Exception e1) {
			System.out.println("Timeout");
			waiting=false;
			}
            		// write to byte array outputstream
            		if(buf[1023]!=0) {
				serverSocket.setSoTimeout(100);
            			byte tmp[] = new byte[buf.length-3];
            			for(int i=0;i<tmp.length;i++) tmp[i]=buf[i+2];
            			bos.write(tmp, 0, tmp.length); 
            		}
            		else {
            			// clean up the last received packet
            			int size=1023;
            			for(int i=1023;i>=0;i--) {
            				if(buf[i] != 0) {
            					size = i+1;
            					break;
            				}
            			}
            			// copy array into array
            			byte tmp[] = new byte[size-2];
            			for(int i=0;i<tmp.length;i++) tmp[i]=buf[i+2];
            			bos.write(tmp,0,tmp.length);
            			waiting = false;
            		}
            		buf = new byte[1024];
               }
            FileOutputStream zz = new FileOutputStream(args[1]);
            bos.writeTo(zz);
            zz.close();
            serverSocket.close();
            System.out.println("File received!");
      }
}
