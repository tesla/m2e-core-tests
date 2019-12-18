/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import static org.junit.Assert.assertEquals;

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
import org.junit.Test;


/**
 * Hello fellow tester: everytime this test finds a regression add an 'x' here: everytime you do mindless test update
 * add an 'y' here:
 * 
 * @author mkleint
 */

public class PomContentAssistProcessorTest {

	@Test
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

    @Override
	public char getChar(int pos) throws BadLocationException {
      return value.charAt(pos);
    }

    @Override
	public String get(int pos, int length) throws BadLocationException {
      return value.substring(pos, pos + length);
    }

    @Override
	public int getLength() {
      return value.length();
    }
  }

  private class MockTextViewer implements ITextViewer {

    public IDocument doc;

    @Override
	public void activatePlugins() {
    }

    @Override
	public void addTextInputListener(ITextInputListener listener) {
    }

    @Override
	public void addTextListener(ITextListener listener) {
    }

    @Override
	public void addViewportListener(IViewportListener listener) {
    }

    @Override
	public void changeTextPresentation(TextPresentation presentation, boolean controlRedraw) {
    }

    @Override
	public int getBottomIndex() {
      return 0;
    }

    @Override
	public int getBottomIndexEndOffset() {
      return 0;
    }

    @Override
	public IDocument getDocument() {
      return doc;
    }

    @Override
	public IFindReplaceTarget getFindReplaceTarget() {
      return null;
    }

    @Override
	public ITextOperationTarget getTextOperationTarget() {
      return null;
    }

    @Override
	public int getTopIndex() {
      return 0;
    }

    @Override
	public int getTopIndexStartOffset() {
      return 0;
    }

    @Override
	public int getTopInset() {
      return 0;
    }

    @Override
	public IRegion getVisibleRegion() {
      return null;
    }

    @Override
	public void invalidateTextPresentation() {
    }

    @Override
	public boolean isEditable() {
      return false;
    }

    @Override
	public boolean overlapsWithVisibleRegion(int offset, int length) {
      return false;
    }

    @Override
	public void removeTextInputListener(ITextInputListener listener) {
    }

    @Override
	public void removeTextListener(ITextListener listener) {
    }

    @Override
	public void removeViewportListener(IViewportListener listener) {
    }

    @Override
	public void resetPlugins() {
    }

    @Override
	public void resetVisibleRegion() {
    }

    @Override
	public void revealRange(int offset, int length) {
    }

    @Override
	public void setAutoIndentStrategy(IAutoIndentStrategy strategy, String contentType) {
    }

    @Override
	public void setDefaultPrefixes(String[] defaultPrefixes, String contentType) {
    }

    @Override
	public void setDocument(IDocument document) {
    }

    @Override
	public void setDocument(IDocument document, int modelRangeOffset, int modelRangeLength) {
    }

    @Override
	public void setEditable(boolean editable) {
    }

    @Override
	public void setEventConsumer(IEventConsumer consumer) {
    }

    @Override
	public void setIndentPrefixes(String[] indentPrefixes, String contentType) {
    }

    @Override
	public void setSelectedRange(int offset, int length) {
    }

    @Override
	public void setTextDoubleClickStrategy(ITextDoubleClickStrategy strategy, String contentType) {
    }

    @Override
	public void setTextHover(ITextHover textViewerHover, String contentType) {
    }

    @Override
	public void setTopIndex(int index) {
    }

    @Override
	public void setUndoManager(IUndoManager undoManager) {
    }

    @Override
	public void setVisibleRegion(int offset, int length) {
    }

    @Override
	public ISelectionProvider getSelectionProvider() {
      return null;
    }

    @Override
	public Point getSelectedRange() {
      return null;
    }

    @Override
	public StyledText getTextWidget() {
      return null;
    }

    @Override
	public void setTextColor(Color color) {
    }

    @Override
	public void setTextColor(Color color, int offset, int length, boolean controlRedraw) {
    }
  }
}
