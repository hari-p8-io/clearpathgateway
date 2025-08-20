package com.anz.fastpayment.router.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Component
public class XmlSchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(XmlSchemaValidator.class);

    public void validate(String xmlContent, String messageType) {
        String schemaPath = resolveSchemaPath(messageType);
        if (schemaPath == null) {
            throw new IllegalArgumentException("Unsupported message type for XSD validation: " + messageType);
        }
        try {
            log.debug("[XSD] Loading schema from classpath:{}", schemaPath);
            ClassPathResource schemaResource = new ClassPathResource(schemaPath);
            try (InputStream is = schemaResource.getInputStream()) {
                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = factory.newSchema(new StreamSource(is));
                Validator validator = schema.newValidator();
                log.debug("[XSD] Running validator for messageType={}", messageType);
                validator.validate(new StreamSource(new ByteArrayInputStream(xmlContent.getBytes())));
            }
        } catch (Exception ex) {
            log.warn("XSD validation failed for type {}: {}", messageType, ex.getMessage());
            throw new IllegalArgumentException("XML does not conform to XSD for type " + messageType, ex);
        }
    }

    private String resolveSchemaPath(String messageType) {
        return switch (messageType) {
            case "pacs.008.001.13" -> "schema/pacs.008.001.13.xsd";
            case "pacs.003.001.11" -> "schema/pacs.003.001.11.xsd";
            case "pacs.007.001.13" -> "schema/pacs.007.001.13.xsd";
            case "camt.056.001.11" -> "schema/camt.056.001.11.xsd";
            default -> null;
        };
    }
}


