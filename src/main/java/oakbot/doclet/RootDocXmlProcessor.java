package oakbot.doclet;

import static oakbot.util.XmlUtils.newDocument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;

/**
 * Converts the Javadoc info in a {@link ClassDoc} object to XML.
 * @author Michael Angstadt
 */
public final class RootDocXmlProcessor {
	/**
	 * Parses the Javadoc information out of a Javadoc {@link ClassDoc} object
	 * and into an XML file.
	 * @param classDoc the class to parse
	 * @return the XML document containing the Javadoc information
	 */
	public static Document parseClass(ClassDoc classDoc) {
		Document document = newDocument();
		Element classElement = parseClass(classDoc, document);
		document.appendChild(classElement);
		return document;
	}

	private static Element parseClass(ClassDoc classDoc, Document document) {
		Element element = document.createElement("class");

		//full name
		String fullName = classDoc.qualifiedTypeName();
		element.setAttribute("fullName", fullName);

		//simple name
		String simpleName = classDoc.simpleTypeName();
		element.setAttribute("simpleName", simpleName);

		//modifiers
		List<String> modifiers = new ArrayList<>();
		{
			boolean isAnnotation = isAnnotation(classDoc); //isAnnotationType() and isAnnotationTypeElement() don't work
			if (isAnnotation) {
				modifiers.add("annotation");
			} else if (classDoc.isException()) {
				modifiers.add("exception");
			} else if (classDoc.isEnum()) {
				modifiers.add("enum");
			} else if (classDoc.isClass()) {
				modifiers.add("class");
			}
			//note: no need to call isInterface()--"interface" is already included in the "modifiers()" method for interfaces

			modifiers.addAll(Arrays.asList(classDoc.modifiers().split("\\s+")));
			if (isAnnotation) {
				modifiers.remove("interface");
			}
		}
		if (!modifiers.isEmpty()) {
			element.setAttribute("modifiers", String.join(" ", modifiers));
		}

		//super class
		ClassDoc superClass = classDoc.superclass();
		if (superClass != null) {
			element.setAttribute("extends", superClass.qualifiedTypeName());
		}

		//interfaces
		List<String> implementsList = new ArrayList<>();
		for (ClassDoc interfaceDoc : classDoc.interfaces()) {
			implementsList.add(interfaceDoc.qualifiedTypeName());
		}
		if (!implementsList.isEmpty()) {
			element.setAttribute("implements", String.join(" ", implementsList));
		}

		//deprecated
		if (isDeprecated(classDoc)) {
			element.setAttribute("deprecated", "true");
		}

		//description
		String description = toMarkdown(classDoc.inlineTags());
		Element descriptionElement = document.createElement("description");
		descriptionElement.setTextContent(description);
		element.appendChild(descriptionElement);

		//constructors
		Element constructorsElement = document.createElement("constructors");
		for (ConstructorDoc constructor : classDoc.constructors()) {
			constructorsElement.appendChild(parseConstructor(constructor, document));
		}
		element.appendChild(constructorsElement);

		//methods
		Element methodsElement = document.createElement("methods");
		for (MethodDoc method : classDoc.methods()) {
			methodsElement.appendChild(parseMethod(method, document));
		}
		element.appendChild(methodsElement);

		//TODO java.lang.Object methods

		return element;
	}

	private static Element parseConstructor(ConstructorDoc constructor, Document document) {
		Element element = document.createElement("constructor");

		//deprecated
		if (isDeprecated(constructor)) {
			element.setAttribute("deprecated", "true");
		}

		//thrown exceptions
		List<String> exceptions = new ArrayList<>();
		for (Type type : constructor.thrownExceptionTypes()) {
			exceptions.add(type.qualifiedTypeName());
		}
		if (!exceptions.isEmpty()) {
			element.setAttribute("throws", String.join(" ", exceptions));
		}

		//description
		String description = toMarkdown(constructor.inlineTags());
		Element descriptionElement = document.createElement("description");
		descriptionElement.setTextContent(description);
		element.appendChild(descriptionElement);

		//parameters
		Element parametersElement = document.createElement("parameters");
		for (Parameter parameter : constructor.parameters()) {
			parametersElement.appendChild(parseParameter(parameter, document));
		}
		element.appendChild(parametersElement);

		return element;
	}

