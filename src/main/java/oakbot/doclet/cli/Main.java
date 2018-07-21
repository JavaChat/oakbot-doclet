package oakbot.doclet.cli;

import static oakbot.util.JunkDrawer.WINDOWS_OS;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import oakbot.doclet.ConfigProperties;
import oakbot.doclet.OakbotDoclet;

/**
 * A command-line interface for generating a Javadoc ZIP file for OakBot.
 * @author Michael Angstadt
 */
public class Main {
	private static final Console console = new Console();
	private static Path tempDir;

	public static void main(String args[]) throws Exception {
		Arguments arguments = new Arguments(args);
		if (arguments.help()) {
			arguments.printHelp();
			return;
		}

		String javadocExe = getJavadocExe();

		String libraryName, libraryVersion, libraryJavadocUrl, libraryJavadocUrlPattern, libraryWebsite;
		List<String> excludePackages = new ArrayList<>();
		boolean prettyPrint;
		MavenLibrary library = null;
		Path source = null, output = null;
		if (arguments.interactive()) {
			console.printf("Welcome to the OakBot Javadoc Generator.%n");
			while (true) {
				String answer = console.readLine("Enter Maven ID or path to source ZIP/JAR/folder: ");
				if (answer.isEmpty()) {
					continue;
				}

				try {
					library = MavenLibrary.parse(answer);
				} catch (IllegalArgumentException e) {
					source = Paths.get(answer);
					if (!Files.exists(source)) {
						console.printf("File or folder does not exist: " + source + "%n");
						continue;
					}
				}
				break;
			}

			libraryName = readLibraryName((library == null) ? null : library.getArtifactId());
			libraryVersion = readLibraryVersion((library == null) ? null : library.getVersion());
			libraryJavadocUrl = readLibraryJavadocUrl();
			libraryJavadocUrlPattern = readLibraryJavadocUrlPattern();
			libraryWebsite = readLibraryWebsite();
			prettyPrint = readPrettyPrint();
			excludePackages = readExcludePackages();
			String defaultOutput = libraryName + "-" + libraryVersion + ".zip";
			output = Paths.get(readOutput(defaultOutput));
			if (!confirmSettings(javadocExe, library, source, libraryName, libraryVersion, libraryJavadocUrl, libraryJavadocUrlPattern, libraryWebsite, prettyPrint, excludePackages, output)) {
				return;
			}
		} else {
			source = arguments.src();
			if (source != null && !Files.exists(source)) {
				die("Source code location does not exist: " + source);
			}

			library = arguments.maven();
			if (source == null && library == null) {
				die("Either a source code directory/ZIP (--src) or Maven coordinates (--maven) must be specified.");
			}

			libraryName = arguments.name();
			if (libraryName.isEmpty()) {
				if (library == null) {
					die("A library name (--name) must be specified if the --src parameter is used.");
				}
				libraryName = library.getArtifactId();
			}

			libraryVersion = arguments.ver();
			if (libraryVersion.isEmpty()) {
				if (library == null) {
					die("A library version (--ver) must be specified if the --src parameter is used.");
				}
				libraryVersion = library.getVersion();
			}

			libraryJavadocUrl = arguments.javadocUrl();
			libraryJavadocUrlPattern = arguments.javadocUrlPattern();
			libraryWebsite = arguments.website();
			excludePackages = arguments.excludePackages();
			prettyPrint = arguments.prettyPrint();
			output = arguments.output();
		}

		tempDir = Files.createTempDirectory("oakbot.doclet");

		try {
			List<Path> dependencyJars;
			if (source == null) {
				source = downloadSource(library);
				downloadPom(library);
				dependencyJars = downloadDependencies();
			} else {
				dependencyJars = Collections.emptyList();
			}

			Path sourceDir = Files.isDirectory(source) ? source : unzipSource(source);

			List<String> commands = buildJavadocArgs(javadocExe, dependencyJars, sourceDir, libraryName, libraryVersion, libraryJavadocUrl, libraryJavadocUrlPattern, libraryWebsite, excludePackages, prettyPrint, output);
			runJavadoc(commands, arguments.verbose());
		} finally {
			deleteTempDir();
		}
	}

	private static void die(String message) {
		System.err.println(message);
		System.exit(1);
	}

