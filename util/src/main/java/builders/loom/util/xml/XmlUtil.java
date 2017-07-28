package builders.loom.util.xml;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class XmlUtil {

    private XmlUtil() {
    }

    public static Element getOnlyElement(final NodeList nodes) {
        if (nodes.getLength() == 1) {
            return (Element) nodes.item(0);
        }
        throw new IllegalArgumentException("Expected one element, but got " + nodes.getLength());
    }

}
