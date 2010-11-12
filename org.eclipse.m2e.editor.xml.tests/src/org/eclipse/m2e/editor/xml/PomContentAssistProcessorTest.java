/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import junit.framework.TestCase;

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IEventConsumer;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.m2e.editor.xml.PomContentAssistProcessor;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;

public class PomContentAssistProcessorTest extends TestCase {

  public void testExtractPrefix() throws Exception {
    MockTextViewer v = new MockTextViewer();
    v.doc = new StringDocument("<module>abc</module>");
    assertEquals("", PomContentAssistProcessor.extractPrefix(v, 8));
    assertEquals("a", PomContentAssistProcessor.extractPrefix(v, 9));
    assertEquals("ab", PomContentAssistProcessor.extractPrefix(v, 10));
    assertEquals("abc", PomContentAssistProcessor.extractPrefix(v, 11));

    v.doc = new StringDocument("<module>abc/def</module>");
    assertEquals("abc/", PomContentAssistProcessor.extractPrefix(v, 12));
    assertEquals("abc/de", PomContentAssistProcessor.extractPrefix(v, 14));

    v.doc = new StringDocument("<module>../abc/def</module>");
    assertEquals("../abc/", PomContentAssistProcessor.extractPrefix(v, 15));

  }

  private class StringDocument extends AbstractDocument {
    private String value;

    public StringDocument(String val) {
      value = val;
    }

    public char getChar(int pos) throws BadLocationException {
      // TODO Auto-generated method stub
      return value.charAt(pos);
    }

    public String get(int pos, int length) throws BadLocationException {
      // TODO Auto-generated method stub
      return value.substring(pos, pos + length);
    }

    public int getLength() {
      // TODO Auto-generated method stub
      return value.length();
    }

  }

  private class MockTextViewer implements ITextViewer {

    public IDocument doc;

    public void activatePlugins() {
      // TODO Auto-generated method stub

    }

    public void addTextInputListener(ITextInputListener listener) {
      // TODO Auto-generated method stub

    }

    public void addTextListener(ITextListener listener) {
      // TODO Auto-generated method stub

    }

    public void addViewportListener(IViewportListener listener) {
      // TODO Auto-generated method stub

    }

    public void changeTextPresentation(TextPresentation presentation, boolean controlRedraw) {
      // TODO Auto-generated method stub

    }

    public int getBottomIndex() {
      // TODO Auto-generated method stub
      return 0;
    }

    public int getBottomIndexEndOffset() {
      // TODO Auto-generated method stub
      return 0;
    }

    public IDocument getDocument() {
      // TODO Auto-generated method stub
      return doc;
    }

    public IFindReplaceTarget getFindReplaceTarget() {
      // TODO Auto-generated method stub
      return null;
    }

    public ITextOperationTarget getTextOperationTarget() {
      // TODO Auto-generated method stub
      return null;
    }

    public int getTopIndex() {
      // TODO Auto-generated method stub
      return 0;
    }

    public int getTopIndexStartOffset() {
      // TODO Auto-generated method stub
      return 0;
    }

    public int getTopInset() {
      // TODO Auto-generated method stub
      return 0;
    }

    public IRegion getVisibleRegion() {
      // TODO Auto-generated method stub
      return null;
    }

    public void invalidateTextPresentation() {
      // TODO Auto-generated method stub

    }

    public boolean isEditable() {
      // TODO Auto-generated method stub
      return false;
    }

    public boolean overlapsWithVisibleRegion(int offset, int length) {
      // TODO Auto-generated method stub
      return false;
    }

    public void removeTextInputListener(ITextInputListener listener) {
      // TODO Auto-generated method stub

    }

    public void removeTextListener(ITextListener listener) {
      // TODO Auto-generated method stub

    }

    public void removeViewportListener(IViewportListener listener) {
      // TODO Auto-generated method stub

    }

    public void resetPlugins() {
      // TODO Auto-generated method stub

    }

    public void resetVisibleRegion() {
      // TODO Auto-generated method stub

    }

    public void revealRange(int offset, int length) {
      // TODO Auto-generated method stub

    }

    public void setAutoIndentStrategy(IAutoIndentStrategy strategy, String contentType) {
      // TODO Auto-generated method stub

    }

    public void setDefaultPrefixes(String[] defaultPrefixes, String contentType) {
      // TODO Auto-generated method stub

    }

    public void setDocument(IDocument document) {
      // TODO Auto-generated method stub

    }

    public void setDocument(IDocument document, int modelRangeOffset, int modelRangeLength) {
      // TODO Auto-generated method stub

    }

    public void setEditable(boolean editable) {
      // TODO Auto-generated method stub

    }

    public void setEventConsumer(IEventConsumer consumer) {
      // TODO Auto-generated method stub

    }

    public void setIndentPrefixes(String[] indentPrefixes, String contentType) {
      // TODO Auto-generated method stub

    }

    public void setSelectedRange(int offset, int length) {
      // TODO Auto-generated method stub

    }

    public void setTextDoubleClickStrategy(ITextDoubleClickStrategy strategy, String contentType) {
      // TODO Auto-generated method stub

    }

    public void setTextHover(ITextHover textViewerHover, String contentType) {
      // TODO Auto-generated method stub

    }

    public void setTopIndex(int index) {
      // TODO Auto-generated method stub

    }

    public void setUndoManager(IUndoManager undoManager) {
      // TODO Auto-generated method stub

    }

    public void setVisibleRegion(int offset, int length) {
      // TODO Auto-generated method stub

    }

    public ISelectionProvider getSelectionProvider() {
      // TODO Auto-generated method stub
      return null;
    }

    public Point getSelectedRange() {
      // TODO Auto-generated method stub
      return null;
    }

    public StyledText getTextWidget() {
      // TODO Auto-generated method stub
      return null;
    }

    public void setTextColor(Color color) {
      // TODO Auto-generated method stub

    }

    public void setTextColor(Color color, int offset, int length, boolean controlRedraw) {
      // TODO Auto-generated method stub

    }

  }

}
