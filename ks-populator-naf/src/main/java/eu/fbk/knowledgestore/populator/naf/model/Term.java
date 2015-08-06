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
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "sentimentOrSpanOrExternalReferencesOrComponent"
})
@XmlRootElement(name = "term")
public class Term {

    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    protected String id;
    @XmlAttribute(name = "type")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String type;
    @XmlAttribute(name = "lemma")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String lemma;
    @XmlAttribute(name = "pos")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String pos;
    @XmlAttribute(name = "morphofeat")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String morphofeat;
    @XmlAttribute(name = "netype")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String netype;
    @XmlAttribute(name = "case")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String _case;
    @XmlAttribute(name = "head")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String head;
    @XmlElements({
        @XmlElement(name = "sentiment", required = true, type = Sentiment.class),
        @XmlElement(name = "span", required = true, type = Span.class),
        @XmlElement(name = "externalReferences", required = true, type = ExternalReferences.class),
        @XmlElement(name = "component", required = true, type = Component.class)
    })
    protected List<Object> sentimentOrSpanOrExternalReferencesOrComponent;

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the lemma property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLemma() {
        return lemma;
    }

    /**
     * Sets the value of the lemma property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLemma(String value) {
        this.lemma = value;
    }

    /**
     * Gets the value of the pos property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPos() {
        return pos;
    }

    /**
     * Sets the value of the pos property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPos(String value) {
        this.pos = value;
    }

    /**
     * Gets the value of the morphofeat property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMorphofeat() {
        return morphofeat;
    }

    /**
     * Sets the value of the morphofeat property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMorphofeat(String value) {
        this.morphofeat = value;
    }

    /**
     * Gets the value of the netype property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNetype() {
        return netype;
    }

    /**
     * Sets the value of the netype property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNetype(String value) {
        this.netype = value;
    }

    /**
     * Gets the value of the case property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCase() {
        return _case;
    }

    /**
     * Sets the value of the case property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCase(String value) {
        this._case = value;
    }

    /**
     * Gets the value of the head property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHead() {
        return head;
    }

    /**
     * Sets the value of the head property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHead(String value) {
        this.head = value;
    }

    /**
     * Gets the value of the sentimentOrSpanOrExternalReferencesOrComponent property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sentimentOrSpanOrExternalReferencesOrComponent property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSentimentOrSpanOrExternalReferencesOrComponent().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Sentiment }
     * {@link Span }
     * {@link ExternalReferences }
     * {@link Component }
     * 
     * 
     */
    public List<Object> getSentimentOrSpanOrExternalReferencesOrComponent() {
        if (sentimentOrSpanOrExternalReferencesOrComponent == null) {
            sentimentOrSpanOrExternalReferencesOrComponent = new ArrayList<Object>();
        }
        return this.sentimentOrSpanOrExternalReferencesOrComponent;
    }

}
