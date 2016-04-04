package oakbot.doclet;

import static oakbot.util.XmlUtils.newDocument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;

/**
 * Converts the Javadoc info in a {@link ClassDoc} object to XML.
 * @author Michael Angstadt
 */
public final class RootDocXmlProcessor {
	private final Document document;

	/**
	 * Parses the Javadoc information out of a Javadoc {@link ClassDoc} object
	 * and into an XML document.
	 * @param classDoc the class to parse
	 * @return the XML document containing the Javadoc information
	 */
	public static Document toDocument(ClassDoc classDoc) {
		Document document = newDocument();
		RootDocXmlProcessor processor = new RootDocXmlProcessor(document);
		Element element = processor.parseClass(classDoc);
		document.appendChild(element);
		return document;
	}

	private RootDocXmlProcessor(Document document) {
		this.document = document;
	}

	private Element parseClass(ClassDoc classDoc) {
		Element element = document.createElement("class");

		applyClassNameAttribute("name", classDoc, element);

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
			applyClassNameAttribute("extends", superClass, element);
		}

		//interfaces
		applyClassNameAttribute("implements", classDoc.interfaces(), element);

		//deprecated
		if (isDeprecated(classDoc)) {
			element.setAttribute("deprecated", "true");
		}

		//since
		String since = parseSince(classDoc);
		if (since != null) {
			element.setAttribute("since", since);
		}

		//description
		String description = toMarkdown(classDoc);
		Element descriptionElement = document.createElement("description");
		descriptionElement.setTextContent(description);
		element.appendChild(descriptionElement);

		//constructors
		for (ConstructorDoc constructor : classDoc.constructors()) {
			element.appendChild(parseConstructor(constructor));
		}

		//methods
		for (MethodDoc method : classDoc.methods()) {
			element.appendChild(parseMethod(method));
		}

		//TODO java.lang.Object methods

