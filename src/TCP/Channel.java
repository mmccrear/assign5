package TCP;
import java.io.IOException;
/**
 * Interface for 1-directional channel using a pipe
 * @author rms
 *
 */
public interface Channel extends Runnable {
	/**
	 * Starts all threads in the channel
	 */
	void start();
	/**
	 * Send for a String
	 * @param s	String to be sent
	 */
	void send(String s);
	/**
	 * Send for a PacketType. Channel serializes the packet before sending it.
	 * @param packet	Packet to be sent
	 */
	void send(PacketType packet);
	/**
	 * Receiver for sent data
	 * @return	data sent by send method.
	 * @throws IOException	from internal pipe
	 */
	String receive() throws IOException;
}
