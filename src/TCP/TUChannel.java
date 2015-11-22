package TCP;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Implements an extension of UChannel with receiver timeout 
 * @author rms
 *
 */
public class TUChannel extends UChannel {
	private static int PORTNO = 8025;
	private Socket insock, outsock;
	private ServerSocket startSock;
	private BufferedReader in;
	private boolean timedOut = false, timerOn = false;

	public TUChannel(double pmunge, double plose) throws IOException {
		super(pmunge, plose);
		startSock = new ServerSocket(PORTNO);
		startSock.setReuseAddress(true);
		new Thread(new Runnable(){
			public void run() {
				try {
					outsock = startSock.accept();
					OutputStream outr = outsock.getOutputStream();
					startSock.close();
					PrintWriter sout = new PrintWriter(new OutputStreamWriter(outr)); 
					for (;;) {
						String line;
						while ((line = bin.readLine()) != null) {
							sout.println(line);
							sout.flush();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
		insock = new Socket("localhost", PORTNO);
		insock.setSoTimeout(100);
		in = new BufferedReader(new InputStreamReader(insock.getInputStream()));
	}
	/**
	 * If timer is running receive throws a TimedoutExpception at a fixed interval  
	 */
	public String receive() throws IOException {
		for (;;) {
			try{
				if (!isTimerOn()) return null;
				if (isTimedOut()) {
					setTimerOn(false);
					throw new TimedOutException();
				}
				String line = in.readLine();
				return line;
			} catch (SocketTimeoutException ex) {
				continue;
			}
		}
	}
	/**
	 * Turns off and disables timer
	 */
	public void reset() {
		setTimedOut(false); 
		setTimerOn(false);
	}
	/**
	 * Turns off timer
	 */
	public void stopTimer() {setTimerOn(false);}
	/**
	 * Starts timer; after delay milliseconds the Thread waiting on receive will 
	 * catch a TimedoutException
	 * @param delay	Number of milliseconds between TimedoutExceptions
	 */
	public void startTimer(final int delay) {
		setTimerOn(true);
		setTimedOut(false);
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {}
				setTimedOut(true);
			}
		}).start();
	}

	private synchronized boolean isTimedOut() {return timedOut;}
	private synchronized void setTimedOut(boolean timedOut) {this.timedOut = timedOut;}

	private synchronized boolean isTimerOn() {return timerOn;}
	private synchronized void setTimerOn(boolean timerOn) {this.timerOn = timerOn;}

	public class TimedOutException extends RuntimeException {}

	public static void main(String[] args) throws IOException {
		Object[] pargs;
		final StringPitcher sp;
		try {
			pargs = RTDBase.argParser("TUChannel", args); 
		} catch (RuntimeException ex) {
			System.out.println(ex.getMessage());
			return;
		}
		final TUChannel uchannel = new TUChannel((Double)pargs[0], (Double)pargs[1]);
		final BufferedReader inn;
		if (pargs[3] == null) {
			sp = null;
			inn = new BufferedReader(new InputStreamReader(System.in));
		}
		else {
			sp = new StringPitcher(new File(System.getenv("user.dir"), (String)pargs[3]), 2000, 1000);
			inn = sp.getReader();
			sp.start();
		}		 	
		uchannel.start();
		uchannel.startTimer(3000);
		new Thread(new Runnable(){
			public void run() {
				for (;;) {
					try {
						String s = uchannel.receive();
						System.out.println("         "+s);
					} catch (TimedOutException ex) {
						System.out.println("         ***Timeout***");
						uchannel.startTimer(3000);						
						continue;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
		new Thread(new Runnable(){
			public void run() {
				try {
					for (;;) {
						String s = inn.readLine();
						if (sp != null) System.out.println(s);
						uchannel.send(s);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}	

