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
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
/**
 * Hello fellow tester:
 * everytime this test finds a regression add an 'x' here:
 * everytime you do mindless test update add an 'y' here:
 * @author mkleint
 *
 */

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

    v.doc = new StringDocument("<project>\t\ta</project>");
    assertEquals("a", PomContentAssistProcessor.extractPrefix(v, 12));
  }

  private class StringDocument extends AbstractDocument {
    private String value;

    public StringDocument(String val) {
      value = val;
    }

    public char getChar(int pos) throws BadLocationException {
      return value.charAt(pos);
    }

    public String get(int pos, int length) throws BadLocationException {
      return value.substring(pos, pos + length);
    }

    public int getLength() {
      return value.length();
    }
  }

  private class MockTextViewer implements ITextViewer {

    public IDocument doc;

    public void activatePlugins() {
    }

    public void addTextInputListener(ITextInputListener listener) {
    }

    public void addTextListener(ITextListener listener) {
    }

    public void addViewportListener(IViewportListener listener) {
    }

    public void changeTextPresentation(TextPresentation presentation, boolean controlRedraw) {
    }

    public int getBottomIndex() {
      return 0;
    }

    public int getBottomIndexEndOffset() {
      return 0;
    }

    public IDocument getDocument() {
      return doc;
    }

    public IFindReplaceTarget getFindReplaceTarget() {
      return null;
    }

    public ITextOperationTarget getTextOperationTarget() {
      return null;
    }

    public int getTopIndex() {
      return 0;
    }

    public int getTopIndexStartOffset() {
      return 0;
    }

    public int getTopInset() {
      return 0;
    }

    public IRegion getVisibleRegion() {
      return null;
    }

    public void invalidateTextPresentation() {
    }

    public boolean isEditable() {
      return false;
    }

    public boolean overlapsWithVisibleRegion(int offset, int length) {
      return false;
    }

    public void removeTextInputListener(ITextInputListener listener) {
    }

    public void removeTextListener(ITextListener listener) {
    }

    public void removeViewportListener(IViewportListener listener) {
    }

    public void resetPlugins() {
    }

    public void resetVisibleRegion() {
    }

    public void revealRange(int offset, int length) {
    }

    public void setAutoIndentStrategy(IAutoIndentStrategy strategy, String contentType) {
    }

    public void setDefaultPrefixes(String[] defaultPrefixes, String contentType) {
    }

    public void setDocument(IDocument document) {
    }

    public void setDocument(IDocument document, int modelRangeOffset, int modelRangeLength) {
    }

    public void setEditable(boolean editable) {
    }

    public void setEventConsumer(IEventConsumer consumer) {
    }

    public void setIndentPrefixes(String[] indentPrefixes, String contentType) {
    }

    public void setSelectedRange(int offset, int length) {
    }

    public void setTextDoubleClickStrategy(ITextDoubleClickStrategy strategy, String contentType) {
    }

    public void setTextHover(ITextHover textViewerHover, String contentType) {
    }

    public void setTopIndex(int index) {
    }

    public void setUndoManager(IUndoManager undoManager) {
    }

    public void setVisibleRegion(int offset, int length) {
    }

    public ISelectionProvider getSelectionProvider() {
      return null;
    }

    public Point getSelectedRange() {
      return null;
    }

    public StyledText getTextWidget() {
      return null;
    }

    public void setTextColor(Color color) {
    }

    public void setTextColor(Color color, int offset, int length, boolean controlRedraw) {
    }
  }
}