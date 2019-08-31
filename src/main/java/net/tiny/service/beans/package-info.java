@XmlSchema(
	    namespace = "http://xmlns.jcp.org/xml/ns/javaee",
	    xmlns = {
	    	@XmlNs(prefix = "",       namespaceURI = "http://xmlns.jcp.org/xml/ns/javaee"),
	    	@XmlNs(prefix = "xsi",    namespaceURI = "http://www.w3.org/2001/XMLSchema-instance")
	    },
	    elementFormDefault = XmlNsForm.QUALIFIED
	)
package net.tiny.service.beans;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