	private static Element parseMethod(MethodDoc method, Document document) {
		Element element = document.createElement("method");

		//name
		String name = method.name();
		element.setAttribute("name", name);

		//modifiers
		String modifiers = method.modifiers();
		element.setAttribute("modifiers", modifiers);

		//deprecated
		if (isDeprecated(method)) {
			element.setAttribute("deprecated", "true");
		}

		//return value
		String returns = method.returnType().qualifiedTypeName();
		element.setAttribute("returns", returns);

		//thrown exceptions
		List<String> exceptions = new ArrayList<>();
		for (Type type : method.thrownExceptionTypes()) {
			exceptions.add(type.qualifiedTypeName());
		}
		if (!exceptions.isEmpty()) {
			element.setAttribute("throws", String.join(" ", exceptions));
		}

		//description
		String description;
		MethodDoc overriddenMethod = findOverriddenMethod(method);
		if (overriddenMethod != null) {
			if (overriddenMethod.containingClass().isPackagePrivate()) {
				description = toMarkdown(overriddenMethod.inlineTags());
			} else {
				String qualifiedName = overriddenMethod.qualifiedName();
				int pos = qualifiedName.lastIndexOf('.');
				qualifiedName = qualifiedName.substring(0, pos) + "#" + qualifiedName.substring(pos + 1);

				//e.g. oakbot.javadoc.PageParser#parseClassPage(org.jsoup.nodes.Document, java.lang.String)
				element.setAttribute("overrides", qualifiedName + overriddenMethod.signature());

				description = toMarkdown(method.inlineTags());
			}
		} else {
			description = toMarkdown(method.inlineTags());
		}
		Element descriptionElement = document.createElement("description");
		descriptionElement.setTextContent(description);
		element.appendChild(descriptionElement);

		//parameters
		Element parametersElement = document.createElement("parameters");
		for (Parameter parameter : method.parameters()) {
			parametersElement.appendChild(parseParameter(parameter, document));
		}
		element.appendChild(parametersElement);

		return element;
	}

	private static Element parseParameter(Parameter parameter, Document document) {
		Element element = document.createElement("parameter");

		String name = parameter.name();
		element.setAttribute("name", name);

		String type = parameter.type().qualifiedTypeName();
		element.setAttribute("type", type + parameter.type().dimension());

		return element;
	}

	/**
	 * Determines if a class is an annotation.
	 * @param classDoc the class
	 * @return true if it's an annotation, false if not
	 */
	private static boolean isAnnotation(ClassDoc classDoc) {
		for (ClassDoc interfaceDoc : classDoc.interfaces()) {
			ClassDoc superClass = interfaceDoc;
			do {
				if ("java.lang.annotation.Annotation".equals(superClass.qualifiedTypeName())) {
					return true;
				}
			} while ((superClass = superClass.superclass()) != null);
		}
		return false;
	}

	/**
	 * Determines if a class or method is deprecated.
	 * @param element the class or method
	 * @return true if it's deprecated, false if not
	 */
	private static boolean isDeprecated(ProgramElementDoc element) {
		for (AnnotationDesc annotation : element.annotations()) {
			if ("Deprecated".equals(annotation.annotationType().simpleTypeName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Finds the original method which is being overridden by a given method.
	 * @param method the method which is overriding another method
	 * @return the method that was overridden or null if not found
	 */
	private static MethodDoc findOverriddenMethod(MethodDoc method) {
		MethodDoc overriddenMethod = method.overriddenMethod();
		if (overriddenMethod != null) {
			return overriddenMethod;
		}

		Parameter[] methodParams = method.parameters();
		for (ClassDoc interfaceDoc : method.containingClass().interfaces()) {
			for (MethodDoc interfaceMethod : interfaceDoc.methods()) {
				if (!interfaceMethod.name().equals(method.name())) {
					continue;
				}

				Parameter[] interfaceMethodParams = interfaceMethod.parameters();
				if (equals(methodParams, interfaceMethodParams)) {
					return interfaceMethod;
				}
			}
		}
		return null;
	}

	/**
	 * Compares two arrays of method parameters for equality.
	 * @param parameters1 the first array
	 * @param parameters2 the second array
	 * @return true if they are equal, false if not
	 */
	private static boolean equals(Parameter[] parameters1, Parameter[] parameters2) {
		if (parameters1.length != parameters2.length) {
			return false;
		}

		for (int i = 0; i < parameters1.length; i++) {
			Parameter one = parameters1[i];
			Parameter two = parameters2[i];
			if (!one.type().qualifiedTypeName().equals(two.type().qualifiedTypeName())) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Converts a Javadoc description to SO-Chat markdown.
	 * @param inlineTags the description
	 * @return the markdown
	 */
	private static String toMarkdown(Tag inlineTags[]) {
		/*
		 * Combine all the Tags into a single string, converting Javadoc tags
		 * (like "@code") into HTML.
		 */
		StringBuilder sb = new StringBuilder();
		for (Tag tag : inlineTags) {
			String text = tag.text();
			switch (tag.name()) {
			case "@code":
				sb.append("<code>").append(escapeHtml(text)).append("</code>");
				break;
			case "@link":
			case "@linkplain":
				//TODO format as a <a> link
				int space = text.indexOf(' ');
				sb.append((space < 0) ? text : text.substring(space + 1));
				break;
			case "@literal":
				sb.append(escapeHtml(text));
				break;
			default:
				sb.append(text);
				break;
			}
		}

		String html = sb.toString();
		org.jsoup.nodes.Document document = Jsoup.parse(html);
		DescriptionNodeVisitor visitor = new DescriptionNodeVisitor();
		document.traverse(visitor);
		return visitor.getDescription();
	}

	/**
	 * Escapes a string for safe inclusion in HTML.
	 * @param text the text to escape
	 * @return the escaped text
	 */
	private static String escapeHtml(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private RootDocXmlProcessor() {
		//hide
	}
}
