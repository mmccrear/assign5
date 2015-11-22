package TCP;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
/**
 * Channel implementation for simulating bit errors and lost packets.
 * Does not support input timeouts.
 * To run:  UChannel [-m pmunge][-l ploss][-f filename]
 * When supplied with a filename goes into automatic data feed
 * 
 * @author rms
 *
 */
public class UChannel implements Channel {
	protected InputStream ins, inr;
	protected OutputStream outs, outr;
	protected double pmunge, plose;
	private PrintWriter p;
	protected BufferedReader bin;
	protected ProdCom prodcom = new ProdCom();
	/**
	 * Constructs a UChannel with bit errors and packet loss
	 * ploss	
	 * @param pmunge		bit error probability
	 * @param plose			lost packet probability
	 * @throws IOException	for channel i/o errors
	 */
	public UChannel(double pmunge, double plose) throws IOException {
		this.pmunge = pmunge;
		this.plose = plose;
		initChannels();
		p = new PrintWriter(outs);
		bin = new BufferedReader(new InputStreamReader(inr));		
	}
	
	private void initChannels() throws IOException {
		ins = new PipedInputStream(); 
		inr = new PipedInputStream();
		outs = new PipedOutputStream((PipedInputStream)ins); 
		outr = new PipedOutputStream((PipedInputStream)inr);
	}
	synchronized public void send(String s) {
		prodcom.produce(s);
	}
	
	synchronized public void send(PacketType packet) {
		prodcom.produce(packet.serialize());
	}

	public String receive() throws IOException {
		String s = bin.readLine();
		return s;
	}
	/**
	 * runs this UChannel in a new Thread
	 */
	public void start() {
		new Thread(this).start();
		new Thread(prodcom).start();
	}
	/**
	 * Reads input data provided by send; introduces bit errors;
	 * writes data to receive, or loses packet 
	 */
	@Override
	public void run() {
		PrintWriter p = new PrintWriter(outr);
		BufferedReader bin = new BufferedReader(new InputStreamReader(ins));
		String s = null;
		try {
			while ((s = bin.readLine()) != null) {
				StringBuffer buf = new StringBuffer(s);
				if (Math.random() < plose) continue;
				for (int i = 0; i < buf.length(); i++) {
					if (Math.random() < pmunge) buf.setCharAt(i, (char)(32+(int)Math.floor(94*Math.random())));
				}
				p.println(buf.toString());
				p.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public class ProdCom implements Runnable {
		String data = null;
		synchronized void produce(String data) {
			this.data = data;
			notify();
		}
		synchronized void consume() {
			for (;;) { 
				try {
					if (data == null) wait();
					p.println(data);
					p.flush();
					data = null;
				} catch (InterruptedException e) {
					continue;
				}
			}
		}
		@Override
		public void run() {
			consume();
		}
	}
		
	public static void main(String[] args) throws IOException {
		Object[] pargs;
		final StringPitcher sp;
		try {
			pargs = RTDBase.argParser("UChannel", args); 
		} catch (RuntimeException ex) {
			System.out.println(ex.getMessage());
			return;
		}
		final UChannel uchannel = new UChannel((Double)pargs[0], (Double)pargs[1]);
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
		new Thread(new Runnable(){
			public void run() {
				for (;;) {
					try {
						String s = uchannel.receive();
						System.out.println("         "+s);
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
