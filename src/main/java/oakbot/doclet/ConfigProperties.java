package oakbot.doclet;

import java.nio.file.Path;
import java.util.Properties;

import oakbot.util.PropertiesWrapper;

/**
 * Gets the doclet's configuration properties.
 * @author Michael Angstadt
 */
public class ConfigProperties extends PropertiesWrapper {
	private static final String OUTPUT_PATH = "oakbot.doclet.output.path";
	private static final String PRETTY_PRINT = "oakbot.doclet.output.prettyPrint";
	private static final String LIBRARY_NAME = "oakbot.doclet.library.name";
	private static final String LIBRARY_VERSION = "oakbot.doclet.library.version";
	private static final String LIBRARY_BASEURL = "oakbot.doclet.library.baseUrl";
	private static final String LIBRARY_JAVADOC_URL_PATTERN = "oakbot.doclet.library.javadocUrlPattern";
	private static final String LIBRARY_PROJECT_URL = "oakbot.doclet.library.projectUrl";

	public ConfigProperties() {
		super();
	}

	public ConfigProperties(Properties properties) {
		super(properties);
	}

	public Path getOutputPath() {
		return getFile(OUTPUT_PATH);
	}

	public void setOutputPath(Path path) {
		set(OUTPUT_PATH, path);
	}

	public boolean isPrettyPrint() {
		return getBoolean(PRETTY_PRINT, false);
	}

	public void setPrettyPrint(boolean prettyPrint) {
		set(PRETTY_PRINT, prettyPrint);
	}

	public String getLibraryName() {
		return get(LIBRARY_NAME);
	}

	public void setLibraryName(String name) {
		set(LIBRARY_NAME, name);
	}

	public String getLibraryVersion() {
		return get(LIBRARY_VERSION);
	}

	public void setLibraryVersion(String version) {
		set(LIBRARY_VERSION, version);
	}

	public String getLibraryBaseUrl() {
		return get(LIBRARY_BASEURL);
	}

	public void setLibraryBaseUrl(String baseUrl) {
		set(LIBRARY_BASEURL, baseUrl);
	}

	public String getLibraryJavadocUrlPattern() {
		return get(LIBRARY_JAVADOC_URL_PATTERN);
	}

	public void setLibraryJavadocUrlPattern(String javadocUrlPattern) {
		set(LIBRARY_JAVADOC_URL_PATTERN, javadocUrlPattern);
	}

	public String getProjectUrl() {
		return get(LIBRARY_PROJECT_URL);
	}

	public void setProjectUrl(String url) {
		set(LIBRARY_PROJECT_URL, url);
	}

}
