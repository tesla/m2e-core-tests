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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

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

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.index.IndexInfo;


/**
 * IndexInfo writer
 * 
 * @author Eugene Kuleshov
 */
public class IndexInfoWriter {

  private static final String ELEMENT_INDEXES = "indexes";

  private static final String ELEMENT_INDEX = "indexInfo";

  private static final String ATT_INDEX_NAME = "indexName";

  private static final String ATT_REPOSITORY_URL = "repositoryUrl";

  private static final String ATT_UPDATE_URL = "updateUrl";
  
  private static final String ATT_IS_SHORT = "isShort";

  private static final String ATT_UPDATE_TIME = "updateTime";

  private static final String UPDATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss Z";

  public Collection readIndexInfo(InputStream is) throws IOException {
    Collection indexes = new ArrayList();
    try {
      SAXParserFactory parserFactory = SAXParserFactory.newInstance();
      SAXParser parser = parserFactory.newSAXParser();
      parser.parse(is, new IndexInfoContentHandler(indexes));
    } catch(SAXException ex) {
      String msg = "Unable to parse index configuration";
      MavenPlugin.log(msg, ex);
      throw new IOException(msg + "; " + ex.getMessage());
    } catch(ParserConfigurationException ex) {
      String msg = "Unable to parse index configuration";
      MavenPlugin.log(msg, ex);
      throw new IOException(msg + "; " + ex.getMessage());
    }
    return indexes;
  }

  public void writeIndexInfo(final Collection indexes, OutputStream os) throws IOException {
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.transform(new SAXSource(new XMLIndexInfoWriter(indexes), new InputSource()), new StreamResult(os));

    } catch(TransformerFactoryConfigurationError ex) {
      throw new IOException("Unable to write index configuration; " + ex.getMessage());

    } catch(TransformerException ex) {
      throw new IOException("Unable to write index configuration; " + ex.getMessage());
    
    }
  }

  static class XMLIndexInfoWriter extends XMLFilterImpl {

    private final Collection indexes;

    public XMLIndexInfoWriter(Collection indexes) {
      this.indexes = indexes;
    }

    public void parse(InputSource input) throws SAXException {
      ContentHandler handler = getContentHandler();
      handler.startDocument();
      handler.startElement(null, ELEMENT_INDEXES, ELEMENT_INDEXES, new AttributesImpl());

      for(Iterator it = this.indexes.iterator(); it.hasNext();) {
        IndexInfo info = (IndexInfo) it.next();
        if(IndexInfo.Type.REMOTE.equals(info.getType())) {
          AttributesImpl indexAttrs = new AttributesImpl();
          indexAttrs.addAttribute(null, ATT_INDEX_NAME, ATT_INDEX_NAME, null, info.getIndexName());
          if(info.getRepositoryUrl()!=null) {
            indexAttrs.addAttribute(null, ATT_REPOSITORY_URL, ATT_REPOSITORY_URL, null, info.getRepositoryUrl());
          }
          if(info.getIndexUpdateUrl()!=null) {
            indexAttrs.addAttribute(null, ATT_UPDATE_URL, ATT_UPDATE_URL, null, info.getIndexUpdateUrl());
          }
          indexAttrs.addAttribute(null, ATT_IS_SHORT, ATT_IS_SHORT, null, Boolean.toString(info.isShort()));
          
          if(info.getUpdateTime()!=null) {
            indexAttrs.addAttribute(null, ATT_UPDATE_TIME, ATT_UPDATE_TIME, null, //
                new SimpleDateFormat(UPDATE_TIME_FORMAT).format(info.getUpdateTime()));
          }

          handler.startElement(null, ELEMENT_INDEX, ELEMENT_INDEX, indexAttrs);
          handler.endElement(null, ELEMENT_INDEX, ELEMENT_INDEX);
        }
      }

      handler.endElement(null, ELEMENT_INDEXES, ELEMENT_INDEXES);
      handler.endDocument();
    }
  }

  static class IndexInfoContentHandler extends DefaultHandler {

    private Collection indexes;

    public IndexInfoContentHandler(Collection indexes) {
      this.indexes = indexes;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
      if(ELEMENT_INDEX.equals(qName) && attributes != null) {
        String indexName = attributes.getValue(ATT_INDEX_NAME);
        String repositoryUrl = attributes.getValue(ATT_REPOSITORY_URL);
        String indexUpdateUrl = attributes.getValue(ATT_UPDATE_URL);
        
        String isShortValue = attributes.getValue(ATT_IS_SHORT);
        boolean isShort = isShortValue == null ? false : Boolean.valueOf(isShortValue).booleanValue();

        String updateTimeValue = attributes.getValue(ATT_UPDATE_TIME);
        Date updateTime;
        try {
          updateTime = new SimpleDateFormat(UPDATE_TIME_FORMAT).parse(updateTimeValue);
        } catch(Exception ex) {
          updateTime = null;
        }

        IndexInfo indexInfo = new IndexInfo(indexName, null, repositoryUrl, IndexInfo.Type.REMOTE, isShort);
        indexInfo.setIndexUpdateUrl(indexUpdateUrl);
        indexInfo.setUpdateTime(updateTime);
        indexes.add(indexInfo);
      }
    }

  }

}
