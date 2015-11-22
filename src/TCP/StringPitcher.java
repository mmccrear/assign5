package TCP;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
/**
 * Reads a text file and "pitches" one line at a time to a reader.
 * Repeats from the beginning when end-of-file is reached
 * @author rms
 *
 */
public class StringPitcher implements Runnable {
	private BufferedReader in;
	private int delay, wiggle;
	private BufferedReader indat;
	private PrintWriter outdat;
	
	/**
	 * Constructs a StringPitcher for given file
	 * @param f			File to be pitched
	 * @param delay		in milleseconds; delay between pitched lines
	 * @throws IOException	if File I/O has an error
	 */
	public StringPitcher(File f, int delay) throws IOException {
		this(f, delay, 0);
	}
	/**
	 * Constructs a StringPitcher for given file
	 * @param f			File to be pitched
	 * @param delay		in milleseconds; delay between pitched lines
	 * @param wiggle	+/- random delay time
	 * @throws IOException	if File I/O has an error
	 */
	public StringPitcher(File f, int delay, int wiggle) throws IOException {
		in = new BufferedReader(new FileReader(f));
		this.delay = delay;
		this.wiggle = wiggle;
		PipedReader pr = new PipedReader();
		indat = new BufferedReader(pr);
		outdat = new PrintWriter(new PipedWriter(pr));
	}
	/**
	 * Reads and returns a line of text
	 * @return	the line read
	 * @throws IOException	if File I/O has an error
	 */
	public String receive() throws IOException {
		String dat = indat.readLine();
		return dat;
	}
	/**
	 * 
	 * @return	a BufferedReader for reading file input
	 */
	BufferedReader getReader() {return indat;}
	
	/**
	 * Creates and starts a Thread to run this StringPitcher
	 */
	public void start() {
		new Thread(this).start();
	}
	
	/**
	 * Reads lines of file text to be piped to reader of this StringPitcher 
	 */
	@Override
	public void run() {
		String line;
		String[] lines;
		try {
			StringBuffer tmp = new StringBuffer();
			while ((line = in.readLine()) != null) {
				tmp.append(line).append("\n");
				outdat.println(line);
				outdat.flush();
				Thread.sleep(delay + (int)Math.floor(Math.random()*(2 * wiggle)-wiggle));
			}
			in.close();
			lines = tmp.toString().split("\n");
			for (;;) {
				for (String line0 : lines) {
					outdat.println(line0);
					outdat.flush();
					Thread.sleep(delay);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Demo of StringPitcher functionality
	 * @param args	Name of file in current directory;
	 *  if empty default file "Dickens.txt" is read
	 * @throws IOException	if File I/O causes an error
	 */
	public static void main(String[] args) throws IOException {
		String filename = (args.length == 0) ? "Dickens.txt" : args[0];
		File f = new File(System.getenv("user.dir"), filename);
		final StringPitcher sp = new StringPitcher(f, 2000);
		new Thread(new Runnable() {
			public void run() {
				String dat;
				try {
					while ((dat = sp.receive()) != null)
						System.out.println(dat);				
				} catch (IOException e) {
					e.printStackTrace();
				} 
			}
		}).start();
		new Thread(sp).start();
	}
}
