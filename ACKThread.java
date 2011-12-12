/* Ivan Trendafilov 0837795 */
/* Sender4 - ACKThread
 * Receives an ACK, parses it, and performs required synchronisation actions with the Timer threads.
 * If an ACK is within the window, add its seq. no. to the list of received ACKs to cleanly 
 * terminate the timer thread with that seq. no. Then, remove it from the sorted set of unACKed 
 * packets. 
 * If the seq. no. of the ACK is equal to the base of the window
 * move the base to the first unACKed number.
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;

public class ACKThread extends Thread {
private static int DEBUG = Sender4.DEBUG;
private short m_lastSeqNo;
private DatagramSocket serverSocket;
private boolean running = true;
public static volatile ArrayList<Short> receivedACKs = new ArrayList<Short>();

	public void run() {
    	byte[] ack = new byte[5];
    	while(running) {
			DatagramPacket ackPacket = new DatagramPacket(ack,ack.length);
				try {
					serverSocket.receive(ackPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
			byte[] ackNo = new byte[2];
			ackNo[0] = ack[0]; 
			ackNo[1] = ack[1];
			short currAckNo = ByteUtils.convertShortFromBytes(ackNo);
			short b = Sender4.getBase();
			// if this ACK is within the window
			if((b <= currAckNo) && (currAckNo <= (b+Sender4.window-1))) {
				// add to the list of received ACKs
				receivedACKs.add(currAckNo);
				if(DEBUG > 1) System.out.println("ACK: "+currAckNo);
				// remove from SortedSet of unACKed sequence numbers
				Sender4.remSeq(currAckNo);
				// if this ACK is equal to the base of the window
				if(currAckNo == b && !Sender4.isSeqEmpty()) {
					// set the window base to the first unACKed sequence number
					Sender4.setBase(Sender4.seqFirst());
				}
				// is this going to be the last ACK?
				if(currAckNo == m_lastSeqNo) {
					running=false;
					Sender4.waiting=false;
					Receiver4.waiting = false;
				}
			}
    	}
    }
	public static boolean ackContains(short c) {
		return receivedACKs.contains(c);
	}
    public ACKThread(int ackPort, short lastSeqNo, int timeout) {
    	m_lastSeqNo = lastSeqNo;
		try {
			serverSocket = new DatagramSocket(ackPort);
			serverSocket.setSoTimeout(0);
		} catch (SocketException e) {
			e.printStackTrace();
		}
    }
}
