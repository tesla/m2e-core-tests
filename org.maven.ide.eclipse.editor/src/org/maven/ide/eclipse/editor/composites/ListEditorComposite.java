/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.composites;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * List editor composite
 * 
 * @author Eugene Kuleshov
 */
public class ListEditorComposite<T> extends Composite {

  private TableViewer viewer;
  
  private Button addButton;
  private Button removeButton;

  public ListEditorComposite(Composite parent, int style) {
    super(parent, style);

    FormToolkit toolkit = new FormToolkit(parent.getDisplay());
    
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginWidth = 1;
    gridLayout.marginHeight = 1;
    setLayout(gridLayout);

    Table table = toolkit.createTable(this, SWT.FLAT | SWT.MULTI | style); 
    viewer = new TableViewer(table);
    
    GridData viewerData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2);
    viewerData.widthHint = 150;
    viewerData.heightHint = 53;
    table.setLayoutData(viewerData);
    
    viewer.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.TRUE);
    // table.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.TRUE);
    
    addButton = toolkit.createButton(this, "Add...", SWT.FLAT);
    addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    
    removeButton = toolkit.createButton(this, "Delete", SWT.FLAT);
    removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
    removeButton.setEnabled(false);
    
    viewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        removeButton.setEnabled(!viewer.getSelection().isEmpty());
      }
    });
    
    toolkit.paintBordersFor(this);
  }
  
  public void setLabelProvider(ILabelProvider labelProvider) {
    viewer.setLabelProvider(labelProvider);
  }
  
  public void setContentProvider(ListEditorContentProvider<T> contentProvider) {
    viewer.setContentProvider(contentProvider);
  }

  public void setInput(Object input) {
    viewer.setInput(input);
  }
  
  public void setOpenListener(IOpenListener listener) {
    viewer.addOpenListener(listener);
  }
  
  public void addSelectionListener(ISelectionChangedListener listener) {
    viewer.addSelectionChangedListener(listener);
  }
  
  public void setAddListener(SelectionListener listener) {
    addButton.addSelectionListener(listener);
  }
  
  public void setRemoveListener(SelectionListener listener) {
    removeButton.addSelectionListener(listener);
  }

  public TableViewer getViewer() {
    return viewer;
  }

  @SuppressWarnings("unchecked")
  public List<T> getSelection() {
    IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
    return selection==null ? Collections.emptyList() : selection.toList();
  }

  public void setSelection(List<T> selection) {
    viewer.setSelection(new StructuredSelection(selection));
  }

}

