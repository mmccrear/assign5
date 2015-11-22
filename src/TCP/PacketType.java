package TCP;
/**
 * Packet interface
 * @author rms
 *
 */
public interface PacketType {
	/**
	 * Serializes content for transmission over a channel
	 * @return	serialized version of packet content.
	 */
	String serialize();
	/**
	 * @return	true if checksum does not match content.
	 */
	boolean isCorrupt();
}
