//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.06.27 at 01:16:00 PM WEST 
//

package com.lyncode.xoai.dataprovider.xml.xoai;

import com.lyncode.xoai.dataprovider.xml.XMLWritable;
import com.lyncode.xoai.dataprovider.exceptions.WritingXmlException;
import com.lyncode.xoai.dataprovider.xml.XmlOutputContext;

import javax.xml.bind.annotation.*;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.List;

import static com.lyncode.xoai.util.XmlIOUtils.writeElement;

/**
 * <p/>
 * Java class for Element complex type.
 * <p/>
 * <p/>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * <p/>
 * <pre>
 * &lt;complexType name="Element">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence minOccurs="0">
 *         &lt;element name="field" maxOccurs="unbounded" minOccurs="0">
 *           &lt;complexType>
 *             &lt;simpleContent>
 *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *                 &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/extension>
 *             &lt;/simpleContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="element" type="{http://www.lyncode.com/xoai}Element" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Element", propOrder = {"field", "element"})
public class Element implements XMLWritable {

    protected List<Element.Field> field;
    protected List<Element> element;
    @XmlAttribute(required = true)
    protected String name;

    /**
     * Gets the value of the field property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list, not a
     * snapshot. Therefore any modification you make to the returned list will
     * be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the field property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <p/>
     * <pre>
     * getField().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link Element.Field }
     */
    public List<Element.Field> getField() {
        if (field == null) {
            field = new ArrayList<Element.Field>();
        }
        return this.field;
    }

    /**
     * Gets the value of the element property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list, not a
     * snapshot. Therefore any modification you make to the returned list will
     * be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the element property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <p/>
     * <pre>
     * getElement().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list {@link Element }
     */
    public List<Element> getElement() {
        if (element == null) {
            element = new ArrayList<Element>();
        }
        return this.element;
    }

    /**
     * Gets the value of the name property.
     *
     * @return possible object is {@link String }
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is {@link String }
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * <p/>
     * Java class for anonymous complex type.
     * <p/>
     * <p/>
     * The following schema fragment specifies the expected content contained
     * within this class.
     * <p/>
     * <pre>
     * &lt;complexType>
     *   &lt;simpleContent>
     *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
     *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/extension>
     *   &lt;/simpleContent>
     * &lt;/complexType>
     * </pre>
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"value"})
    public static class Field implements XMLWritable {

        @XmlValue
        protected String value;
        @XmlAttribute
        protected String name;

        /**
         * Gets the value of the value property.
         *
         * @return possible object is {@link String }
         */
        public String getValue() {
            return value;
        }

        /**
         * Sets the value of the value property.
         *
         * @param value allowed object is {@link String }
         */
        public void setValue(String value) {
            this.value = value;
        }

        /**
         * Gets the value of the name property.
         *
         * @return possible object is {@link String }
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the value of the name property.
         *
         * @param value allowed object is {@link String }
         */
        public void setName(String value) {
            this.name = value;
        }


        @Override
        public void write(XmlOutputContext writer) throws WritingXmlException {
            try {
                if (this.name != null)
                    writer.getWriter().writeAttribute("name", this.getName());

                if (this.value != null)
                    writer.getWriter().writeCharacters(value);

            } catch (XMLStreamException e) {
                throw new WritingXmlException(e);
            }
        }

    }

    @Override
    public void write(XmlOutputContext context) throws WritingXmlException {
        try {
            if (this.name != null)
                context.getWriter().writeAttribute("name", this.getName());

            for (Field f : this.getField())
                writeElement(context, "field", f);

            for (Element e : this.getElement())
                writeElement(context, "element", e);

        } catch (XMLStreamException e) {
            throw new WritingXmlException(e);
        }

    }

}
