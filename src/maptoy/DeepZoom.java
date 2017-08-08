package maptoy;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.net.URL;

public class DeepZoom {

    @XmlRootElement(name = "Image", namespace = "http://schemas.microsoft.com/deepzoom/2008")
    static class DziImage {
        @XmlElement(name="Size", namespace = "http://schemas.microsoft.com/deepzoom/2008")
        public DziSize size;


        private static JAXBContext jaxb;
        static {
            try {
                jaxb = JAXBContext.newInstance(DziImage.class);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }

        static DziImage get(String objId) throws IOException {
            URL url = new URL("http://nla.gov.au/" + objId + "/dzi");
            try {
                return (DziImage) jaxb.createUnmarshaller().unmarshal(url);
            } catch (JAXBException e) {
                throw new IOException(e);
            }
        }
    }

    static class DziSize {
        @XmlAttribute(name="Width")
        public int width;

        @XmlAttribute(name="Height")
        public int height;
    }
}
