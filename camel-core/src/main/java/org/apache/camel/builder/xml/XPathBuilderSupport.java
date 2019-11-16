/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.builder.xml;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XML XPathBuilder  support.
 */
public class XPathBuilderSupport {

    private static final ErrorHandler DOCUMENT_BUILDER_LOGGING_ERROR_HANDLER = new DocumentBuilderLoggingErrorHandler();
    private static final String DOCUMENT_BUILDER_FACTORY_FEATURE = "org.apache.camel.xmlconverter.documentBuilderFactory.feature";

    private static final Logger LOG = LoggerFactory.getLogger(XPathBuilderSupport.class);

    private volatile DocumentBuilderFactory documentBuilderFactory;


    public DocumentBuilderFactory createDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);
        try {
            // Set secure processing
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        } catch (ParserConfigurationException e) {
            LOG.warn("DocumentBuilderFactory doesn't support the feature {} with value {}, due to {}.", new Object[] {XMLConstants.FEATURE_SECURE_PROCESSING, true, e});
        }
        try {
            // Disable the external-general-entities by default
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        } catch (ParserConfigurationException e) {
            LOG.warn("DocumentBuilderFactory doesn't support the feature {} with value {}, due to {}.",
                    new Object[] {"http://xml.org/sax/features/external-general-entities", false, e});
        }
        // setup the SecurityManager by default if it's apache xerces
        try {
            Class<?> smClass = ObjectHelper.loadClass("org.apache.xerces.util.SecurityManager");
            if (smClass != null) {
                Object sm = smClass.getDeclaredConstructor().newInstance();
                // Here we just use the default setting of the SeurityManager
                factory.setAttribute("http://apache.org/xml/properties/security-manager", sm);
            }
        } catch (Exception e) {
            LOG.warn("DocumentBuilderFactory doesn't support the attribute {}, due to {}.", new Object[] {"http://apache.org/xml/properties/security-manager", e});
        }
        // setup the feature from the system property
        setupFeatures(factory);
        return factory;
    }

    protected void setupFeatures(DocumentBuilderFactory factory) {
        Properties properties = System.getProperties();
        List<String> features = new ArrayList<>();
        for (Map.Entry<Object, Object> prop : properties.entrySet()) {
            String key = (String)prop.getKey();
            if (key.startsWith(DOCUMENT_BUILDER_FACTORY_FEATURE)) {
                String uri = StringHelper.after(key, ":");
                Boolean value = Boolean.valueOf((String)prop.getValue());
                try {
                    factory.setFeature(uri, value);
                    features.add("feature " + uri + " value " + value);
                } catch (ParserConfigurationException e) {
                    LOG.warn("DocumentBuilderFactory doesn't support the feature {} with value {}, due to {}.", uri, value, e);
                }
            }
        }
        if (features.size() > 0) {
            StringBuilder featureString = new StringBuilder();
            // just log the configured feature
            for (String feature : features) {
                if (featureString.length() != 0) {
                    featureString.append(", ");
                }
                featureString.append(feature);
            }
            LOG.info("DocumentBuilderFactory has been set with features {{}}.", featureString);
        }

    }

    public Document createDocument() throws ParserConfigurationException {
        DocumentBuilder builder = createDocumentBuilder();
        return builder.newDocument();
    }

    public DocumentBuilder createDocumentBuilder(DocumentBuilderFactory factory) throws ParserConfigurationException {
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(DOCUMENT_BUILDER_LOGGING_ERROR_HANDLER);
        return builder;
    }

    public DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        return createDocumentBuilder(getDocumentBuilderFactory());
    }

    public DocumentBuilderFactory getDocumentBuilderFactory() {
        if (documentBuilderFactory == null) {
            documentBuilderFactory = createDocumentBuilderFactory();
        }
        return documentBuilderFactory;
    }


    private static class DocumentBuilderLoggingErrorHandler implements ErrorHandler {

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            LOG.warn(exception.getMessage(), exception);
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            LOG.error(exception.getMessage(), exception);
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            LOG.error(exception.getMessage(), exception);
        }
    }
}
