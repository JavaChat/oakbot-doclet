package oakbot.doclet;

import static oakbot.util.XmlUtils.newDocument;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;

/**
 * A custom Javadoc doclet that saves class information to XML files inside of a
 * ZIP file.
 * @author Michael Angstadt
 */
public class OakbotDoclet {
	private static final ConfigProperties properties = new ConfigProperties(System.getProperties());

	private static final Transformer transformer;
	static {
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (TransformerException e) {
			//should never be thrown
			throw new RuntimeException(e);
		}

		if (properties.isPrettyPrint()) {
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		}
	}

	/**
	 * The entry point for the {@code javadoc} command.
	 * @param rootDoc contains the parsed javadoc information
	 * @return true if successful, false if not
	 * @throws Exception if an error occurred during the parsing
	 */
	public static boolean start(RootDoc rootDoc) throws Exception {
		Path outputPath = properties.getOutputPath();
		if (outputPath == null) {
			outputPath = Paths.get(defaultZipFilename());
		} else if (Files.isDirectory(outputPath)) {
			outputPath = outputPath.resolve(defaultZipFilename());
		}

		System.out.println("Saving to: " + outputPath);

		Path tempFile = Files.createTempFile("oakbot-doclet-javadocs", ".zip");
		Files.delete(tempFile); //file must be deleted, otherwise the ZIP file will not get created
		try {
			try (FileSystem fs = createZip(tempFile)) {
				createClassFiles(fs, rootDoc);
				createInfoFile(fs);
			}
			Files.move(tempFile, outputPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			Files.deleteIfExists(tempFile);
			throw e;
		}

		return true;
	}

	private static String defaultZipFilename() {
		return properties.getLibraryName() + "-" + properties.getLibraryVersion() + ".zip";
	}

	/**
	 * Creates and opens a ZIP file.
	 * @param file the path to the file
	 * @return the ZIP file system
	 * @throws IOException if there's a problem creating the file
	 */
	private static FileSystem createZip(Path file) throws IOException {
		URI uri = URI.create("jar:file:" + file.toAbsolutePath());
		Map<String, String> env = new HashMap<>();
		env.put("create", "true");
		return FileSystems.newFileSystem(uri, env);
	}

	/**
	 * Creates the "info.xml" file.
	 * @param fs the ZIP file
	 * @throws IOException if there's a problem creating the file
	 */
	private static void createInfoFile(FileSystem fs) throws IOException {
		Document document = newDocument();
		Element element = document.createElement("info");
		setAttribute("name", properties.getLibraryName(), element);
		setAttribute("version", properties.getLibraryVersion(), element);
		setAttribute("baseUrl", properties.getLibraryBaseUrl(), element);
		setAttribute("javadocUrlPattern", properties.getLibraryJavadocUrlPattern(), element);
		setAttribute("projectUrl", properties.getProjectUrl(), element);
		element.setAttribute("generated", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()));
		document.appendChild(element);

		Path path = fs.getPath("info.xml");
		writeXmlDocument(document, path);
	}

	private static void setAttribute(String name, String value, Element element) {
		if (value == null || value.isEmpty()) {
			return;
		}

		element.setAttribute(name, value);
	}

	/**
	 * Creates the XML files containing the Javadoc information of each class.
	 * @param fs the ZIP file
	 * @param rootDoc the Javadoc information
	 * @throws IOException if there's a problem writing to the ZIP file
	 */
	private static void createClassFiles(FileSystem fs, RootDoc rootDoc) throws IOException {
		ClassDoc classDocs[] = rootDoc.classes();
		ProgressPrinter progress = new ProgressPrinter(classDocs.length);
		for (ClassDoc classDoc : classDocs) {
			progress.print(classDoc);

			Document document = RootDocXmlProcessor.toDocument(classDoc);
			Path path = fs.getPath(classFilePath(classDoc));
			Files.createDirectories(path.getParent());
			writeXmlDocument(document, path);
		}
		System.out.println();
	}

	/**
	 * Builds the path string for where to save a class's Javadoc XML file.
	 * @param classDoc the class
	 * @return the path string
	 */
	private static String classFilePath(ClassDoc classDoc) {
		/*
		 * Note: We can't just use classDoc.qualifiedName() because, if we
		 * replace all dots with slashes, then inner classes will not work
		 * right. For example, "Map.Entry" will turn into
		 * "java/util/Map/Entry.xml".
		 */
		PackageDoc packageDoc = classDoc.containingPackage();

		StringBuilder sb = new StringBuilder();
		if (packageDoc != null) {
			sb.append(packageDoc.name().replace('.', '/')).append('/');
		}

		List<String> containingClasses = new ArrayList<>();
		ClassDoc containingClassDoc = classDoc;
		while ((containingClassDoc = containingClassDoc.containingClass()) != null) {
			containingClasses.add(containingClassDoc.simpleTypeName());
		}

		Collections.reverse(containingClasses);
		for (String containingClass : containingClasses) {
			sb.append(containingClass).append('.');
		}

		sb.append(classDoc.simpleTypeName()).append(".xml");

		return sb.toString();
	}

	/**
	 * Writes an XML document to a file.
	 * @param document the XML document
	 * @param file the file
	 * @throws IOException if there's a problem writing to the file
	 */
	private static void writeXmlDocument(Document document, Path file) throws IOException {
		DOMSource source = new DOMSource(document);
		try (Writer writer = Files.newBufferedWriter(file)) {
			StreamResult result = new StreamResult(writer);
			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Outputs the status of the parsing operation.
	 */
	private static class ProgressPrinter {
		private final int totalClasses;
		private int classesParsed = 0;
		private int prevMessageLength = 0;

		/**
		 * @param totalClasses the total number of classes being parsed
		 */
		public ProgressPrinter(int totalClasses) {
			this.totalClasses = totalClasses;
		}

		/**
		 * Prints a message saying that a class is about to be parsed.
		 * @param next the next class to be parsed
		 */
		public void print(ClassDoc next) {
			StringBuilder sb = new StringBuilder();
			sb.append("Parsing ").append(++classesParsed).append('/').append(totalClasses);
			sb.append(" (").append(next.simpleTypeName()).append(')');

			int curMessageLength = sb.length();
			int spaces = prevMessageLength - curMessageLength;
			for (int i = 0; i < spaces; i++) {
				sb.append(' ');
			}
			prevMessageLength = curMessageLength;

			System.out.print('\r' + sb.toString());
		}
	}
}
