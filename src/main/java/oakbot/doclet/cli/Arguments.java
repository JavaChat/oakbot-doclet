package oakbot.doclet.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Parses the program's command-line arguments.
 * @author Michael Angstadt
 */
public class Arguments {
	private final OptionSet options;

	public Arguments(String[] args) {
		OptionParser parser = new OptionParser();
		parser.accepts("verbose");
		parser.accepts("v");
		parser.accepts("i");
		parser.accepts("help");
		parser.accepts("h");

		parser.accepts("maven").withRequiredArg();

		parser.accepts("src").withRequiredArg();
		parser.accepts("name").withRequiredArg();
		parser.accepts("ver").withRequiredArg(); //don't use "version", as that refers to the program's version

		parser.accepts("excludePackages").withRequiredArg();
		parser.accepts("javadocUrl").withRequiredArg();
		parser.accepts("javadocUrlPattern").withRequiredArg();
		parser.accepts("website").withRequiredArg();

		parser.accepts("prettyPrint");
		parser.accepts("output").withRequiredArg();

		options = parser.parse(args);
	}

	public boolean verbose() {
		return bool(false, "verbose", "v");
	}

	public boolean interactive() {
		return bool(false, "i");
	}

	public boolean help() {
		return bool(false, "help", "h");
	}

	public MavenLibrary maven() {
		String value = value("maven");
		return value.isEmpty() ? null : MavenLibrary.parse(value);
	}

	public Path src() {
		return path("src");
	}

	public String name() {
		return value("name");
	}

	public String ver() {
		return value("ver");
	}

	public String excludePackages() {
		return value("excludePackages");
	}

	public String javadocUrl() {
		return value("javadocUrl");
	}

	public String javadocUrlPattern() {
		return value("javadocUrlPattern");
	}

	public String website() {
		return value("website");
	}

	public boolean prettyPrint() {
		return bool(false, "prettyPrint");
	}

	public Path output() {
		return path("output");
	}

	public void printHelp() {
		String helpText;

		/*
		 * Getting the file contents using a Path doesn't work, throws
		 * FileSystemNotFoundException when run from the packaged JAR:
		 * 
		 * Path helpText =
		 * Paths.get(getClass().getResource("help.txt").toURI());
		 */
		try (InputStream in = getClass().getResourceAsStream("help.txt")) {
			helpText = readContents(in);
		} catch (IOException e) {
			//should never be thrown because file is on the classpath
			throw new RuntimeException(e);
		}

		System.out.println(helpText);
	}

	private boolean bool(boolean defaultValue, String... options) {
		for (String option : options) {
			if (this.options.has(option)) {
				return true;
			}
		}
		return defaultValue;
	}

	private Path path(String option) {
		String value = value(option);
		return value.isEmpty() ? null : Paths.get(value);
	}

	private String value(String option) {
		String value = (String) options.valueOf(option);
		return (value == null) ? "" : value.trim();
	}

	/**
	 * Reads the contents of an input stream to a string.
	 * @param in the input stream
	 * @return the string
	 * @throws IOException if there's a problem reading from the input stream
	 */
	private static String readContents(InputStream in) throws IOException {
		Reader reader = new InputStreamReader(in);
		StringBuilder sb = new StringBuilder();
		char buffer[] = new char[2048];
		int read;
		while ((read = reader.read(buffer)) != -1) {
			sb.append(buffer, 0, read);
		}
		return sb.toString();
	}
}
