/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLFilterImpl;

import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArchetypeCatalogFactory;
import org.maven.ide.eclipse.embedder.ArchetypeCatalogFactory.LocalCatalogFactory;
import org.maven.ide.eclipse.embedder.ArchetypeCatalogFactory.RemoteCatalogFactory;


/**
 * Archetype catalogs writer
 * 
 * @author Eugene Kuleshov
 */
public class ArchetypeCatalogsWriter {

  private static final String ELEMENT_CATALOGS = "archetypeCatalogs";

  private static final String ELEMENT_CATALOG = "catalog";

  private static final String ATT_CATALOG_TYPE = "type";

  private static final String ATT_CATALOG_LOCATION = "location";
  
  public static final String ATT_CATALOG_DESCRIPTION = "description";
  
  private static final String TYPE_LOCAL = "local";

  private static final String TYPE_REMOTE = "remote";

  
  public Collection<ArchetypeCatalogFactory> readArchetypeCatalogs(InputStream is) throws IOException {
    Collection<ArchetypeCatalogFactory> catalogs = new ArrayList<ArchetypeCatalogFactory>();
    try {
      SAXParserFactory parserFactory = SAXParserFactory.newInstance();
      SAXParser parser = parserFactory.newSAXParser();
      parser.parse(is, new ArchetypeCatalogsContentHandler(catalogs));
    } catch(SAXException ex) {
      String msg = "Unable to parse Archetype catalogs list";
      MavenLogger.log(msg, ex);
      throw new IOException(msg + "; " + ex.getMessage());
    } catch(ParserConfigurationException ex) {
      String msg = "Unable to parse Archetype catalogs list";
      MavenLogger.log(msg, ex);
      throw new IOException(msg + "; " + ex.getMessage());
    }
    return catalogs;
  }

  public void writeArchetypeCatalogs(final Collection<ArchetypeCatalogFactory> catalogs, OutputStream os) throws IOException {
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.transform(new SAXSource(new XMLArchetypeCatalogsWriter(catalogs), new InputSource()), new StreamResult(os));

    } catch(TransformerFactoryConfigurationError ex) {
      throw new IOException("Unable to write Archetype catalogs; " + ex.getMessage());

    } catch(TransformerException ex) {
      throw new IOException("Unable to write Archetype catalogs; " + ex.getMessage());
    
    }
  }

  static class XMLArchetypeCatalogsWriter extends XMLFilterImpl {

    private final Collection<ArchetypeCatalogFactory> catalogs;

    public XMLArchetypeCatalogsWriter(Collection<ArchetypeCatalogFactory> catalogs) {
      this.catalogs = catalogs;
    }

    public void parse(InputSource input) throws SAXException {
      ContentHandler handler = getContentHandler();
      handler.startDocument();
      handler.startElement(null, ELEMENT_CATALOGS, ELEMENT_CATALOGS, new AttributesImpl());

      for(ArchetypeCatalogFactory factory : this.catalogs) {
        if(factory.isEditable()) {
          if(factory instanceof LocalCatalogFactory) {
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute(null, ATT_CATALOG_TYPE, ATT_CATALOG_TYPE, null, TYPE_LOCAL);
            attrs.addAttribute(null, ATT_CATALOG_LOCATION, ATT_CATALOG_LOCATION, null, factory.getId());
            handler.startElement(null, ELEMENT_CATALOG, ELEMENT_CATALOG, attrs);
            handler.endElement(null, ELEMENT_CATALOG, ELEMENT_CATALOG);
          } else if(factory instanceof RemoteCatalogFactory) {
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute(null, ATT_CATALOG_TYPE, ATT_CATALOG_TYPE, null, TYPE_REMOTE);
            attrs.addAttribute(null, ATT_CATALOG_LOCATION, ATT_CATALOG_LOCATION, null, factory.getId());
            handler.startElement(null, ELEMENT_CATALOG, ELEMENT_CATALOG, attrs);
            handler.endElement(null, ELEMENT_CATALOG, ELEMENT_CATALOG);
          }
        }
      }

      handler.endElement(null, ELEMENT_CATALOGS, ELEMENT_CATALOGS);
      handler.endDocument();
    }
  }

  static class ArchetypeCatalogsContentHandler extends DefaultHandler {

    private Collection<ArchetypeCatalogFactory> catalogs;

    public ArchetypeCatalogsContentHandler(Collection<ArchetypeCatalogFactory> catalogs) {
      this.catalogs = catalogs;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
      if(ELEMENT_CATALOG.equals(qName) && attributes != null) {
        String type = attributes.getValue(ATT_CATALOG_TYPE);
        if(TYPE_LOCAL.equals(type)) {
          String path = attributes.getValue(ATT_CATALOG_LOCATION);
          if(path!=null) {
            String description = attributes.getValue(ATT_CATALOG_DESCRIPTION);
            catalogs.add(new LocalCatalogFactory(path, description, true));
          }
        } else if(TYPE_REMOTE.equals(type)) {
          String url = attributes.getValue(ATT_CATALOG_LOCATION);
          if(url!=null) {
            String description = attributes.getValue(ATT_CATALOG_DESCRIPTION);
            catalogs.add(new RemoteCatalogFactory(url, description, true));
          }
        }
      }
    }

  }

}
