package builders.loom.util.xml;

import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

public class XmlBuilder {

    private final Document document;

    private XmlBuilder(final Document document) {
        this.document = document;
    }

    public static Element root(final String rootElementName) {
        Objects.requireNonNull(rootElementName, "rootElementName required");

        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder;
        try {
            documentBuilder = dbFactory.newDocumentBuilder();
        } catch (final ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }

        final XmlBuilder xmlBuilder = new XmlBuilder(documentBuilder.newDocument());
        return new Element(xmlBuilder, null, rootElementName);
    }

    Document getDocument() {
        return document;
    }

    public static class Element {

        private final XmlBuilder xmlBuilder;
        private final Element parent;
        private final org.w3c.dom.Element wrappedElement;

        Element(final XmlBuilder xmlBuilder, final Element parent, final String elementName) {
            this.xmlBuilder = xmlBuilder;
            this.parent = parent;
            this.wrappedElement = xmlBuilder.getDocument().createElement(elementName);
            if (parent != null) {
                parent.wrappedElement.appendChild(wrappedElement);
            } else {
                getDocument().appendChild(wrappedElement);
            }
        }

        public Element attr(final String name, final String value) {
            Objects.requireNonNull(name, "name required");
            Objects.requireNonNull(value, "value required");
            wrappedElement.setAttribute(name, value);
            return this;
        }

        public Element element(final String elementName) {
            Objects.requireNonNull(elementName, "elementName required");

            return new Element(xmlBuilder, this, elementName);
        }

        public Element and() {
            return parent;
        }

        public Document getDocument() {
            return xmlBuilder.getDocument();
        }

    }

}