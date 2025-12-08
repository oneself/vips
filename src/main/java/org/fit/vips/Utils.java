package org.fit.vips;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.fit.cssbox.layout.ElementBox;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Utils {

	public static void setEvincedIds(Node node) {
		setEvincedIds(node, new IntWrapper());
	}

	private static void setEvincedIds(Node node, IntWrapper evincedId) {
		evincedId.increase();
		if (node instanceof Element) {
			((Element) node).setAttribute("evinced_id", String.valueOf(evincedId.get()));
		}
		for (int i = 0; i < node.getChildNodes().getLength(); i++) {
			setEvincedIds(node.getChildNodes().item(i), evincedId);
		}
	}

	private static Element initHtmlDoc(Document htmlDoc) {
		Element htmlRoot = htmlDoc.createElement("html");
		htmlDoc.appendChild(htmlRoot);
		Element htmlBody = htmlDoc.createElement("body");
		htmlRoot.appendChild(htmlBody);
		return htmlBody;
	}

	private static void createCollapsableTree(Document htmlDoc, Element htmlBody) {
		Element scriptElement = htmlDoc.createElement("script");
		scriptElement.setAttribute("type", "text/javascript");
		scriptElement.setAttribute("src", "http://code.jquery.com/jquery-1.11.1.js");
		htmlBody.appendChild(scriptElement);
		Element treeScript = htmlDoc.createElement("script");
		treeScript.setAttribute("type", "text/javascript");
		treeScript.setTextContent("$(function () { " + "$('li > ul').each(function (i) {  "
				+ "var parentLi = $(this).parent('li');  " + "parentLi.addClass('folder'); "
				+ "var subUl = $(this).remove(); "
				+ "parentLi.wrapInner('<a style=\"color:red;cursor:pointer;display:block;\"/>').find('a').click(function () { "
				+ "subUl.toggle(); " + "}); " + "parentLi.append(subUl); " + "}); " + "});");
		htmlBody.appendChild(treeScript);
	}

	private static Element createHtmlListItemElement(Element xmlNode, Document htmlDoc) {
		Element liNode = htmlDoc.createElement("li");
		liNode.setAttribute("VIPSType", xmlNode.getTagName());
		liNode.setAttribute("style",
				"background-color:" + xmlNode.getAttribute("BgColor") + ";" + "font-size:"
						+ xmlNode.getAttribute("FontSize") + ";" + "font-weight:" + xmlNode.getAttribute("FontWeight")
						+ ";" + "border: 1px solid black;" + "padding: 10px");
		liNode.setAttribute("EvincedId", xmlNode.getAttribute("EvincedId"));
		liNode.setTextContent(xmlNode.getAttribute("ID") + ". EvincedIDs: " + xmlNode.getAttribute("EvincedId")
				+ "Content: " + xmlNode.getAttribute("Content"));
		return liNode;
	}

	private static void createHtmlElement(Element xmlNode, Element parentHtmlNode, Document htmlDoc) {
		Element liNode = createHtmlListItemElement(xmlNode, htmlDoc);
		parentHtmlNode.appendChild(liNode);
		if (xmlNode.hasChildNodes()) {
			Element ulNode = htmlDoc.createElement("ul");
			liNode.appendChild(ulNode);
			for (int i = 0; i < xmlNode.getChildNodes().getLength(); i++) {
				Node xmlChild = xmlNode.getChildNodes().item(i);
				if (xmlChild instanceof Element) {
					createHtmlElement((Element) xmlChild, ulNode, htmlDoc);
				}
			}
		}
	}

	public static Document xmlToHtml(Document xmlDoc) {
		Document htmlDoc = null;
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			htmlDoc = docBuilder.newDocument();
			Element htmlBody = initHtmlDoc(htmlDoc);
			createCollapsableTree(htmlDoc, htmlBody);
			Element ul = htmlDoc.createElement("ul");
			htmlBody.appendChild(ul);
			createHtmlElement(xmlDoc.getDocumentElement(), ul, htmlDoc);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return htmlDoc;
	}

	public static Document loadXmlDocumentFromFile(String fileName) {
		System.out.println("Load xml file from: " + fileName);
		File file = new File(fileName);
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = null;
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
			return documentBuilder.parse(file);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static void writeHtmlToFile(Element htmlDoc, String fileName) {
		try {
			DOMSource source = new DOMSource(htmlDoc);
			FileWriter writer = new FileWriter(new File(fileName));
			StreamResult result = new StreamResult(writer);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.transform(source, result);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static String getEvincedId(ElementBox elementBox) {
		if (elementBox.getElement().hasAttribute(("evinced_id"))) {
			return elementBox.getElement().getAttribute("evinced_id") + ";";
		}
		return elementBox.getParent().getElement().getAttribute("evinced_id") + ";";
	}

	private static void extractAllEvincedIds(Node node, Set<String> ids) {
		if (node instanceof Element) {
			Element element = (Element) node;
			if (element.hasAttribute("EvincedId")) {
				String[] evincedIds = element.getAttribute("EvincedId").split(";");
				for (String id : evincedIds) {
					ids.add(id);
				}
			}
		}

		if (node.hasChildNodes()) {
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				extractAllEvincedIds(node.getChildNodes().item(i), ids);
			}
		}
	}

	public static void writeAllEvincedIdsToFile(Document htmlDoc, String fileName) {
		Set<String> evincedIds = new HashSet<>();
		extractAllEvincedIds(htmlDoc.getDocumentElement(), evincedIds);
		try (PrintStream out = new PrintStream(new FileOutputStream(fileName))) {
			out.print(String.join(";", evincedIds));
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static void generateEvincedScript(Document htmlDoc, String fileName) {
		// Load script template from classpath resources
		try (InputStream is = Utils.class.getResourceAsStream("/evinced-mark-VIPS-blocks.js");
				PrintStream out = new PrintStream(new FileOutputStream(fileName))) {
			if (is == null) {
				throw new RuntimeException("Resource not found: /evinced-mark-VIPS-blocks.js");
			}
			Set<String> evincedIds = new HashSet<>();
			extractAllEvincedIds(htmlDoc.getDocumentElement(), evincedIds);
			String evincedIdsStr = String.join(";", evincedIds);
			String evincedScript = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			evincedScript = evincedScript.replace("<put-blocks-ids-here>", evincedIdsStr);
			out.print(evincedScript);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}