package TCP;
/**
 * Static class implementing CheckSum methods
 * 
 *
 */
public class CkSum {
	/**
	 * Generates a 4-hex-digit checksum
	 * @param s		Input String requiring a checksum
	 * @return		4-hex-digit String representing the checksum of s
	 */
	public static String genCheck(String s) {
		String hex_value = new String();
		// 'hex_value' will be used to store various hex values as a string
		int x, i, checksum = 0;
		// 'x' will be used for general purpose storage of integer values
		// 'i' is used for loops
		// 'checksum' will store the final checksum
		for (i = 0; i < s.length() - 2; i = i + 2) {
			x = (int) (s.charAt(i));
			hex_value = Integer.toHexString(x);
			x = (int) (s.charAt(i + 1));
			hex_value = hex_value + Integer.toHexString(x);
			// Extract two characters and get their hexadecimal ASCII values
			x = Integer.parseInt(hex_value, 16);
			// Convert the hex_value into int and store it
			checksum += x;
			// Add 'x' into 'checksum'
		}
		if (s.length() % 2 == 0) {
			// If number of characters is even, then repeat above loop's steps
			// one more time.
			x = (int) (s.charAt(i));
			hex_value = Integer.toHexString(x);
			x = (int) (s.charAt(i + 1));
			hex_value = hex_value + Integer.toHexString(x);
			x = Integer.parseInt(hex_value, 16);
		} else {
			// If number of characters is odd, last 2 digits will be 00.
			x = (int) (s.charAt(i));
			hex_value = "00" + Integer.toHexString(x);
			x = Integer.parseInt(hex_value, 16);
		}
		checksum += x;
		// Add the generated value of 'x' from the if-else case into 'checksum'
		hex_value = Integer.toHexString(checksum);
		// Convert into hexadecimal string
		if (hex_value.length() > 4) {
			// If a carry is generated, then we wrap the carry
			int carry = Integer.parseInt(("" + hex_value.charAt(0)), 16);
			// Get the value of the carry bit
			hex_value = hex_value.substring(1, 5);
			// Remove it from the string
			checksum = Integer.parseInt(hex_value, 16);
			// Convert it into an int
			checksum += carry;
			// Add it to the checksum
		}
		checksum = generateComplement(checksum);
		// Get the complement
		String ans = Integer.toHexString(checksum);
		while (ans.length() < 4) ans = "0"+ans;
		return ans;
	}
	/**
	 * Determines if input checksum is correct for given String
	 * @param s				String to be tested
	 * @param checksum_s	4-hex-digit checksum as a String
	 * @return				true if checksum_s is correct checksum for s; false otherwise
	 */
	public static boolean checkString(String s, String checksum_s) {
		try {
			int checksum = Integer.parseInt(checksum_s, 16);
			int generated_checksum = Integer.parseInt(genCheck(s), 16);
			// Calculate checksum of received data
			generated_checksum = generateComplement(generated_checksum);
			// 	Then get its complement, since generated checksum is complemented
			int syndrome = generated_checksum + checksum;
			// Syndrome is addition of the 2 checksums
			syndrome = generateComplement(syndrome);
			// It is complemented
			return (syndrome == 0);
		} catch (NumberFormatException e) {
			return false;
		}
	}
	/**
	 * Generates 1's complement of a hexadecimal value
	 * @param checksum	32-bit integer
	 * @return			1's complement of checksum
	 */

	public static int generateComplement(int checksum) {
		checksum = Integer.parseInt("FFFF", 16) - checksum;
		return checksum;
	}
}