	private static void deleteTempDir() throws IOException {
		console.printf("Cleaning up...");
		rmdirRecursive(tempDir);
		console.printf("done.%n");
	}

	private static List<String> buildJavadocArgs(String javadocExe, List<Path> dependencies, Path source, String name, String version, String javadocUrl, String javadocUrlPattern, String website, List<String> excludePackages, boolean prettyPrint, Path output) throws IOException {
		List<String> commands = new ArrayList<>();
		commands.add(javadocExe);

		commands.add("-doclet");
		commands.add(OakbotDoclet.class.getName());

		String docletClasspath = getClasspath();
		commands.add("-docletpath");
		commands.add(docletClasspath);

		if (!dependencies.isEmpty()) {
			commands.add("-classpath");
			commands.add(buildClasspath(dependencies));
		}

		commands.add("-sourcepath");
		commands.add(source.toString());

		for (String subpackage : getSubpackages(source)) {
			commands.add("-subpackages");
			commands.add(subpackage);
		}

		for (String excludePackage : excludePackages) {
			commands.add("-exclude");
			commands.add(excludePackage);
		}

		ConfigProperties config = new ConfigProperties();
		config.setOutputPath(output);
		config.setPrettyPrint(prettyPrint);
		config.setLibraryName(name);
		config.setLibraryVersion(version);
		if (!javadocUrl.isEmpty()) {
			config.setLibraryBaseUrl(javadocUrl);
		}
		if (!javadocUrlPattern.isEmpty()) {
			config.setLibraryJavadocUrlPattern(javadocUrlPattern);
		}
		if (!website.isEmpty()) {
			config.setProjectUrl(website);
		}

		for (Map.Entry<String, String> entry : config) {
			commands.add("-J-D" + entry.getKey() + "=" + entry.getValue());
		}

		commands.add("-J-Xmx1024m");

		return commands;
	}

	private static void runJavadoc(List<String> commands, boolean verbose) throws IOException, InterruptedException {
		if (verbose) {
			console.printf("Starting doclet with commands: " + commands + "%n");
		} else {
			console.printf("Starting doclet...%n");
		}

		ProcessBuilder builder = new ProcessBuilder(commands);
		Process process = builder.start();
		pipeOutput(process);
		process.waitFor();
	}

