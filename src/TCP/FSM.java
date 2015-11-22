package TCP;
import java.io.IOException;
/**
 * Implements a finite state machine by repeatedly calling loop with the current state.
 * Next state is the value returned by loop
 * @author rms
 *
 */
public abstract class FSM implements Runnable {
	/**
	 *	Current state 
	 */
	private int myState = 0;
	/**
	 * Repeated called by run with current state
	 * @param myState	current state
	 * @return			next state
	 * @throws IOException	from i/o actions
	 */
	public abstract int loop(int myState) throws IOException;
	/**
	 * Used by thread to repeatedly call loop
	 */
	public void run() {
		try {
			for (;;) myState = loop(myState);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
