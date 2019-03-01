package oakbot.doclet.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds a javadoc command.
 * @author Michael Angstadt
 */
public class JavadocCommandBuilder {
	private final List<String> command = new ArrayList<>();

	/**
	 * @param javadocExe the path to the javadoc executable
	 */
	public JavadocCommandBuilder(String javadocExe) {
		command.add(javadocExe);
	}

	/**
	 * Sets the fully-qualified name of the doclet class.
	 * @param className the doclet class name
	 * @return this
	 */
	public JavadocCommandBuilder doclet(String className) {
		return param("doclet", className);
	}

	/**
	 * Sets the doclet's classpath.
	 * @param classpath the classpath
	 * @return this
	 */
	public JavadocCommandBuilder docletClasspath(String classpath) {
		return param("docletpath", classpath);
	}

	/**
	 * Sets the path to the directory containing the source code to analyze.
	 * @param path the path
	 * @return this
	 */
	public JavadocCommandBuilder source(String path) {
		return param("sourcepath", path);
	}

	/**
	 * Sets the character encoding of the source code.
	 * @param charset the character set (e.g. "UTF-8")
	 * @return this
	 */
	public JavadocCommandBuilder sourceEncoding(String charset) {
		return param("encoding", charset);
	}

	/**
	 * Sets the classpath of the source code's dependencies.
	 * @param classpath the classpath
	 * @return this
	 */
	public JavadocCommandBuilder sourceDependenciesClasspath(String classpath) {
		return param("classpath", classpath);
	}

	/**
	 * Defines which top-level packages the doclet should analyze.
	 * @param subpackages the subpackages to analyze
	 * @return this
	 */
	public JavadocCommandBuilder includePackages(List<String> subpackages) {
		for (String subpackage : subpackages) {
			param("subpackages", subpackage);
		}
		return this;
	}

	/**
	 * Defines which top-level packages the doclet should ignore.
	 * @param subpackages the subpackages to ignore
	 * @return this
	 */
	public JavadocCommandBuilder excludePackages(List<String> subpackages) {
		for (String subpackage : subpackages) {
			param("exclude", subpackage);
		}
		return this;
	}

	/**
	 * Sets the system properties to pass to the doclet.
	 * @param it the system properties
	 * @return this
	 */
	public JavadocCommandBuilder systemProperties(Iterable<Map.Entry<String, String>> it) {
		for (Map.Entry<String, String> entry : it) {
			command.add("-J-D" + entry.getKey() + "=" + entry.getValue());
		}
		return this;
	}

	/**
	 * Sets the max heap size of the doclet.
	 * @param mb the max heap size in MB
	 * @return this
	 */
	public JavadocCommandBuilder maxHeapSize(int mb) {
		command.add("-J-Xmx" + mb + "m");
		return this;
	}

	/**
	 * Builds the command and its arguments (for use with
	 * {@link java.lang.ProcessBuilder}).
	 * @return the command and its arguments
	 */
	public List<String> build() {
		return command;
	}

	/**
	 * Adds a parameter to the command.
	 * @param name the parameter name
	 * @param value the parameter value
	 * @return this
	 */
	private JavadocCommandBuilder param(String name, String value) {
		command.add("-" + name);
		command.add(value);
		return this;
	}
}
