package oakbot.doclet.cli;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
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
		String value = value("src");
		return value.isEmpty() ? null : Paths.get(value);
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

	public void printHelp() {
		String text;
		try {
			Path helpText = Paths.get(getClass().getResource("help.txt").toURI());
			text = new String(Files.readAllBytes(helpText));
		} catch (IOException | URISyntaxException e) {
			//should never be thrown because file is on the classpath
			throw new RuntimeException(e);
		}

		System.out.println(text);
	}

	private boolean bool(boolean defaultValue, String... options) {
		for (String option : options) {
			if (this.options.has(option)) {
				return true;
			}
		}
		return defaultValue;
	}

	private String value(String option) {
		String value = (String) options.valueOf(option);
		return (value == null) ? "" : value.trim();
	}
}