	private static List<Path> downloadDependencies() throws IOException, InterruptedException {
		//use Maven to perform the dependency resolution
		ProcessBuilder builder = new ProcessBuilder("mvn", "dependency:copy-dependencies");
		builder.directory(tempDir.toFile());
		Process process = builder.start();
		pipeOutput(process);
		int exitValue = process.waitFor();
		if (exitValue != 0) {
			throw new RuntimeException("Maven processed failed.");
		}

		Path dependencyDir = tempDir.resolve(Paths.get("target", "dependency"));
		if (!Files.exists(dependencyDir)) {
			return new ArrayList<>(0);
		}

		//build a list of all the JARs
		List<Path> jars = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dependencyDir, entry -> {
			String filename = entry.getFileName().toString();
			return filename.endsWith(".jar");
		})) {
			stream.forEach(path -> jars.add(path));
		}
		return jars;
	}

	private static Path downloadPom(MavenLibrary library) throws IOException {
		console.printf("Downloading project POM...");
		Path dest = tempDir.resolve("pom.xml");
		String url = library.getPomUrl();
		download(url, dest, null);
		console.printf("done.%n");
		return dest;
	}

	private static Path downloadSource(MavenLibrary library) throws IOException {
		String url = library.getSourcesUrl();
		int pos = url.lastIndexOf('/');
		String filename = url.substring(pos + 1);
		Path dest = tempDir.resolve(filename);

		download(url, dest, (downloaded, size) -> {
			int percent = (int) ((double) downloaded / size * 100);
			int downloadedKb = downloaded / 1024;
			int sizeKb = size / 1024;
			console.printf("\rDownloading " + filename + " (" + downloadedKb + "KB / " + sizeKb + "KB, " + percent + "%%)");
		});
		console.printf("%n");

		return dest;
	}

	private static String getJavadocExe() {
		String javaHomeEnv = System.getenv("JAVA_HOME");
		if (javaHomeEnv == null) {
			die("Please set your JAVA_HOME environment variable: export JAVA_HOME=/path/to/jdk");
		}

		String executable = WINDOWS_OS ? "javadoc.exe" : "javadoc";
		Path javadoc = Paths.get(javaHomeEnv, "bin", executable);
		if (!Files.exists(javadoc)) {
			die("JAVA_HOME path is not valid or does not contain a \"javadoc\" executable: " + javaHomeEnv);
		}

		return javadoc.toString();
	}

	private static String readLibraryName(String defaultValue) {
		return readLineDefault("Library name", defaultValue);
	}

	private static String readLibraryVersion(String defaultValue) {
		return readLineDefault("Library version", defaultValue);
	}

	private static String readLineDefault(String message, String defaultValue) {
		StringBuilder sb = new StringBuilder(message);
		if (defaultValue != null) {
			sb.append(" [").append(defaultValue).append(']');
		}
		sb.append(": ");
		message = sb.toString();

		while (true) {
			String answer = console.readLine(message);
			if (answer.contains(" ") || answer.contains("\t")) {
				console.printf("Value cannot contain whitespace.%n");
				continue;
			}

			if (!answer.isEmpty()) {
				return answer;
			}
			if (defaultValue != null) {
				return defaultValue;
			}
		}
	}

	private static String readLibraryJavadocUrl() {
		return console.readLine("Library's base javadoc URL (optional): ");
	}

	private static String readLibraryJavadocUrlPattern() {
		return console.readLine("Library's javadoc URL pattern (optional): ");
	}

	private static String readLibraryWebsite() {
		return console.readLine("Library's website (optional): ");
	}

	private static boolean readPrettyPrint() {
		String answer = console.readLine("Pretty-print the XML? [y/N] ");
		return "y".equalsIgnoreCase(answer);
	}

	private static List<String> readExcludePackages() {
		String answer = console.readLine("Enter a comma separated list of packages you want to exclude (optional): ");
		return answer.isEmpty() ? Collections.emptyList() : Arrays.asList(answer.split("\\s*,\\s*"));
	}

	private static String readOutput(String defaultValue) {
		String answer = console.readLine("Save ZIP file as: [" + defaultValue + "] ");
		return answer.isEmpty() ? defaultValue : answer;
	}

	private static boolean confirmSettings(String javadocExe, MavenLibrary library, Path source, String libraryName, String libraryVersion, String libraryJavadocUrl, String libraryJavadocUrlPattern, String libraryWebsite, boolean prettyPrint, List<String> excludePackages, Path output) throws IOException {
		console.printf("=============Confirmation=============%n");
		console.printf("Javadoc executable: " + javadocExe + "%n");
		if (library != null) {
			console.printf("Maven ID: " + library + "%n");
		}
		if (source != null) {
			console.printf("Source: " + source + "%n");
		}
		console.printf("Library Name: " + libraryName + "%n");
		console.printf("Version: " + libraryVersion + "%n");
		console.printf("Base javadoc URL: " + libraryJavadocUrl + "%n");
		console.printf("Javadoc URL pattern: " + libraryJavadocUrlPattern + "%n");
		console.printf("Website: " + libraryWebsite + "%n");
		console.printf("Pretty print XML: " + prettyPrint + "%n");
		console.printf("Exclude packages: " + excludePackages + "%n");
		console.printf("Save ZIP file to: " + output + "%n");

		String answer = console.readLine("Proceed? [Y/n] ");
		return answer.isEmpty() || "y".equalsIgnoreCase(answer);
	}

	private static Path unzipSource(Path sourceJar) throws IOException {
		console.printf("Extracting files from source archive...");
		Path dir = tempDir.resolve("src");
		Files.createDirectory(dir);
		unzip(dir, sourceJar);
		console.printf("done.%n");
		return dir;
	}

	private static String getClasspath() {
		URL urls[] = ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs();
		List<Path> paths = new ArrayList<>(urls.length);
		for (URL url : urls) {
			String urlPath = url.getPath();
			if (WINDOWS_OS) {
				//example URL: /C:/Users/mikea/OakBot/workspace/oakbot-doclet/target/oakbot-doclet-0.0.4-jar-with-dependencies.jar
				//@formatter:off
				urlPath = urlPath
					.substring(1) //remove the first slash
					.replace('/', '\\'); //use backslashes
				//@formatter:on
			}

			Path path = Paths.get(urlPath);
			paths.add(path);
		}

		return buildClasspath(paths);
	}

	private static List<String> getSubpackages(Path sourceDir) throws IOException {
		List<String> subpackages = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir, entry -> {
			if (!Files.isDirectory(entry)) {
				return false;
			}

			String filename = entry.getFileName().toString();
			return !filename.equals("META-INF");
		})) {
			stream.forEach(path -> subpackages.add(path.getFileName().toString()));
		}

		return subpackages;
	}

	/**
	 * Builds a classpath string.
	 * @param paths the files and directories to include in the classpath
	 * @return the classpath string
	 */
	private static String buildClasspath(Collection<Path> paths) {
		List<String> pathStrings = new ArrayList<>(paths.size());
		for (Path path : paths) {
			pathStrings.add(path.toString());
		}

		String separator = System.getProperty("path.separator");
		return String.join(separator, pathStrings);
	}

	/**
	 * Downloads a file from the Internet.
	 * @param url the file URL
	 * @param destination where to save the file to
	 * @param progress callback for monitoring the download progress (can be
	 * null)
	 * @throws IOException
	 */
	private static void download(String url, Path destination, Progress progress) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		try (InputStream in = connection.getInputStream()) {
			if (progress == null) {
				Files.copy(in, destination);
				return;
			}

			int size = connection.getContentLength();
			int downloaded = 0;
			byte buffer[] = new byte[1024 * 16];
			try (OutputStream out = Files.newOutputStream(destination)) {
				int read;
				while ((read = in.read(buffer)) != -1) {
					out.write(buffer, 0, read);
					downloaded += read;
					progress.progress(downloaded, size);
				}
			}
		}
	}

	private interface Progress {
		void progress(int downloaded, int size);
	}

	/**
	 * Extracts all the files in a ZIP file.
	 * @param destinationDir the destination directory.
	 * @param zipFile the ZIP file
	 * @throws IOException
	 */
	private static void unzip(Path destinationDir, Path zipFile) throws IOException {
		try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(zipFile))) {
			ZipEntry entry;
			while ((entry = zin.getNextEntry()) != null) {
				String zipPath = entry.getName();
				Path destFile = destinationDir.resolve(zipPath);

				//entry is a directory
				if (zipPath.endsWith("/")) {
					if (!Files.exists(destFile)) {
						Files.createDirectories(destFile);
					}
					continue;
				}

				//make sure the parent directory exists
				Path parent = destFile.getParent();
				if (!Files.exists(parent)) {
					Files.createDirectories(parent);
				}

				//copy the file
				Files.copy(zin, destFile, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	/**
	 * Recursively deletes a directory.
	 * @param directory the directory to delete
	 * @throws IOException
	 * @see http://stackoverflow.com/a/8685959/13379
	 */
	private static void rmdirRecursive(Path directory) throws IOException {
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException {
				/*
				 * Try to delete the file anyway, even if its attributes could
				 * not be read, since delete-only access is theoretically
				 * possible.
				 */
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
				if (exception == null) {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				} else {
					// directory iteration failed; propagate exception
					throw exception;
				}
			}
		});
	}

	/**
	 * Pipes the output of a process to this program's stdout and stderr
	 * streams.
	 * @param process the process
	 */
	private static void pipeOutput(Process process) {
		PipeThread stdout = new PipeThread(process.getInputStream(), System.out);
		PipeThread stderr = new PipeThread(process.getErrorStream(), System.err);
		stdout.start();
		stderr.start();
	}

	private static class PipeThread extends Thread {
		private final InputStream in;
		private final PrintStream out;

		public PipeThread(InputStream in, PrintStream out) {
			this.in = in;
			this.out = out;
			setDaemon(true);
		}

		@Override
		public void run() {
			try (InputStream in = new BufferedInputStream(this.in)) {
				int read;
				while ((read = in.read()) != -1) {
					out.print((char) read);
				}
			} catch (IOException e) {
				//shouldn't be thrown
			}
		}
	}

	/**
	 * Mimics the {@link java.io.Console} class so that this program can be run
	 * from Eclipse.
	 * @author Michael Angstadt
	 */
	private static class Console {
		private final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		public void printf(String message, Object... objects) {
			System.out.printf(message, objects);
		}

		public String readLine(String message) {
			System.out.print(message);
			try {
				return in.readLine().trim();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
