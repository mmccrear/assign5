package TCP;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.Timer;

public class SlidingWindow extends RTDBase {
	public final static int MAXSEQ = 10000, N = 5;
	private int timeout;
	public SlidingWindow(double pmunge, double ploss, int timeout, String filename) throws IOException {
		super(pmunge, ploss, filename, 1000);
		this.timeout = timeout;
		sender = new RSenderSW();
		receiver = new RReceiverSW();
	}

	public static class Packet implements PacketType{
		public final static int DIGITS = (int)Math.log10(MAXSEQ);
		public final static String fmt = String.format("%%0%dd", DIGITS);
		public String checksum;
		public String data;
		public boolean corrupt = false;
		public int seqnum;   
		
		public Packet(String data){
			this(data, 0);
		}
		public Packet(String data, int seqnum){
			this(data, seqnum, CkSum.genCheck(pack(seqnum, data)));
		}
		public Packet(String data, int seqnum, String checksum) {
			this.data = data;
			this.seqnum = seqnum;
			this.checksum = checksum;
		}
		public static Packet deserialize(String data) {
			String hex = data.substring(0, 4);
			String dat = data.substring(4+DIGITS);
			try {
				int seqnum = Integer.parseInt(data.substring(4, 4+DIGITS));
				return new Packet(dat, seqnum, hex);
			} catch (NumberFormatException ex) {
				Packet packet = new Packet(dat, 0, hex);
				packet.corrupt = true;
				return packet;
			}
		}
		@Override
		public String serialize() {
			return checksum+pack(seqnum, data);
		}
		@Override
		public boolean isCorrupt() {
			return corrupt || !CkSum.checkString(pack(seqnum, data), checksum);
		}
		@Override
		public String toString() {
			return String.format("%s "+fmt+" (%s/%s)", data, seqnum, checksum, CkSum.genCheck(pack(seqnum,data)));
		}
		private static String pack(int seqnum, String data) {
			return String.format(fmt, seqnum)+data;
		}
	}
	
	public class Window {
		int base;
		int size;
		int currentIndex = 0;
		Packet[] packets;
		
		public Window(int size) {
			this.size=size;
			base = 0;
			packets = new Packet[size];
		}
		synchronized public void add(Packet packet) {
			packets[currentIndex] = packet;
			currentIndex = (currentIndex+1)%size;
		}
		synchronized public void rebase(int newbase) {
			base = newbase;
		}
		synchronized public int getBase() {
			return base;
		}
		synchronized public Packet[] getInWindow(int top ) {
			ArrayList<Packet> orderedpackets = new ArrayList<>();
			int index = currentIndex;
			while(true){
				index = (index-1+size)%size;
				orderedpackets.add(0,packets[index]);
				if(packets[index].seqnum==top){
					break;
				}
			}
			
			Packet[] returnArray = new Packet[orderedpackets.size()];
			for(int i=0; i<returnArray.length;i++){
				returnArray[i]=orderedpackets.get(i);
			}
			
			return returnArray;
		}
	}
	
	public class RSenderSW extends RSender {
		Packet packet = null;
		Window window = new Window(N);
		int nextSeqnum = 0;
		Vector<String> hold = new Vector<String>();
		Timer timer = new Timer(timeout, new TimerAction());
		
		public RSenderSW() {
			super();
			timer.setRepeats(false);
		}
		
		public String getFromApp(int n) throws IOException {
			if (hold.size() == 0) return super.getFromApp(n);
			try{Thread.sleep(1000);}catch(InterruptedException ex){}
			String ans = hold.remove(0);
			return ans;
		}
	
		public void refuseInput(String s) {
			hold.add(s);
		}
		
		public Thread getInputThread() {
			return new Thread(new Runnable(){
				public void run() {
					try {
						for (;;) loop1();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		};
		
// 	Timeout Action
		public class TimerAction implements ActionListener {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("  **Sender: timeout; resending from " + window.getBase() + "**");
				timer.start();
				for (Packet p : window.getInWindow(nextSeqnum-1)) {
					    System.out.println("  **Resending " + p.toString());
						forward.send(p);
				}
			}
		}
		
// Input Thread
		public void loop1() throws IOException {
			String input = getFromApp(0);
			if(nextSeqnum < window.getBase()+N){
				Packet outboundPacket = new Packet(input,nextSeqnum);
				window.add(outboundPacket);
				if(window.getBase() == nextSeqnum){
					timer.start();
				}
				nextSeqnum++;
				System.out.println("Sender: Sending: " + outboundPacket.toString());
				forward.send(outboundPacket);
				
			}
			else{
				refuseInput(input);
			}
		}
		
// Acknowledgment Thread
		@Override
		public int loop(int myState) throws IOException {
			Packet backwardsPacket = Packet.deserialize(backward.receive());
			if (!backwardsPacket.isCorrupt()) {
				window.rebase(backwardsPacket.seqnum+1);
				System.out.println("  **Sender: noncorrupt ack; base = " + window.getBase() + "**");
				if (window.getBase() == nextSeqnum) {
					timer.stop();
					//window.rebase(backwardsPacket.seqnum+1);
				} else {
					timer.start();
				}
				
			}
			return myState;
		}
	}

	public class RReceiverSW extends RReceiver {
		int expectedseqnum = 0;
		@Override
		public int loop(int myState) throws IOException {
			Packet forwardsPacket = Packet.deserialize(forward.receive());
			System.out.println("\t**Receiver: " + forwardsPacket.toString() + "**");
			if (!forwardsPacket.isCorrupt() && (forwardsPacket.seqnum == expectedseqnum)) {
				//System.out.println("GOT A GOOD PACKET");
				Packet ackPacket = new Packet("ACK", expectedseqnum);
				System.out.println("\t**Receiver: ok " + expectedseqnum + " data; replying " + ackPacket.toString());
				backward.send(ackPacket);
				deliverToApp(forwardsPacket.data);
				expectedseqnum++;
				return myState;
			}
						
			Packet ackPacket = new Packet("ACK", expectedseqnum-1);
			
			// received a duplicate packet
			if (forwardsPacket.seqnum != expectedseqnum) {
				System.out.println("\t**Receiver: duplicate " + forwardsPacket.seqnum + 
									" packet; discarding; replying " +  ackPacket.toString() + "**");
			} else {
				// packet was corrupt
				System.out.println("\t**Receiver: corrupt data; replying " + ackPacket.toString() + "**");
			}
			
			backward.send(ackPacket);
			
			return myState;			
		}
	}
	@Override
	public void run() {
		super.run();
		((SlidingWindow.RSenderSW)sender).getInputThread().start();
	}

	public static void main(String[] args) throws IOException {
		Object[] pargs = argParser("SlidingWindow", args);
		SlidingWindow rdt30 = new SlidingWindow((Double)pargs[0], (Double)pargs[1], (Integer)pargs[2], (String)pargs[3]);
		rdt30.run();
	}
	
}
