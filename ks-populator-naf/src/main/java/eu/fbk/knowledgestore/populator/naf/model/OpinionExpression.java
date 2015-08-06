//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.08.03 at 06:08:32 PM CEST 
//


package eu.fbk.knowledgestore.populator.naf.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "span"
})
@XmlRootElement(name = "opinion_expression")
public class OpinionExpression {

    @XmlAttribute(name = "polarity")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String polarity;
    @XmlAttribute(name = "strength")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String strength;
    @XmlAttribute(name = "subjectivity")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String subjectivity;
    @XmlAttribute(name = "sentiment_semantic_type")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String sentimentSemanticType;
    @XmlAttribute(name = "sentiment_product_feature")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String sentimentProductFeature;
    @XmlElement(required = true)
    protected List<Span> span;

    /**
     * Gets the value of the polarity property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPolarity() {
        return polarity;
    }

    /**
     * Sets the value of the polarity property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPolarity(String value) {
        this.polarity = value;
    }

    /**
     * Gets the value of the strength property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStrength() {
        return strength;
    }

    /**
     * Sets the value of the strength property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStrength(String value) {
        this.strength = value;
    }

    /**
     * Gets the value of the subjectivity property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSubjectivity() {
        return subjectivity;
    }

    /**
     * Sets the value of the subjectivity property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSubjectivity(String value) {
        this.subjectivity = value;
    }

    /**
     * Gets the value of the sentimentSemanticType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSentimentSemanticType() {
        return sentimentSemanticType;
    }

    /**
     * Sets the value of the sentimentSemanticType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSentimentSemanticType(String value) {
        this.sentimentSemanticType = value;
    }

    /**
     * Gets the value of the sentimentProductFeature property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSentimentProductFeature() {
        return sentimentProductFeature;
    }

    /**
     * Sets the value of the sentimentProductFeature property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSentimentProductFeature(String value) {
        this.sentimentProductFeature = value;
    }

    /**
     * Gets the value of the span property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the span property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSpan().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Span }
     * 
     * 
     */
    public List<Span> getSpan() {
        if (span == null) {
            span = new ArrayList<Span>();
        }
        return this.span;
    }

}
