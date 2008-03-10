/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.Messages;

/**
 * A simple dialog allowing the selection of
 * Maven projects and subfolders containing POMs.
 */
public class MavenProjectSelectionDialog extends AbstractMavenDialog {

  protected static final String DIALOG_SETTINGS = MavenProjectSelectionDialog.class.getName();

  /** the tree viewer */
  private TreeViewer viewer;
  

  /** Creates a dialog instance. */
  public MavenProjectSelectionDialog( Shell parent ) {
    super( parent, DIALOG_SETTINGS );

    setShellStyle( getShellStyle() | SWT.RESIZE );
    setTitle( Messages.getString( "projectSelectionDialog.title" ) );
  }
  
  /** Produces the result of the selection. */
  protected void computeResult() {
    setResult( ( ( IStructuredSelection ) viewer.getSelection() ).toList() ); 
  }


  /** Creates the dialog controls. */
  protected Control createDialogArea( Composite parent ) {
    readSettings();

    Composite composite = (Composite) super.createDialogArea(parent);

    viewer = new TreeViewer(composite);
    viewer.getControl().setLayoutData(
      new GridData( GridData.FILL, GridData.FILL, true, true ) );
    viewer.setContentProvider(new MavenContainerContentProvider());
    viewer.setLabelProvider(
      WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
    viewer.setInput( ResourcesPlugin.getWorkspace() );
    
    viewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        okPressed();
      }
    });

    return composite;
  }


  /** The content provider class. */
  protected class MavenContainerContentProvider implements ITreeContentProvider {

    /** Returns the children of the parent node. */
    public Object[] getChildren( Object parent ) {
      if ( parent instanceof IWorkspace ) {
        IProject[] projects = ( ( IWorkspace ) parent ).getRoot().getProjects();

        List children = new ArrayList();
        for ( int i = 0; i < projects.length; i++) {
          try {
            if ( projects[i].isOpen() &&
                projects[i].hasNature( MavenPlugin.NATURE_ID ) ) {
              children.add( projects[i] );
            }
          }
          catch ( CoreException e ) {
            MavenPlugin.log( "Error checking project: " + e.getMessage(), e );
          }
        }
        return children.toArray();
      } 
      else if ( parent instanceof IContainer ) {
        IContainer container = ( IContainer ) parent;
        if ( container.isAccessible() ) {
          try {
            List children = new ArrayList();
            IResource[] members = container.members();
            for ( int i = 0; i < members.length; i++ ) {
              if ( members[i] instanceof IContainer &&
                  ( ( IContainer ) members[i] ).exists(
                    new Path( MavenPlugin.POM_FILE_NAME ) ) ) {
                children.add( members[i] );
              }
            }
            return children.toArray();
          }
          catch (CoreException e) {
            MavenPlugin.log( "Error checking container: " + e.getMessage(), e );
          }
        }
      }
      return new Object[0];
    }
    
    /** Returns the parent of the given element. */
    public Object getParent( Object element ) {
      if ( element instanceof IResource ) {
        return ( ( IResource) element ).getParent();
      }
      return null;
    }

    /** Returns true if the element has any children. */
    public boolean hasChildren( Object element ) {
      return getChildren( element ).length > 0;
    }

    /** Disposes of any resources used. */
    public void dispose() {
    }

    /** Handles the input change. */
    public void inputChanged( Viewer viewer, Object arg1, Object arg2 ) {
    }

    /** Returns the elements of the given root. */
    public Object[] getElements( Object element ) {
      return getChildren( element );
    }
  }
}
