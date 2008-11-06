/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.refactoring;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.maven.ide.eclipse.core.MavenLogger;


/**
 * This class creates an org.eclipse.ltk.core.refactoring.DocumentChange instance based on old and new text values
 * <p>
 * 
 * @author Anton Kraev
 */
public class ChangeCreator {
  private String label;

  private IDocument oldDocument;

  private IDocument newDocument;

  private IFile oldFile;

  @SuppressWarnings("unchecked")
  public class LineComparator implements IRangeComparator {

    private final IDocument fDocument;
    private final ArrayList fHashes;

      /**
     * Create a line comparator for the given document.
     * 
     * @param document
     */
    public LineComparator(IDocument document) {
      fDocument= document;

      Object[] nulls= new Object[fDocument.getNumberOfLines()];
      fHashes= new ArrayList(Arrays.asList(nulls));
    }

    /*
       * @see org.eclipse.compare.rangedifferencer.IRangeComparator#getRangeCount()
       */
      public int getRangeCount() {
          return fDocument.getNumberOfLines();
      }

      /*
       * @see org.eclipse.compare.rangedifferencer.IRangeComparator#rangesEqual(int, org.eclipse.compare.rangedifferencer.IRangeComparator, int)
       */
      public boolean rangesEqual(int thisIndex, IRangeComparator other, int otherIndex) {
        try {
          return getHash(thisIndex).equals(((LineComparator) other).getHash(otherIndex));
        } catch (BadLocationException e) {
          MavenLogger.log("Problem comparing", e);
          return false;
        }
      }

    /*
     * @see org.eclipse.compare.rangedifferencer.IRangeComparator#skipRangeComparison(int, int, org.eclipse.compare.rangedifferencer.IRangeComparator)
     */
    public boolean skipRangeComparison(int length, int maxLength, IRangeComparator other) {
      return false;
    }

    /**
     * @param line the number of the line in the document to get the hash for
     * @return the hash of the line
     * @throws BadLocationException if the line number is invalid
     */
    private Integer getHash(int line) throws BadLocationException {
      Integer hash= (Integer) fHashes.get(line);
      if (hash == null) {
        IRegion lineRegion;
        lineRegion = fDocument.getLineInformation(line);
        String lineContents= fDocument.get(lineRegion.getOffset(), lineRegion.getLength());
        hash= new Integer(computeDJBHash(lineContents));
        fHashes.set(line, hash);
      }

      return hash;
    }

    /**
     * Compute a hash using the DJB hash algorithm
     * 
     * @param string the string for which to compute a hash
     * @return the DJB hash value of the string
     */
    private int computeDJBHash(String string) {
      int hash= 5381;
      int len= string.length();
      for (int i= 0; i < len; i++) {
        char ch= string.charAt(i);
        hash= (hash << 5) + hash + ch;
      }

      return hash;
    }
  }

  public ChangeCreator(IFile oldFile, IDocument oldDocument, IDocument newDocument, String label) {
    this.newDocument = newDocument;
    this.oldDocument = oldDocument;
    this.oldFile = oldFile;
    this.label = label;
  }

  public TextFileChange createChange() {
    TextFileChange change = new TextFileChange(label, oldFile);
    change.setSaveMode(TextFileChange.FORCE_SAVE);
    change.setEdit(new MultiTextEdit());
    Object leftSide= new LineComparator(oldDocument);
    Object rightSide= new LineComparator(newDocument);

    RangeDifference[] differences = RangeDifferencer.findDifferences((IRangeComparator) leftSide, (IRangeComparator) rightSide);
    for(int i = 0; i < differences.length; i++ ) {
      RangeDifference curr = differences[i];
      if (curr.kind() == RangeDifference.CHANGE && curr.rightLength() > 0) {
        int startLine = curr.rightStart();
        int endLine = curr.rightEnd() - 1;
        for(int j = startLine; j <= endLine; j++ ) {
          int newPos = curr.leftStart() - startLine + j;
          try {
            String newText = newDocument.get(newDocument.getLineOffset(newPos), newDocument.getLineLength(newPos));
            change.addEdit(new ReplaceEdit(oldDocument.getLineOffset(j), oldDocument.getLineLength(j), newText));
          } catch(BadLocationException ex) {
            ex.printStackTrace();
          }
        }
      }
    }
    return change;
  }
}
