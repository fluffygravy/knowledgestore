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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "mark"
})
@XmlRootElement(name = "markables")
public class Markables {

    @XmlElement(required = true)
    protected List<Mark> mark;

    /**
     * Gets the value of the mark property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the mark property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMark().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Mark }
     * 
     * 
     */
    public List<Mark> getMark() {
        if (mark == null) {
            mark = new ArrayList<Mark>();
        }
        return this.mark;
    }

}