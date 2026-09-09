package com.bolivariano.microservice.recbanred.service;

import com.bolivariano.microservice.recbanred.core.constants.Defaults;
import com.bolivariano.microservice.recbanred.core.enums.TipoVersion;
import com.bolivariano.microservice.recbanred.core.exceptions.CustomException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static com.bolivariano.microservice.recbanred.core.constants.CodeDefaults.*;

@Service
public class MarshalService {

    private static final Logger log = LoggerFactory.getLogger(MarshalService.class);

    /**
     * Método genérico para marshallear cualquier peticion SOAP.
     *
     * @param objectValue - El objeto a transformar en peticion XML
     */
    public String marshallRequest(Object objectValue, TipoVersion versionType) throws CustomException {
        try {
            final JAXBContext context = JAXBContext.newInstance(objectValue.getClass());
            final Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            StringWriter stringWriter = new StringWriter();
            marshaller.marshal(objectValue, new StreamResult(stringWriter));
            return this.addSoapEnv(stringWriter.toString(), versionType);
        } catch (Exception e) {
            throw new CustomException("Error en transformacion JAVA a XML: " + e.getMessage(), e, TRANSFORMATION_ERROR);
        }
    }

    /**
     * Método genérico para unmarshallear cualquier respuesta SOAP.
     *
     * @param soapXml     Respuesta SOAP como String
     * @return Objeto unmarshalleado de tipo T
     * @throws CustomException Excepcion personalizada si existe un error
     */
    @SuppressWarnings("unchecked")
    public <T> T unmarshalResponse(String soapXml, Class<T> clazz) throws CustomException {
        String cleanXml = extractSoapBodyXml(soapXml);
        try {
            JAXBContext marshaller = JAXBContext.newInstance(clazz);

            XMLInputFactory xif = XMLInputFactory.newFactory();
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);

            StringReader reader = new StringReader(cleanXml);
            XMLStreamReader xsr = xif.createXMLStreamReader(reader);

            Unmarshaller unmarshaller = marshaller.createUnmarshaller();
            return (T) unmarshaller.unmarshal(xsr);
        } catch (JAXBException | XMLStreamException e) {
            throw new CustomException("Error en transformacion XML a Objeto JAVA: " + e.getMessage(), e, TRANSFORMATION_ERROR);
        }
    }

    /**
     * Agrega el soapEnvelope de la peticion
     *
     * @param value - el valor del request del DTO
     * @return XML con la configuracion agregando el soapEnvelope
     */
    private String addSoapEnv(String value, TipoVersion versionType) {
        return switch (versionType) {
            case V2 -> "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                    + "<soapenv:Header/>"
                    + "<soapenv:Body>"
                    + value
                    + "</soapenv:Body>"
                    + "</soapenv:Envelope>";
            case V1 -> "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">"
                    + "<soap:Header/>"
                    + "<soap:Body>"
                    + value
                    + "</soap:Body>"
                    + "</soap:Envelope>";
            default -> {
                log.error("VERSION NO SOPORTADA");
                yield Defaults.EMPTY;
            }
        };
    }

    /**
     * Elimina el soapEnvelope y soapBody de una respuesta SOAP.
     *
     * @param soapXml XML SOAP completo
     * @return XML limpio sin Envelope y sin Body
     * @throws CustomException Excepcion personalizada si hay un problema en la manipulación del XML
     */
    private String extractSoapBodyXml(String soapXml) throws CustomException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            factory.setFeature("http://apache.org/xml/features/validation/schema", false);
            factory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", false);

            factory.setFeature("http://apache.org/xml/features/xinclude", false);

            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(soapXml.getBytes(StandardCharsets.UTF_8)));

            NodeList bodyNodes = document.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Body");
            if (bodyNodes.getLength() == 0) {
                throw new IllegalArgumentException("El XML no contiene un Body válido.");
            }

            Node bodyNode = bodyNodes.item(0);
            for (int i = 0; i < bodyNode.getChildNodes().getLength(); i++) {
                Node child = bodyNode.getChildNodes().item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    return nodeToString(child);
                }
            }
            throw new CustomException("Nodo vacío <soapenv:Body>", null, TRANSFORMATION_ERROR);
        } catch (Exception ex) {
            throw new CustomException("Error al procesar SOAP de respuesta: " + ex.getMessage(), ex, TRANSFORMATION_ERROR);
        }
    }

    private String nodeToString(Node node) throws CustomException {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer;
            StringWriter writer = new StringWriter();
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new CustomException("Error transformando el response: " + e.getMessage(), e, TRANSFORMATION_ERROR);
        }
    }

    public String extractResultCodeOrFaultString(String xml) throws CustomException {
        String result = extractTagValue(xml, "ResultCode");
        if (result != null) return result;

        result = extractTagValue(xml, "faultstring");
        if (result != null) return result;

        throw new CustomException("No se encontró información en la respuesta: nodo vacío", null, TRANSFORMATION_ERROR);
    }

    public String extractErrorMessage(String xml) throws CustomException {
        String error = extractTagValue(xml, "ErrorMessage");
        if (error != null) return error;

        throw new CustomException("No se encontró información en la respuesta: nodo vacío", null, TRANSFORMATION_ERROR);
    }

    private String extractTagValue(String xml, String tagName) throws CustomException {
        try {
            Document doc = createSecureDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList nodeList = doc.getElementsByTagNameNS("*", tagName);
            if (nodeList.getLength() > 0) {
                return nodeList.item(0).getTextContent();
            }
            return null;
        } catch (Exception e) {
            throw new CustomException("Error extrayendo nodo " + tagName + ": " + e.getMessage(), e, TRANSFORMATION_ERROR);
        }
    }

    private DocumentBuilder createSecureDocumentBuilder() throws CustomException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/validation/schema", false);
            factory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", false);
            factory.setFeature("http://apache.org/xml/features/xinclude", false);

            return factory.newDocumentBuilder();
        } catch (Exception e) {
            throw new CustomException("Error en la configuracion del DocumentBuilder para la transformacion de la data XML", e, TRANSFORMATION_ERROR);
        }

    }
}