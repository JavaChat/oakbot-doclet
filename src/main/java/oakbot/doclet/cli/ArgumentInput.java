package oakbot.doclet.cli;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Gets the program's input from the command-line arguments.
 * @author Michael Angstadt
 */
public class ArgumentInput implements Input {
	private final Arguments arguments;

	/**
	 * @param arguments the command-line arguments
	 */
	public ArgumentInput(Arguments arguments) {
		this.arguments = arguments;
	}

	@Override
	public InputParameters read() {
		Path source = arguments.src();
		if (source != null && !Files.exists(source)) {
			throw new IllegalArgumentException("Source code location does not exist: " + source);
		}

		MavenLibrary mavenLibrary = arguments.maven();
		if (source == null && mavenLibrary == null) {
			throw new IllegalArgumentException("Either a source code directory/ZIP (--src) or Maven coordinates (--maven) must be specified.");
		}

		String name = arguments.name();
		if (name.isEmpty()) {
			if (mavenLibrary == null) {
				throw new IllegalArgumentException("A library name (--name) must be specified if the --src parameter is used.");
			}
			name = mavenLibrary.getArtifactId();
		}

		String version = arguments.ver();
		if (version.isEmpty()) {
			if (mavenLibrary == null) {
				throw new IllegalArgumentException("A library version (--ver) must be specified if the --src parameter is used.");
			}
			version = mavenLibrary.getVersion();
		}

		return new InputParameters.Builder() //@formatter:off
			.source(source)
			.mavenLibrary(mavenLibrary)
			.name(name)
			.version(version)
			.javadocUrl(arguments.javadocUrl())
			.javadocUrlPattern(arguments.javadocUrlPattern())
			.website(arguments.website())
			.excludePackages(arguments.excludePackages())
			.prettyPrint(arguments.prettyPrint())
			.output(arguments.output())
		.build(); //@formatter:on
	}
}
