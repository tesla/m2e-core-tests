/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wizards;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.apache.maven.artifact.Artifact;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;

/**
 * MavenPomSelectionComposite
 *
 * @author Eugene Kuleshov
 */
public class MavenPomSelectionComponent extends Composite {

  Text searchText = null;

  TreeViewer searchResultViewer = null;

  /**
   * Set&lt;Artifact&gt;
   */
  Set artifacts;

  /**
   * One of 
   *   {@link IndexManager#SEARCH_ARTIFACT}, 
   *   {@link IndexManager#SEARCH_CLASS_NAME}, 
   */
  String queryType;
  
  String queryText;

  SearchJob searchJob;

  private IStatus status;

  private ISelectionChangedListener selectionListener;

  HashSet artifactKeys = new HashSet();
  
  public MavenPomSelectionComponent(Composite parent, int style) {
    super(parent, style);
    
    createSearchComposite();
  }

  private void createSearchComposite() {
    setLayout(new GridLayout(1, false));
    
    Label searchTextlabel = new Label(this, SWT.NONE);
    searchTextlabel.setText("Query:");
    searchTextlabel.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

    searchText = new Text(this, SWT.BORDER);
    searchText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
    searchText.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if(e.keyCode == SWT.ARROW_DOWN) {
          searchResultViewer.getTree().setFocus();
        }
      }
    });
    searchText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        scheduleSearch(searchText.getText());
      }
    });

    Label searchResultsLabel = new Label(this, SWT.NONE);
    searchResultsLabel.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
    searchResultsLabel.setText("Search Results:");

    Tree tree = new Tree(this, SWT.BORDER | SWT.SINGLE);
    tree.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

    searchResultViewer = new TreeViewer(tree);
  }

  // 
  // this.queryText = queryText;
  // this.queryType = queryType;
  // this.artifacts = artifacts;
  
  public void init(String queryText, String queryType, Set artifacts) {
    this.queryText = queryText;
    this.queryType = queryType;
    this.artifacts = artifacts;
    
    if(queryText != null) {
      searchText.setText(queryText);
    }
    
    if(artifacts!=null) {
      for(Iterator it = artifacts.iterator(); it.hasNext();) {
        Artifact a = (Artifact) it.next();
        artifactKeys.add(a.getGroupId() + ":" + a.getArtifactId());
        artifactKeys.add(a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion());
      }
    }
    
    searchResultViewer.setContentProvider(new SearchResultContentProvider());
    searchResultViewer.setLabelProvider(new SearchResultLabelProvider(artifactKeys, queryType));
    searchResultViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        if(!selection.isEmpty()) {
          if(selection.size() == 1) {
            IndexedArtifactFile f = getSelectedIndexedArtifactFile(selection.getFirstElement());
            int severity = artifactKeys.contains(f.group + ":" + f.artifact) ? IStatus.ERROR : IStatus.OK;
            setStatus(severity, f.fname + " " + f.size + " " + f.date);
          } else {
            setStatus(IStatus.OK, "Selected " + selection.size());
          }
        } else {
          setStatus(IStatus.ERROR, "No selection");
        }
      }
    });

    setStatus(IStatus.ERROR, "");
    scheduleSearch(queryText);
  }
  
  public IStatus getStatus() {
    return this.status;
  }

  public void addDoubleClickListener(IDoubleClickListener listener) {
    searchResultViewer.addDoubleClickListener(listener);
  }
  
  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    this.selectionListener = listener;
  }

  void setStatus(int severity, String message) {
    this.status = new Status(severity, MavenPlugin.PLUGIN_ID, 0, message, null);
    if(selectionListener!=null) {
      selectionListener.selectionChanged(new SelectionChangedEvent(searchResultViewer, searchResultViewer.getSelection()));
    }
  }

  public IndexedArtifact getIndexedArtifact() {
    IStructuredSelection selection = (IStructuredSelection) searchResultViewer.getSelection();
    Object element = selection.getFirstElement();
    if(element instanceof IndexedArtifact) {
      return (IndexedArtifact) element;
    }
    TreeItem[] treeItems = searchResultViewer.getTree().getSelection();
    return (IndexedArtifact) treeItems[0].getParentItem().getData();
  }
  
  public IndexedArtifactFile getIndexedArtifactFile() {
    IStructuredSelection selection = (IStructuredSelection) searchResultViewer.getSelection();
    return getSelectedIndexedArtifactFile(selection.getFirstElement());
  }

  IndexedArtifactFile getSelectedIndexedArtifactFile(Object element) {
    if(element instanceof IndexedArtifact) {
      return (IndexedArtifactFile) ((IndexedArtifact) element).files.iterator().next();
    }
    return (IndexedArtifactFile) element;
  }

  void scheduleSearch(String query) {
    if(query != null && query.length() > 0) {
      if(searchJob == null) {
        IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
        searchJob = new SearchJob(queryType, indexManager);
      }

      searchJob.setQuery(query.toLowerCase());
      if(!searchJob.isRunning()) {
        searchJob.schedule();
      }
    }
  }


  /**
   * Search Job
   */
  private class SearchJob extends Job {

    private IndexManager indexManager;

    private String query;

    private String field;

    boolean isRunning = false;

    public SearchJob(String field, IndexManager indexManager) {
      super("Repository search");
      this.field = field;
      this.indexManager = indexManager;
    }

    public boolean isRunning() {
      return isRunning;
    }

    public void setQuery(String query) {
      this.query = query;
    }

    protected IStatus run(IProgressMonitor monitor) {
      isRunning = true;
      while(!monitor.isCanceled() && query != null) {
        String activeQuery = query;
        query = null;
        try {
          // Map res = indexer.search(indexManager.getIndexes(), activeQuery, field);
          // Map res = indexManager.search(activeQuery, IndexManager.SEARCH_PACKAGING);
          Map res = indexManager.search(activeQuery, field);
          if(IndexManager.SEARCH_CLASS_NAME.equals(field) && activeQuery.length() < IndexManager.MIN_CLASS_QUERY_LENGTH) {
            setResult(IStatus.WARNING, "Query '" + activeQuery + "' is too short", Collections.EMPTY_MAP);
          } else {
            setResult(IStatus.OK, "Results for '" + activeQuery + "' (" + res.size() + ")", res);
          }
        } catch(final RuntimeException ex) {
          setResult(IStatus.ERROR, "Search error: " + ex.toString(), Collections.EMPTY_MAP);
        } catch(final Exception ex) {
          setResult(IStatus.ERROR, "Search error: " + ex.getMessage(), Collections.EMPTY_MAP);
        }
      }
      isRunning = false;
      return Status.OK_STATUS;
    }

    private void setResult(final int severity, final String message, final Map result) {
      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          setStatus(severity, message);
          searchResultViewer.setInput(result);
        }
      });
    }

  }

  public static class SearchResultLabelProvider extends LabelProvider implements IColorProvider {
    private final Set artifactKeys;
    private final String queryType;

    public SearchResultLabelProvider(Set artifactKeys, String queryType) {
      this.artifactKeys = artifactKeys;
      this.queryType = queryType;
    }

    public String getText(Object element) {
      if(element instanceof IndexedArtifact) {
        IndexedArtifact a = (IndexedArtifact) element;
        return (a.className == null ? "" : a.className + "   " + a.packageName + "   ") + a.group + "   " + a.artifact;
      } else if(element instanceof IndexedArtifactFile) {
        IndexedArtifactFile f = (IndexedArtifactFile) element;
        long size_k = (f.size + 512)/1024;
        return f.version + " - " + f.fname + " - " + size_k + "K - " + f.date + " [" + f.repository + "]";
      }
      return super.getText(element);
    }

    public Color getForeground(Object element) {
      if(element instanceof IndexedArtifactFile) {
        IndexedArtifactFile f = (IndexedArtifactFile) element;
        if(artifactKeys.contains(f.group + ":" + f.artifact + ":" + f.version)) {
          return Display.getDefault().getSystemColor(SWT.COLOR_RED);
        }
        return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
      } else if(element instanceof IndexedArtifact) {
        IndexedArtifact i = (IndexedArtifact) element;
        if(artifactKeys.contains(i.group + ":" + i.artifact)) {
          return Display.getDefault().getSystemColor(SWT.COLOR_RED);
        }
      }
      return null;
    }

    public Color getBackground(Object element) {
      return null;
    }
    
    public Image getImage(Object element) {
      if(element instanceof IndexedArtifactFile) {
        IndexedArtifactFile f = (IndexedArtifactFile) element;
        if(IndexManager.SEARCH_CLASS_NAME.equals(queryType)) {
          if(f.sourcesExists==IndexManager.PRESENT) {
            return MavenPlugin.getImage("icons/java_src_obj.gif");
          }
          return MavenPlugin.getImage("icons/java_obj.gif");
        }
        if(f.sourcesExists==IndexManager.PRESENT) {
          return MavenPlugin.getImage("icons/jar_src_version.gif");
        }
        return MavenPlugin.getImage("icons/jar_version.gif");
      } else if(element instanceof IndexedArtifact) {
        // IndexedArtifact i = (IndexedArtifact) element;
        if(IndexManager.SEARCH_CLASS_NAME.equals(queryType)) {
          return MavenPlugin.getImage("icons/java_obj.gif");
        }
        return MavenPlugin.getImage("icons/jar_obj.gif");
      }
      return null;
    }

  }

  public static class SearchResultContentProvider implements ITreeContentProvider {
    private static Object[] EMPTY = new Object[0];

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public Object[] getElements(Object inputElement) {
      if(inputElement instanceof Map) {
        return ((Map) inputElement).values().toArray();
      }
      return EMPTY;
    }

    public Object[] getChildren(Object parentElement) {
      if(parentElement instanceof IndexedArtifact) {
        IndexedArtifact a = (IndexedArtifact) parentElement;
        return a.files.toArray();
      }
      return EMPTY;
    }

    public boolean hasChildren(Object element) {
      return element instanceof IndexedArtifact;
    }

    public Object getParent(Object element) {
      return null;
    }

    public void dispose() {
    }

  }

}