		return element;
	}

	private Element parseConstructor(ConstructorDoc constructor) {
		Element element = document.createElement("constructor");

		//deprecated
		if (isDeprecated(constructor)) {
			element.setAttribute("deprecated", "true");
		}

		//thrown exceptions
		applyClassNameAttribute("throws", constructor.thrownExceptionTypes(), element);

		//since
		String since = parseSince(constructor);
		if (since != null) {
			element.setAttribute("since", since);
		}

		//description
		String description = toMarkdown(constructor);
		Element descriptionElement = document.createElement("description");
		descriptionElement.setTextContent(description);
		element.appendChild(descriptionElement);

		//parameters
		for (Parameter parameter : constructor.parameters()) {
			element.appendChild(parseParameter(parameter));
		}

		return element;
	}

	private Element parseMethod(MethodDoc method) {
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
		Type returnType = method.returnType();
		if (!"void".equals(returnType.qualifiedTypeName())) {
			applyClassNameAttribute("returns", returnType, element);
		}

		//thrown exceptions
		applyClassNameAttribute("throws", method.thrownExceptionTypes(), element);

		//since
		String since = parseSince(method);
		if (since != null) {
			element.setAttribute("since", since);
		}

		//description
		String description;
		MethodDoc overriddenMethod = findOverriddenMethod(method);
		if (overriddenMethod != null) {
			if (overriddenMethod.containingClass().isPackagePrivate()) {
				description = toMarkdown(overriddenMethod);
			} else {
				element.setAttribute("overrides", methodName(overriddenMethod));
				description = toMarkdown(method);
			}
		} else {
			description = toMarkdown(method);
		}
		Element descriptionElement = document.createElement("description");
		descriptionElement.setTextContent(description);
		element.appendChild(descriptionElement);

		//parameters
		for (Parameter parameter : method.parameters()) {
			element.appendChild(parseParameter(parameter));
		}

		return element;
	}

	private Element parseParameter(Parameter parameter) {
		Element element = document.createElement("parameter");

		String name = parameter.name();
		element.setAttribute("name", name);

		Type type = parameter.type();
		applyClassNameAttribute("type", type, element);

		return element;
	}

	/**
	 * Add a specially formatted attribute to an element that contains a class's
	 * fully qualified name. For example,
	 * {@code <class name="java.util|Map.Entry">}
	 * @param attributeName the name of the attribute to add
	 * @param type the type information
	 * @param element the element
	 */
	private static void applyClassNameAttribute(String attributeName, Type type, Element element) {
		applyClassNameAttribute(attributeName, new Type[] { type }, element);
	}

	/**
	 * Add a specially formatted attribute to an element that contains a class's
	 * fully qualified name. For example,
	 * {@code <class name="java.util|Map.Entry">}
	 * @param attributeName the name of the attribute to add
	 * @param types the type information
	 * @param element the element
	 */
	private static void applyClassNameAttribute(String attributeName, Type types[], Element element) {
		if (types.length == 0) {
			return;
		}

		List<String> names = Arrays.stream(types).map(RootDocXmlProcessor::typeName).collect(Collectors.toList());
		element.setAttribute(attributeName, String.join(" ", names));
	}

	/**
	 * Add a specially formatted attribute to an element that contains a class's
	 * fully qualified name. For example,
	 * {@code <class name="java.util|Map.Entry">}
	 * @param attributeName the name of the attribute to add
	 * @param classDoc the class information
	 * @param element the element
	 */
	private static void applyClassNameAttribute(String attributeName, ClassDoc classDoc, Element element) {
		applyClassNameAttribute(attributeName, new ClassDoc[] { classDoc }, element);
	}

	/**
	 * Add a specially formatted attribute to an element that contains a class's
	 * fully qualified name. For example,
	 * {@code <class name="java.util|Map.Entry">}
	 * @param attributeName the name of the attribute to add
	 * @param classDocs the class information
	 * @param element the element
	 */
	private static void applyClassNameAttribute(String attributeName, ClassDoc classDocs[], Element element) {
		if (classDocs.length == 0) {
			return;
		}

		List<String> names = Arrays.stream(classDocs).map(RootDocXmlProcessor::className).collect(Collectors.toList());
		element.setAttribute(attributeName, String.join(" ", names));
	}

	/**
	 * Builds a specially formatted string that is used to define a type's fully
	 * qualified name.
	 * @param type the type information
	 * @return the fully qualified name (e.g. "java.lang|String[]")
	 */
	private static String typeName(Type type) {
		ClassDoc classDoc = type.asClassDoc();
		String typeName = (classDoc == null) ? type.simpleTypeName() : className(classDoc);
		return typeName + type.dimension();
	}

	/**
	 * Builds a specially formatted string that is used to define a parameter's
	 * fully qualified name.
	 * @param parameter the parameter information
	 * @return the fully qualified name (e.g. "java.lang|String[]")
	 */
	private static String parameterName(Parameter parameter) {
		return typeName(parameter.type());
	}

	/**
	 * Builds a specially formatted string that is used to define a method's
	 * fully qualified name.
	 * @param methodDoc the method information
	 * @return the fully qualified name (e.g.
	 * "java.util|Map.Entry#equals(java.lang|Object)")
	 */
	private static String methodName(MethodDoc methodDoc) {
		String className = className(methodDoc.containingClass());
		String methodName = methodDoc.name();
		List<String> parameterNames = Arrays.stream(methodDoc.parameters()).map(RootDocXmlProcessor::parameterName).collect(Collectors.toList());

		return className + '#' + methodName + '(' + String.join(", ", parameterNames) + ')';
	}

	/**
	 * Builds a specially formatted string that is used to define a class's
	 * fully qualified name.
	 * @param classDoc the class information
	 * @return the fully qualified name (e.g. "java.util|Map.Entry")
	 */
	private static String className(ClassDoc classDoc) {
		StringBuilder sb = new StringBuilder();

		//package
		PackageDoc packageDoc = classDoc.containingPackage();
		if (packageDoc != null) {
			sb.append(packageDoc.name()).append('|');
		}

		//outerClasses
		List<String> outerClasses = new ArrayList<>(1);
		ClassDoc outer = classDoc;
		while ((outer = outer.containingClass()) != null) {
			outerClasses.add(outer.simpleTypeName());
		}
		if (!outerClasses.isEmpty()) {
			Collections.reverse(outerClasses);
			sb.append(String.join(".", outerClasses)).append('.');
		}

		//simple name
		sb.append(classDoc.simpleTypeName());

		return sb.toString();
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
	 * Gets the value of an element's {@literal @since} tag, if present.
	 * @param doc the Javadoc element.
	 * @return the {@literal @since} tag text or null if not found
	 */
	private static String parseSince(Doc doc) {
		Tag tags[] = doc.tags("@since");
		return (tags.length == 0) ? null : tags[0].text();
	}

	/**
	 * Converts a Javadoc element's description to SO-Chat markdown.
	 * @param doc the Javadoc element
	 * @return the markdown
	 */
	private static String toMarkdown(Doc doc) {
		/*
		 * Combine all the Tags into a single string, converting Javadoc tags
		 * (like "@code") into HTML.
		 */
		StringBuilder sb = new StringBuilder();
		for (Tag tag : doc.inlineTags()) {
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
}
