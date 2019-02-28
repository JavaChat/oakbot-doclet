package oakbot.doclet.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Mimics the {@link java.io.Console} class so that this program can be run
 * from Eclipse.
 * @author Michael Angstadt
 */
public class Console {
	private final BufferedReader reader;
	private final PrintStream out;

	public Console() {
		this(System.in, System.out);
	}

	/**
	 * For unit testing.
	 * @param in
	 * @param out
	 */
	Console(InputStream in, PrintStream out) {
		reader = new BufferedReader(new InputStreamReader(in));
		this.out = out;
	}

	/**
	 * Prints to stdout.
	 * @param message the text to print (formatted string)
	 * @param args the formatted string arguments
	 */
	public void printf(String message, Object... args) {
		out.printf(message, args);
	}

	/**
	 * Prompts the user for input.
	 * @param message the text to print
	 * @return the user's response or empty string if they didn't type anything
	 */
	public String readLine(String message) {
		out.print(message + " ");
		try {
			return reader.readLine().trim();
		} catch (IOException ignore) {
			throw new RuntimeException(ignore);
		}
	}

	/**
	 * Prompts the user for input and optionally provides them with a suggested
	 * response.
	 * @param message the text to print
	 * @param defaultValue the value to return if the user does not enter
	 * anything or null to force the user to enter something
	 * @return the user's response or the default value if the user did not
	 * enter anything
	 */
	public String readLine(String message, String defaultValue) {
		if (defaultValue != null) {
			message += " [" + defaultValue + "]";
		}

		while (true) {
			String answer = readLine(message);
			if (!answer.isEmpty()) {
				return answer;
			}

			if (defaultValue != null) {
				return defaultValue;
			}
		}
	}

	/**
	 * Prompts the user for input and provides them with a suggested response.
	 * Will not accept a response that contains whitespace.
	 * @param message the text to print
	 * @param defaultValue the value to return if the user does not enter
	 * anything or null to force the user to enter something
	 * @return the user's response or the default value if the user did not
	 * enter anything
	 */
	public String readLineNoWhitespace(String message, String defaultValue) {
		while (true) {
			String answer = readLine(message, defaultValue);
			if (answer.contains(" ") || answer.contains("\t")) {
				printf("Value cannot contain whitespace.%n");
				continue;
			}

			return answer;
		}
	}

	/**
	 * Prompts the user for a yes/no answer.
	 * @param message the text to print
	 * @param defaultToYes true to default to "yes" if the user does not provide
	 * a response, false to default to "no"
	 * @return true if the user answered "yes", false otherwise
	 */
	public boolean readLineYesNo(String message, boolean defaultToYes) {
		message = new StringBuilder(message) //@formatter:off
			.append(" [")
			.append(defaultToYes ? "Y/n" : "y/N")
			.append("]: ")
		.toString(); //@formatter:on

		String answer = readLine(message);
		return "y".equalsIgnoreCase(answer);
	}
}
