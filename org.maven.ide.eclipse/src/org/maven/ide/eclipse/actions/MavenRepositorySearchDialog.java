/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.actions;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.wizards.MavenPomSelectionComponent;


/**
 * Maven POM Search dialog
 * 
 * @author Eugene Kuleshov
 */
public class MavenRepositorySearchDialog extends AbstractMavenDialog {
  private static final String DIALOG_SETTINGS = MavenRepositorySearchDialog.class.getName();

  /**
   * Set&lt;Artifact&gt;
   */
  private Set artifacts;

  /**
   * One of 
   *   {@link IndexManager#SEARCH_ARTIFACT}, 
   *   {@link IndexManager#SEARCH_CLASS_NAME}, 
   */
  private String queryType;

  private String queryText;

  MavenPomSelectionComponent pomSelectionComponent;

  private IndexedArtifact selectedIndexedArtifact;

  private IndexedArtifactFile selectedIndexedArtifactFile;
  
  /**
   * Create repository search dialog
   * 
   * @param parent parent shell
   * @param title dialog title
   * @param queryType one of 
   *   {@link IndexManager#SEARCH_ARTIFACT}, 
   *   {@link IndexManager#SEARCH_CLASS_NAME}, 
   * @param artifacts Set&lt;Artifact&gt;
   */
  public MavenRepositorySearchDialog(Shell parent, String title, String queryType, Set artifacts) {
    super(parent, DIALOG_SETTINGS);
    this.artifacts = artifacts;
    this.queryType = queryType;

    setShellStyle(getShellStyle() | SWT.RESIZE);
    setStatusLineAboveButtons(true);
    setTitle(title);
  }

  public void setQuery(String query) {
    this.queryText = query;
  }

  protected Control createDialogArea(Composite parent) {
    readSettings();

    Composite composite = (Composite) super.createDialogArea(parent);

    pomSelectionComponent = new MavenPomSelectionComponent(composite, SWT.NONE);
    pomSelectionComponent.init(queryText, queryType, artifacts);
    
    pomSelectionComponent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    
    pomSelectionComponent.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        okPressedDelegate();
      }
    });
    pomSelectionComponent.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        updateStatusDelegate(pomSelectionComponent.getStatus());
      }
    });
    
    return composite;
  }
  
  void okPressedDelegate() {
    okPressed();
  }
  
  void updateStatusDelegate(IStatus status) {
    updateStatus(status);
  }

  protected void computeResult() {
    selectedIndexedArtifact = pomSelectionComponent.getIndexedArtifact();
    selectedIndexedArtifactFile = pomSelectionComponent.getIndexedArtifactFile();
    setResult(Collections.singletonList(selectedIndexedArtifactFile));
  }
  
  public IndexedArtifact getSelectedIndexedArtifact() {
    return this.selectedIndexedArtifact;
  }
  
  public IndexedArtifactFile getSelectedIndexedArtifactFile() {
    return this.selectedIndexedArtifactFile;
  }
  
}
