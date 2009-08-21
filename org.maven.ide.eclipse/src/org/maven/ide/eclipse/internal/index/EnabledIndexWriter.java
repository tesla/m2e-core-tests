/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;

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
import org.maven.ide.eclipse.index.EnabledIndex;


/**
 * Writes the state of manually enabled/disabled indexes
 * 
 * @author dyocum
 */
public class EnabledIndexWriter {

  private static final String ELEMENT_INDEXES = "indexes";

  private static final String ELEMENT_INDEX = "index";

  private static final String ATT_INDEX_NAME = "indexName";

  private static final String ATT_REPOSITORY_URL = "repositoryUrl";



  public Collection<EnabledIndex> readIndexInfo(InputStream is) throws IOException {
    Collection<EnabledIndex> indexes = new ArrayList<EnabledIndex>();
    try {
      SAXParserFactory parserFactory = SAXParserFactory.newInstance();
      SAXParser parser = parserFactory.newSAXParser();
      parser.parse(is, new EnabledIndexContentHandler(indexes));
    } catch(SAXException ex) {
      String msg = "Unable to parse index configuration";
      MavenLogger.log(msg, ex);
      throw new IOException(msg + "; " + ex.getMessage());
    } catch(ParserConfigurationException ex) {
      String msg = "Unable to parse index configuration";
      MavenLogger.log(msg, ex);
      throw new IOException(msg + "; " + ex.getMessage());
    }
    return indexes;
  }

  public void writeIndexInfo(final Collection<EnabledIndex> indexes, OutputStream os) throws IOException {
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      //transformer.transform(new SAXSource(new XMLIndexInfoWriter(indexes), new InputSource()), new StreamResult(os));
      transformer.transform(new SAXSource(new XMLEnabledIndexWriter(indexes), new InputSource()), new StreamResult(os));
    } catch(TransformerFactoryConfigurationError ex) {
      throw new IOException("Unable to write index configuration; " + ex.getMessage());

    } catch(TransformerException ex) {
      throw new IOException("Unable to write index configuration; " + ex.getMessage());
    
    }
  }

  static class XMLEnabledIndexWriter extends XMLFilterImpl {

    private final Collection<EnabledIndex> indexes;

    public XMLEnabledIndexWriter(Collection<EnabledIndex> indexes) {
      this.indexes = indexes;
    }

    public void parse(InputSource input) throws SAXException {
      ContentHandler handler = getContentHandler();
      handler.startDocument();
      handler.startElement(null, ELEMENT_INDEXES, ELEMENT_INDEXES, new AttributesImpl());

      for(EnabledIndex index : this.indexes) {
        AttributesImpl indexAttrs = new AttributesImpl();
        if(index.getName() != null){
          indexAttrs.addAttribute(null, ATT_INDEX_NAME, ATT_INDEX_NAME, null, index.getName());
        }
        if(index.getUrl()!=null) {
          indexAttrs.addAttribute(null, ATT_REPOSITORY_URL, ATT_REPOSITORY_URL, null, index.getUrl());
        }
        handler.startElement(null, ELEMENT_INDEX, ELEMENT_INDEX, indexAttrs);
        handler.endElement(null, ELEMENT_INDEX, ELEMENT_INDEX);
      }

      handler.endElement(null, ELEMENT_INDEXES, ELEMENT_INDEXES);
      handler.endDocument();
    }
  }

  static class EnabledIndexContentHandler extends DefaultHandler {

    private Collection<EnabledIndex> indexes;

    public EnabledIndexContentHandler(Collection<EnabledIndex> indexes) {
      this.indexes = indexes;
    }

    public void startElement(String uri, String localName, String elemName, Attributes attributes) {
      if(ELEMENT_INDEX.equals(elemName) && attributes != null) {
        String indexName = attributes.getValue(ATT_INDEX_NAME);
        String repositoryUrl = attributes.getValue(ATT_REPOSITORY_URL);
        EnabledIndex index = new EnabledIndex(indexName, repositoryUrl);
        indexes.add(index);
      }
    }

  }

}
