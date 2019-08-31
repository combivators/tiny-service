package net.tiny.service.beans;

import java.io.File;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "beans")
public class BeansXml {
	public static final String BEANS_XML = "META-INF/beans.xml";
	public static enum BeanDiscoveryMode {
		none, annotated, all;
	}

	public static class Alternatives {
	    @XmlElement(name = "class")
	    public List<String> classes = null;
	    @XmlElement(name = "stereotype")
	    public List<String> stereotypes = null;
	}

	public static class Scanning {
	    @XmlAttribute
	    public String name;
	    public Scanning(){}
	    public Scanning(String name){ this.name = name;}
	}

	static final String SCHEMA_LOCATION = "http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/beans_1_1.xsd";

	@XmlAttribute(namespace = "http://www.w3.org/2001/XMLSchema-instance")
	@XmlSchemaType(name = "schemaLocation")
	private String schemaLocation = SCHEMA_LOCATION;

	@XmlAttribute(name = "version")
	private String version;

	@XmlAttribute(name = "bean-discovery-mode")
	private BeanDiscoveryMode beanDiscoveryMode = BeanDiscoveryMode.annotated;

    @XmlElementWrapper(name = "interceptors")
    @XmlElement(name = "class")
	private List<String> interceptors = null;

    @XmlElementWrapper(name = "decorators")
    @XmlElement(name = "class")
	private List<String> decorators = null;

    @XmlElement(name = "alternatives")
	private Alternatives alternatives = null;

    @XmlElementWrapper(name = "scan")
    @XmlElement(name = "exclude")
	private List<Scanning> scanning = null;

	public List<String> getInterceptors() {
		return interceptors;
	}
	public void setInterceptors(List<String> interceptors) {
		this.interceptors = interceptors;
	}
	public List<String> getDecorators() {
		return decorators;
	}
	public void setDecorators(List<String> decorators) {
		this.decorators = decorators;
	}
	public Alternatives getAlternatives() {
		return alternatives;
	}
	public void setAlternatives(Alternatives alternatives) {
		this.alternatives = alternatives;
	}
	public List<Scanning> getScanning() {
		return scanning;
	}
	public void setScanning(List<Scanning> scanning) {
		this.scanning = scanning;
	}
	public String getSchemaLocation() {
		return schemaLocation;
	}
	public void setSchemaLocation(String schemaLocation) {
		this.schemaLocation = schemaLocation;
	}
	public BeanDiscoveryMode getBeanDiscoveryMode() {
		return beanDiscoveryMode;
	}
	public boolean isAnnotatedDiscoveryMode() {
		return BeanDiscoveryMode.annotated.equals(beanDiscoveryMode);
	}
	public boolean isAllDiscoveryMode() {
		return BeanDiscoveryMode.all.equals(beanDiscoveryMode);
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}

	public List<Pattern> getPatterns() {
		List<Pattern> patterns = new ArrayList<>();
		if(null != scanning && !scanning.isEmpty()) {
			for(BeansXml.Scanning scan : scanning) {
				String exclude = scan.name;
				exclude = exclude.replaceAll("[.]", "[.]");
				exclude = exclude.replaceAll("[*][*]", ".*");
				Pattern pattern = Pattern.compile(exclude);
				patterns.add(pattern);
			}
		}

		return patterns;
	}

	public String toString() {
		StringWriter writer = new StringWriter();
		try {
			JAXBContext jc = JAXBContext.newInstance(BeansXml.class);
	        Marshaller marshaller = jc.createMarshaller();
	        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	        marshaller.marshal(this, writer);
			return writer.toString();
		} catch (JAXBException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}

	public static BeansXml valueOf(String resource) {
		URL url = null;
		try {
			File file = new File(resource);
			if (file.exists()) {
				url = file.toURI().toURL();
			} else {
				url = new URL(resource);
			}
		} catch (MalformedURLException ex) {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			url = loader.getResource(resource);
		}
		if (url != null) {
			try {
				Class<BeansXml> requiredType = BeansXml.class;
				JAXBContext context = JAXBContext.newInstance(requiredType);
				Unmarshaller unmarshaller = context.createUnmarshaller();
		        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		        Document doc = documentBuilder.parse( url.openStream() );
				BeansXml beansXml = requiredType.cast(unmarshaller.unmarshal(doc));
				return beansXml;
			} catch (Exception ex) {
				throw new IllegalArgumentException(String.format("Not found '%1$s'", resource));
			}
		} else {
			throw new IllegalArgumentException(String.format("Not found '%1$s'", resource));
		}
	}
}
