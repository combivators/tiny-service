package net.tiny.ws.xml;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "endpoints")
public class Endpoints {

    static final String REGEX = "^[{][\\S][^}]+[}][\\S][^}]+";

    @XmlAccessorType(XmlAccessType.NONE)
    public static class Endpoint {
        @XmlAttribute(name = "name")
        public String name;

        @XmlAttribute(name = "service")
        public String service;

        @XmlAttribute(name = "port")
        public String port;

        @XmlAttribute(name = "implementation")
        public String implementation;

        @XmlAttribute(name = "url-pattern")
        public String urlPattern;

        @XmlAttribute(name = "wsdl")
        public String wsdl;

        private String servletName;
        private Class<?> interfaceClass;
        private byte[] contents;

        public String getServletName() {
            return servletName;
        }

        public void setServletName(String servletName) {
            this.servletName = servletName;
        }

        public byte[] getContents() {
            return contents;
        }

        public void setContents(byte[] contents) {
            this.contents = contents;
        }

        public Class<?> getInterfaceClass() {
            return interfaceClass;
        }

        public void setInterfaceClass(Class<?> classType) {
            this.interfaceClass = classType;
        }

        public void setInterfaceClass(String className) throws ClassNotFoundException {
            this.interfaceClass = Class.forName(className);
        }

        public String getTargetNamespace() {
            if(service != null && Pattern.matches(REGEX, service)) {
                int offset = service.lastIndexOf("}");
                return service.substring(1, offset);
            } else if(port != null && Pattern.matches(REGEX, port)) {
                int offset = port.lastIndexOf("}");
                return port.substring(1, offset);
            } else {
                return "";
            }
        }

        public String getSimpleService() {
            if(service != null && Pattern.matches(REGEX, service)) {
                int offset = service.lastIndexOf("}");
                return service.substring(offset+1);
            } else {
                return service;
            }
        }

        public String getSimplePort() {
            if(port != null && Pattern.matches(REGEX, port)) {
                int offset = port.lastIndexOf("}");
                return port.substring(offset+1);
            } else {
                return port;
            }
        }

        @Override
        public int hashCode() {
            StringBuffer sb = new StringBuffer();
            sb.append(name);
            sb.append(service);
            sb.append(port);
            sb.append(implementation);
            sb.append(urlPattern);
            sb.append(wsdl);
            return sb.toString().hashCode();
        }

        @Override
        public String toString() {
            return String.format("name='%s', service='%s', port='%s', impl='%s', url='%s', wsdl='%s'",
                    name, service, port, implementation, urlPattern, wsdl);
        }
    }

    @XmlElement(name = "endpoint")
    private List<Endpoint> list;

    @XmlAttribute(name = "version")
    private String version = "2.0";

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<Endpoint> getEndpoints() {
        return list;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.list = endpoints;
    }

    public void addEndpoint(Endpoint endpoint) {
        if(this.list == null) {
            this.list = new LinkedList<Endpoint>();
        }
        if(!this.list.contains(endpoint)) {
            this.list.add(endpoint);
        }
    }

    public Endpoints.Endpoint getEndpoint(String servletName) {
        if(this.list != null && !this.list.isEmpty()) {
            for(Endpoint ep : this.list) {
                if(servletName.equals(ep.getServletName())) {
                    return ep;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("endpoints = %s", list);
    }

}