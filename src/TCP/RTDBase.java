package TCP;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The base class for each of the RTD simulations
 * @author 	rms
 * @version 1.0
 * @since	2015-10-30
 */

public abstract class RTDBase implements Runnable {
	/**
	 * Communication channels used by the simulation
	 */
	protected Channel forward, backward;
	/**
	 * Instance of RSender used by the simulation
	 */
	protected RSender sender;
	/**
	 * Instance of RReceiver used by the simulation
	 */
	protected RReceiver receiver;
	/**
	 * StringPitcher, if automatic data feed is elected.
	 */
	protected StringPitcher sp = null;		

	/**
	 * Constructs the RTDBase class with forward and backward channels
	 * @param pmunge		Probability (value in [0 to 1]) that a character is altered during transmission 
	 * @param plost			Probability that a message is lost
	 * @param filename		File in current user directory used for automatic data feed
	 * @throws IOException	if file open fails
	 */
	protected RTDBase(double pmunge, double plost, String filename) throws IOException {
		this(pmunge, plost, filename, 500);
	}

	/**
	 * Constructs the RTDBase class with forward and backward channels
	 * @param pmunge		Probability (value in [0 to 1]) that a character is altered during transmission 
	 * @param plost			Probability that a message is lost
	 * @param filename		File in current user directory used for automatic data feed
	 * @param interval		Interval, in milleseconds between data feeds if automatic data feed is used
	 * @throws IOException	if file open fails
	 */
	protected RTDBase(double pmunge, double plost, String filename, int interval) throws IOException {
		if (filename != null) sp = new StringPitcher(new File(System.getenv("user.dir"), filename), interval, 0);
		this.forward = new UChannel(pmunge, plost);
		this.backward = new UChannel(pmunge, plost);
	}

	/**
	 * Base class for all RSender Classes
	 * @author rms
	 *
	 */
	protected abstract class RSender extends FSM {
		/**
		 * Reader instance for data input
		 */
		protected BufferedReader appIn;
		/*
		 * Creates an RSender instance with input from file or Standard Input 
		 */
		protected RSender() {
			appIn = (sp != null) ? sp.getReader() : new BufferedReader(new InputStreamReader(System.in));
		}
		/**
		 * Reads and returns a line from appIn  
		 * @param delay			Time to sleep between reading and returning data
		 * @return				Line read
		 * @throws IOException	if read fails
		 */
		protected String getFromApp(int delay) throws IOException {
			String dat = appIn.readLine();
			if (sp != null) {
				System.out.println(dat);
				if (delay > 0) {
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			return dat;
		}
		@Override
		public abstract int loop(int myState) throws IOException;
	}
	/**
	 * Base class for all RReceiver Classes
	 * @author rms
	 *
	 */
	protected abstract class RReceiver extends FSM {
		protected void deliverToApp(String dat) {
			System.out.println("-->        "+dat);
		}
		@Override
		public abstract int loop(int myState) throws IOException;
	}
	/**
	 * Starts threads in forward, backward, sender, receiver and sp.
	 */
	@Override
	public void run() {
		if (forward != null) forward.start();
		if (backward != null) backward.start();
		if (sender != null) new Thread(sender).start();
		if (receiver != null) new Thread(receiver).start();
		if (sp != null) new Thread(sp).start();
	}
	/**
	 * Universal argument parser for all simulators
	 * 
	 * @param prog	Simulation program name
	 * @param args	Arguments passed to main
	 * @return		Array containing -m, -l, -t and -f argument values
	 */
	public static Object[] argParser(String prog, String args[]) {
		Object[] ans = new Object[4];
		ans[0] = ans[1] = 0.0;
		ans[2] = 0;
		ans[3] = null;
		int idx = 0;
		try {
			while (idx < args.length) {
				switch (args[idx]) {
				case "-m":
					ans[0] = Double.parseDouble(args[++idx]);
					break;
				case "-l":
					ans[1] = Double.parseDouble(args[++idx]);
					break;
				case "-t":
					ans[2] = Integer.parseInt(args[++idx]);
					break;
				case "-f":
					ans[3] = args[++idx];
					break;
				}
				idx++;
			}
		} catch (Exception ex) {
			throw new RuntimeException(String.format("Usage: java %s [-m pmunge][-l ploss][-t timeout][-f file]", prog));
		}
		
		return ans;
	}
	

}
