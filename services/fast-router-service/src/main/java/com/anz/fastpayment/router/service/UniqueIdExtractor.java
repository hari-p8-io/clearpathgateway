package com.anz.fastpayment.router.service;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Component
public class UniqueIdExtractor {

    public String extractUniqueId(String xml, String messageType) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            if ("pacs.008.001.13".equals(messageType)) {
                String ns = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.13";
                String e2e = text(doc, ns, "EndToEndId");
                if (notBlank(e2e)) return e2e;
                String instr = text(doc, ns, "InstrId");
                if (notBlank(instr)) return instr;
                String tx = text(doc, ns, "TxId");
                if (notBlank(tx)) return tx;
            }
            if ("pacs.002".equalsIgnoreCase(messageType)) {
                String ns = "urn:iso:std:iso:20022:tech:xsd:pacs.002.001.**"; // best-effort if needed later
                String e2e = text(doc, ns, "OrgnlEndToEndId");
                if (notBlank(e2e)) return e2e;
                String tx = text(doc, ns, "OrgnlTxId");
                if (notBlank(tx)) return tx;
                String mid = text(doc, ns, "OrgnlMsgId");
                if (notBlank(mid)) return mid;
            }
        } catch (Exception ignore) { }
        // Always non-null: fallback to PUID must be supplied by caller, so return empty to signal fallback
        return "";
    }

    private String text(Document doc, String ns, String local) {
        if (ns == null) {
            NodeList n = doc.getElementsByTagName(local);
            return n.getLength() > 0 ? n.item(0).getTextContent() : null;
        }
        NodeList nodes = doc.getElementsByTagNameNS(ns, local);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
}


