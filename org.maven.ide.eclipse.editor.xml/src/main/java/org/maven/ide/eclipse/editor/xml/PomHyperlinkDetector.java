/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;

import org.maven.ide.eclipse.actions.OpenPomAction;

final class PomHyperlinkDetector implements IHyperlinkDetector {
  public IHyperlink[] detectHyperlinks(ITextViewer textViewer, final IRegion region, boolean canShowMultipleHyperlinks) {
    if (region == null || textViewer == null) {
      return null;
    }

    IDocument document = textViewer.getDocument();
    if (document == null) {
      return null;
    }

    IRegion lineInfo;
    String line;
    try {
      lineInfo = document.getLineInformationOfOffset(region.getOffset());
      line = document.get(lineInfo.getOffset(), lineInfo.getLength());
    } catch (BadLocationException ex) {
      return null;
    }

    if (line.length() == 0) {
      return null;
    }

    final int offset = region.getOffset();
    
    final String text = document.get();
    
    String fragment = getFragment(text, offset, "<dependency>", "</dependency>");
    if(fragment==null) {
      fragment = getFragment(text, offset, "<dependencyManagement>", "</dependencyManagement>");
      if(fragment==null) {
        fragment = getFragment(text, offset, "<parent>", "</parent>");
        if(fragment==null) {
          return null;
        }
      }
    }
    
    final String groupId = getValue(fragment, "<groupId>", "</groupId>");
    final String artifactId = getValue(fragment, "<artifactId>", "</artifactId>");
    final String version = getValue(fragment, "<version>", "</version>");
    
    IHyperlink pomHyperlink = new IHyperlink() {

      public IRegion getHyperlinkRegion() {
        int start = text.substring(0, offset).lastIndexOf('>');
        int end = text.indexOf("</", start);
        return region;
      }

      public String getHyperlinkText() {
        return groupId + " : " + artifactId + ":" + version;
      }

      public String getTypeLabel() {
        return "pom";
      }

      public void open() {
        new Job("Opening POM") {
          protected IStatus run(IProgressMonitor monitor) {
            OpenPomAction.openEditor(groupId, artifactId, version);
            return Status.OK_STATUS;
          }
        }.schedule();
      }
      
    };
    
    return new IHyperlink[] { pomHyperlink };
  }
  
  private String getValue(String dependency, String startTag, String endTag) {
    int start = dependency.indexOf(startTag);
    if(start==-1) {
      return null;
    }
    int end = dependency.indexOf(endTag);
    if(end==-1) {
      return null;
    }
    
    return dependency.substring(start + startTag.length(), end).trim();
  }

  private String getFragment(String text, int offset, String startTag, String endTag) {
    int start = text.substring(0, offset).lastIndexOf(startTag);
    if(start==-1) {
      return null;
    }

    int end = text.indexOf(endTag, start);
    if(end==-1 || end<=offset) {
      return null;
    }
    
    return text.substring(start, end + endTag.length());
  }
  
  private int findChar(String text, char c, int offset, int step) {
    for(int pos = offset; pos>=0 && pos<text.length(); pos += step) {
      if(text.charAt(pos)==c) {
        return pos;
      }
    }
    return -1;
  }
  
}