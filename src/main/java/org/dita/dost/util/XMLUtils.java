/*
 * This file is part of the DITA Open Toolkit project.
 * See the accompanying license.txt file for applicable licenses.
 */
package org.dita.dost.util;

import static javax.xml.XMLConstants.NULL_NS_URI;

import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * XML utility methods.
 * 
 * @since 1.5.4
 * @author Jarno Elovirta
 */
public final class XMLUtils {

    /** Private constructor to make class uninstantiable. */
    private XMLUtils() {}

    /**
     * Add or set attribute.
     * 
     * @param atts attributes
     * @param uri namespace URI
     * @param localName local name
     * @param qName qualified name
     * @param type attribute type
     * @param value attribute value
     */
    public static void addOrSetAttribute(final AttributesImpl atts, final String uri, final String localName,
            final String qName, final String type, final String value) {
        final int i = atts.getIndex(qName);
        if (i != -1) {
            atts.setAttribute(i, uri, localName, qName, type, value);
        } else {
            atts.addAttribute(uri, localName, qName, type, value);
        }
    }

    /**
     * Add or set attribute. Convenience method for {@link #addOrSetAttribute(AttributesImpl, String, String, String, String, String)}.
     * 
     * @param atts attributes
     * @param localName local name
     * @param value attribute value
     */
    public static void addOrSetAttribute(final AttributesImpl atts, final String localName, final String value) {
        addOrSetAttribute(atts, NULL_NS_URI, localName, localName, "CDATA", value);
    }

    /**
     * Add or set attribute. Convenience method for {@link #addOrSetAttribute(AttributesImpl, String, String, String, String, String)}.
     * 
     * @param atts attributes
     * @param att attribute node
     */
    public static void addOrSetAttribute(final AttributesImpl atts, final Node att) {
        if (att.getNodeType() != Node.ATTRIBUTE_NODE) {
            throw new IllegalArgumentException();
        }
        final Attr a = (Attr) att;
        String localName = a.getLocalName();
        if (localName == null) {
            localName = a.getName();
            final int i = localName.indexOf(':');
            if (i != -1) {
                localName = localName.substring(i + 1);
            }
        }
        addOrSetAttribute(atts,
                a.getNamespaceURI() != null ? a.getNamespaceURI() : NULL_NS_URI,
                        localName,
                                a.getName() != null ? a.getName() : localName,
                                        a.isId() ? "ID" : "CDATA",
                                                a.getValue());
    }

    /**
     * Remove an attribute from the list. Do nothing if attribute does not exist.
     * 
     * @param atts attributes
     * @param qName QName of the attribute to remove
     */
    public static void removeAttribute(final AttributesImpl atts, final String qName) {
        final int i = atts.getIndex(qName);
        if (i != -1) {
            atts.removeAttribute(i);
        }
    }
    
    /**
     * Get element node string value.
     * 
     * @param element element to get string value for
     * @return concatenated text node descendant values
     */
    public static String getStringValue(final Element element) {
        final StringBuilder buf = new StringBuilder();
        final NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node n = children.item(i);
            switch (n.getNodeType()) {
            case Node.TEXT_NODE:
                buf.append(n.getNodeValue());
                break;
            case Node.ELEMENT_NODE:
                buf.append(getStringValue((Element) n));
                break;
            }
        }
        return buf.toString();
    }

    /**
     * Convenience builder for {@link org.xml.sax.Attributes SAX Attributes}.
     */
    public static final class AttributesBuilder {
    	
    	final AttributesImpl atts;
    	
    	/**
    	 * Construct empty attributes builder.
    	 */
    	public AttributesBuilder() {
    		atts = new AttributesImpl();
    	}
    	
    	/**
    	 * Construct attributes builder with initial attribute set.
    	 * 
    	 * @param atts initial attributes
    	 */
    	public AttributesBuilder(final Attributes atts) {
    		this.atts = new AttributesImpl(atts);
    	}
    	
        /**
         * Add or set attribute.
         * 
         * @param uri namespace URI
         * @param localName local name
         * @param qName qualified name
         * @param type attribute type
         * @param value attribute value
         * @return this builder
         */
        public AttributesBuilder add(final String uri, final String localName,
                final String qName, final String type, final String value) {
            final int i = atts.getIndex(qName);
            if (i != -1) {
                atts.setAttribute(i, uri, localName, qName, type, value);
            } else {
                atts.addAttribute(uri, localName, qName, type, value);
            }
            return this;
        }
        
        /**
         * Add or set attribute. Convenience method for {@link #add(AttributesImpl, String, String, String, String, String)}.
         * 
         * @param localName local name
         * @param value attribute value
         * @return this builder
         */
        public AttributesBuilder add(final String localName, final String value) {
            return add(NULL_NS_URI, localName, localName, "CDATA", value);
        }
    	
        /**
         * Add or set attribute. Convenience method for {@link #add(AttributesImpl, String, String, String, String, String)}.
         * 
         * @param uri namespace URI
         * @param localName local name
         * @param value attribute value
         * @return this builder
         */
        public AttributesBuilder add(final String uri, final String localName, final String value) {
            return add(uri, localName, localName, "CDATA", value);
        }
        
        /**
         * Returns a newly-created Attributes based on the contents of the builder.
         * @return new attributes
         */
    	public Attributes build() {
    		return new AttributesImpl(atts);
    	}
    	
    }
    
}
