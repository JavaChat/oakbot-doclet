package oakbot.doclet.cli;

/**
 * Gets the program's input.
 * @author Michael Angstadt
 */
public interface Input {
	/**
	 * Reads the input parameters.
	 * @return the input parameters
	 * @throws IllegalArgumentException if the parameters are invalid in any way
	 */
	InputParameters read();
}
