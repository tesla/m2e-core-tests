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
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;

import org.maven.ide.eclipse.actions.OpenPomAction;


/**
 * @author Eugene Kuleshov
 */
class PomHyperlinkDetector implements IHyperlinkDetector {

  private final String[] versioned = new String[] {
      "dependency>",
      "parent>",
      "plugin>",
      "reportPlugin>",
      "extension>"
  };
  public IHyperlink[] detectHyperlinks(ITextViewer textViewer, final IRegion region, boolean canShowMultipleHyperlinks) {
    if(region == null || textViewer == null) {
      return null;
    }

    IDocument document = textViewer.getDocument();
    if(document == null) {
      return null;
    }

    IRegion lineInfo;
    String line;
    try {
      lineInfo = document.getLineInformationOfOffset(region.getOffset());
      line = document.get(lineInfo.getOffset(), lineInfo.getLength());
    } catch(BadLocationException ex) {
      return null;
    }

    if(line.length() == 0) {
      return null;
    }

    final int offset = region.getOffset();

    final String text = document.get();
    Fragment fragment = null;
    for (String el : versioned) {
      fragment = getFragment(text, offset, "<" + el, "</" + el);
      if (fragment != null) break;
    }
    
    if (fragment == null) {
      //TODO add different rules here.
      return null;
    }
    
    final Fragment groupId = getValue(fragment, "<groupId>", "</groupId>");
    final Fragment artifactId = getValue(fragment, "<artifactId>", "</artifactId>");
    final Fragment version = getValue(fragment, "<version>", "</version>");
    if (version == null) {
      // better exit now until we are capable of resolving the version from resolved project. 
      return null;
    } 
    IHyperlink pomHyperlink = new IHyperlink() {
      public IRegion getHyperlinkRegion() {
        //the goal here is to have the groupid/artifactid/version combo underscored by the link.
        //that will prevent underscoring big portions (like plugin config) underscored and
        // will also handle cases like dependencies within plugins.
        int max = groupId != null ? groupId.offset + groupId.length : Integer.MIN_VALUE;
        int min = groupId != null ? groupId.offset : Integer.MAX_VALUE;
        max = Math.max(max, artifactId != null ? artifactId.offset + artifactId.length : Integer.MIN_VALUE);
        min = Math.min(min, artifactId != null ? artifactId.offset : Integer.MAX_VALUE);
        max = Math.max(max, version != null ? version.offset + version.length : Integer.MIN_VALUE);
        min = Math.min(min, version != null ? version.offset : Integer.MAX_VALUE);
        return new Region(min, max - min);
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
            // TODO resolve groupId if groupId==null
            // TODO resolve version if version==null
            OpenPomAction.openEditor(groupId == null ? "org.apache.maven.plugins" : groupId.text, 
                                     artifactId == null ? null : artifactId.text, 
                                     version == null ? null : version.text, monitor);
            return Status.OK_STATUS;
          }
        }.schedule();
      }

    };

    return new IHyperlink[] {pomHyperlink};
  }

  /**
   * fragment offset returned contains the xml elements 
   * while the text only includes the element text value
   */
  private Fragment getValue(Fragment section, String startTag, String endTag) {
    int start = section.text.indexOf(startTag);
    if(start == -1) {
      return null;
    }
    int end = section.text.indexOf(endTag);
    if(end == -1) {
      return null;
    }

    return new Fragment(section.text.substring(start + startTag.length(), end).trim(), section.offset + start, end + endTag.length() - start);
  }

  /**
   * returns the text, offset and length of the xml element. text includes the xml tags. 
   */
  private Fragment getFragment(String text, int offset, String startTag, String endTag) {
    int start = text.substring(0, offset).lastIndexOf(startTag);
    if(start == -1) {
      return null;
    }

    int end = text.indexOf(endTag, start);
    if(end == -1 || end <= offset) {
      return null;
    }
    end = end + endTag.length();
    return new Fragment(text.substring(start, end), start, end - start);
  }
  
  private static class Fragment {
    final int length;
    final int offset;
    final String text;
    
    Fragment(String text, int start, int len) {
      this.text = text;
      this.offset = start;
      
      this.length = len;
      
    }

    @Override
    public String toString() {
      return text;
    }
  }

//  private int findChar(String text, char c, int offset, int step) {
//    for(int pos = offset; pos>=0 && pos<text.length(); pos += step) {
//      if(text.charAt(pos)==c) {
//        return pos;
//      }
//    }
//    return -1;
//  }

}
