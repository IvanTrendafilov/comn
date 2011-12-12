/* Ivan Trendafilov 0837795 */
/**
 *  Sender4 - Timer thread - each packet is wrapped around a timer thread.
 * The thread doesn't actually contain the packet, unless a timeout
 * occurs, in which case it gets the timer from Sender4.
 */
public class Timer extends Thread
{
	// debug from Sender4
	public static int DEBUG = Sender4.DEBUG;
	// Rate at which timer is checked
	protected int m_rate = 1;
	// Length of timeout 
	private int m_length = Sender4.timeout;
	// Time elapsed 
	private int m_elapsed;
	// thread /packet sequence number 
	private Short threadSeqNo;
	private boolean running = true;

	public Timer (short SeqNo) {
		Thread.currentThread().setPriority(MIN_PRIORITY);
		if(DEBUG > 1) System.out.println("Sending: "+SeqNo);
		threadSeqNo = SeqNo;
		m_elapsed = 0;
	}

	/** Performs timer specific code */
	// Sleeps and checks if the packet has been received. If so, terminates.
	// If not, times out & resends
	public void run()
	{
		// Keep looping
	loop:
		while (running)
		{
			// Put the timer to sleep
			try
			{ 
				Thread.sleep(m_rate);
			}
			catch (InterruptedException ioe) 
			{
				continue;
			}
					// is the packet ACK-ed?
					if(ACKThread.ackContains(threadSeqNo)) {
					running = false;
					break loop;
				}
				synchronized (this) {
				// Increment time remaining
					m_elapsed += m_rate;

					// Check to see if the time has been exceeded
					if (m_elapsed > m_length)
					{
					// Trigger a timeout and reset timer
						synchronized (this) {
					timeout();
					m_elapsed = 0;
						}
					}
				}
			}
		}

	public void timeout() {
			synchronized (this) {
				try {
					// re-send packet
					Sender4.clientSocket.send(Sender4.packets.get(threadSeqNo));
				} catch (Exception e) {
					// in case by the time this reaches the timeout, the ACK is received and the clientSocket is closed.
					System.exit(0);
				}
				if(DEBUG > 1) System.out.println("Resend "+threadSeqNo);
			}
	}
}