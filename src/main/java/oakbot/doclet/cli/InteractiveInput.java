package oakbot.doclet.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Gets the program's input from an interactive command prompt.
 * @author Michael Angstadt
 */
public class InteractiveInput implements Input {
	private final Console console;

	public InteractiveInput(Console console) {
		this.console = console;
	}

	@Override
	public InputParameters read() {
		console.printf("Welcome to the OakBot Javadoc Generator.%n");

		MavenLibrary library = null;
		Path source = null;
		while (true) {
			String answer = console.readLine("Enter Maven ID or path to source ZIP/JAR/folder:");
			if (answer.isEmpty()) {
				continue;
			}

			try {
				library = MavenLibrary.parse(answer);
			} catch (IllegalArgumentException e) {
				source = Paths.get(answer);
				if (!Files.exists(source)) {
					console.printf("File or folder does not exist: " + source + "%n");
					source = null;
					continue;
				}
			}

			break;
		}

		String name = console.readLineNoWhitespace("Library name:", (library == null) ? null : library.getArtifactId());
		String version = console.readLineNoWhitespace("Library version:", (library == null) ? null : library.getVersion());
		String javadocUrl = console.readLine("Library's base javadoc URL (optional):");
		String javadocUrlPattern = console.readLine("Library's javadoc URL pattern (optional):");
		String website = console.readLine("Library's website (optional):");
		boolean prettyPrint = console.readLineYesNo("Pretty-print the XML?", false);

		String answer = console.readLine("Enter a comma separated list of packages you want to exclude (optional):");
		List<String> excludePackages = answer.isEmpty() ? Collections.emptyList() : Arrays.asList(answer.split("\\s*,\\s*"));

		answer = console.readLine("Save ZIP file as:", name + "-" + version + ".zip");
		Path output = Paths.get(answer);

		InputParameters params = new InputParameters.Builder() //@formatter:off
			.source(source)
			.mavenLibrary(library)
			.name(name)
			.version(version)
			.javadocUrl(javadocUrl)
			.javadocUrlPattern(javadocUrlPattern)
			.website(website)
			.excludePackages(excludePackages)
			.prettyPrint(prettyPrint)
			.output(output)
		.build(); //@formatter:on

		if (!confirmSettings(params)) {
			System.exit(0);
		}

		return params;
	}

	private boolean confirmSettings(InputParameters params) {
		console.printf("=============Confirmation=============%n");
		if (params.getMavenLibrary() != null) {
			console.printf("Maven ID: %s%n", params.getMavenLibrary());
		}
		if (params.getSource() != null) {
			console.printf("Source: %s%n", params.getSource());
		}
		console.printf("Library Name: %s%n", params.getName());
		console.printf("Version: %s%n", params.getVersion());
		console.printf("Base javadoc URL: %s%n", params.getJavadocUrl());
		console.printf("Javadoc URL pattern: %s%n", params.getJavadocUrlPattern());
		console.printf("Website: %s%n", params.getWebsite());
		console.printf("Pretty print XML: %s%n", params.isPrettyPrint());
		console.printf("Exclude packages: %s%n", params.getExcludePackages());
		console.printf("Save ZIP file to: %s%n", params.getOutput());

		return console.readLineYesNo("Proceed?", true);
	}
}
