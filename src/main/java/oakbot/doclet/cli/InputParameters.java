package oakbot.doclet.cli;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * The program input.
 * @author Michael Angstadt
 */
public class InputParameters {
	private final String name, version, javadocUrl, javadocUrlPattern, website;
	private final List<String> excludePackages;
	private final boolean prettyPrint;
	private final MavenLibrary mavenLibrary;
	private final Path source, output;

	private InputParameters(Builder builder) {
		name = builder.name;
		version = builder.version;
		javadocUrl = builder.javadocUrl;
		javadocUrlPattern = builder.javadocUrlPattern;
		website = builder.website;
		excludePackages = builder.excludePackages;
		prettyPrint = builder.prettyPrint;
		mavenLibrary = builder.mavenLibrary;
		source = builder.source;
		output = builder.output;
	}

	/**
	 * Gets the name of the library.
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the version of the library.
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Gets the URL to the library's online Javadocs.
	 * @return the Javadoc URL or empty string if not specified
	 */
	public String getJavadocUrl() {
		return javadocUrl;
	}

	/**
	 * The pattern for the library's online Javadocs.
	 * @return the Javadoc URL pattern or empty string if not specified
	 */
	public String getJavadocUrlPattern() {
		return javadocUrlPattern;
	}

	/**
	 * Gets the URL of the library's website.
	 * @return the website URL or empty string if not defined
	 */
	public String getWebsite() {
		return website;
	}

	/**
	 * Gets the names of the top-level packages to exclude from the generated
	 * ZIP file.
	 * @return the packages to exclude
	 */
	public List<String> getExcludePackages() {
		return excludePackages;
	}

	/**
	 * Determines whether the XML output should be formatted for human
	 * readability.
	 * @return true to pretty print, false not to
	 */
	public boolean isPrettyPrint() {
		return prettyPrint;
	}

	/**
	 * Gets the Maven information on the library.
	 * @return the Maven information or null if source code was specified
	 * instead
	 */
	public MavenLibrary getMavenLibrary() {
		return mavenLibrary;
	}

	/**
	 * Gets the path to the library's source code
	 * @return the path to the source code or null if Maven coordinates were
	 * provided
	 */
	public Path getSource() {
		return source;
	}

	/**
	 * Gets the path to where the generated ZIP file should be saved.
	 * @return the output path
	 */
	public Path getOutput() {
		return output;
	}

	/**
	 * Creates instances of {@link InputParameters}.
	 * @author Michael Angstadt
	 */
	public static class Builder {
		private String name, version, javadocUrl, javadocUrlPattern, website;
		private List<String> excludePackages = Collections.emptyList();
		private boolean prettyPrint;
		private MavenLibrary mavenLibrary;
		private Path source, output;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder version(String version) {
			this.version = version;
			return this;
		}

		public Builder javadocUrl(String javadocUrl) {
			this.javadocUrl = javadocUrl;
			return this;
		}

		public Builder javadocUrlPattern(String javadocUrlPattern) {
			this.javadocUrlPattern = javadocUrlPattern;
			return this;
		}

		public Builder website(String website) {
			this.website = website;
			return this;
		}

		public Builder excludePackages(List<String> excludePackages) {
			this.excludePackages = excludePackages;
			return this;
		}

		public Builder prettyPrint(boolean prettyPrint) {
			this.prettyPrint = prettyPrint;
			return this;
		}

		public Builder mavenLibrary(MavenLibrary mavenLibrary) {
			this.mavenLibrary = mavenLibrary;
			return this;
		}

		public Builder source(Path source) {
			this.source = source;
			return this;
		}

		public Builder output(Path output) {
			this.output = output;
			return this;
		}

		public InputParameters build() {
			return new InputParameters(this);
		}
	}
}
