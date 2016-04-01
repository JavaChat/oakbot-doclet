package oakbot.util;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

/**
 * Contains XML utility methods.
 * @author Michael Angstadt
 */
public class XmlUtils {
	/**
	 * Creates an empty XML document.
	 * @return the XML document
	 */
	public static Document newDocument() {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			//should never be thrown
			throw new RuntimeException(e);
		}
	}
}
