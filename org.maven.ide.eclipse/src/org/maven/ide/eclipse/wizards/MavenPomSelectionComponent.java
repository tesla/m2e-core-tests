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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.apache.lucene.search.BooleanQuery;

import org.maven.ide.eclipse.MavenImages;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.index.IndexInfo;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;

/**
 * MavenPomSelectionComposite
 *
 * @author Eugene Kuleshov
 */
public class MavenPomSelectionComponent extends Composite {

  /* (non-Javadoc)
   * @see org.eclipse.swt.widgets.Widget#dispose()
   */
  public void dispose() {
    if(searchJob != null){
      searchJob.cancel();
    }
    super.dispose();
  }

  Text searchText = null;

  TreeViewer searchResultViewer = null;
  
  Button javadocCheckBox;
  Button sourcesCheckBox;
  Button testCheckBox;

  /**
   * One of 
   *   {@link IndexManager#SEARCH_ARTIFACT}, 
   *   {@link IndexManager#SEARCH_CLASS_NAME}, 
   */
  String queryType;
  
  SearchJob searchJob;

  private IStatus status;

  private ISelectionChangedListener selectionListener;
  
  public static final String P_SEARCH_INCLUDE_JAVADOC = "searchIncludesJavadoc";
  public static final String P_SEARCH_INCLUDE_SOURCES = "searchIncludesSources";  
  public static final String P_SEARCH_INCLUDE_TESTS = "searchIncludesTests";
  private static final long SHORT_DELAY = 150L;
  private static final long LONG_DELAY = 500L;
  
  HashSet<String> artifactKeys = new HashSet<String>();

  public MavenPomSelectionComponent(Composite parent, int style) {
    super(parent, style);
    createSearchComposite();
  }
  
  private void createSearchComposite() {
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    setLayout(gridLayout);
    
    Label searchTextlabel = new Label(this, SWT.NONE);
    searchTextlabel.setText("&Enter groupId, artifactId or sha1 prefix or pattern (*):");
    searchTextlabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

    searchText = new Text(this, SWT.BORDER);
    searchText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
    searchText.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if(e.keyCode == SWT.ARROW_DOWN) {
          searchResultViewer.getTree().setFocus();
        }
      }
    });
    searchText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        scheduleSearch(searchText.getText(), true);
      }
    });

    Label searchResultsLabel = new Label(this, SWT.NONE);
    searchResultsLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
    searchResultsLabel.setText("&Search Results:");

    Tree tree = new Tree(this, SWT.BORDER | SWT.SINGLE);
    tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    tree.setData("name", "searchResultTree");

    searchResultViewer = new TreeViewer(tree);
    
  }
  
  protected boolean showClassifiers(){
    return (queryType != null && IndexManager.SEARCH_ARTIFACT.equals(queryType));
  }
  
  private void setupButton(final Button button, String label, final String prefName, int horizontalIndent){
    button.setText(label);
    GridData gd = new GridData(SWT.LEFT, SWT.TOP, false, false);
    gd.horizontalIndent=horizontalIndent;
    button.setLayoutData(gd);
    boolean check = MavenPlugin.getDefault().getPreferenceStore().getBoolean(prefName);
    button.setSelection(check);
    button.addSelectionListener(new SelectionAdapter(){
      public void widgetSelected(SelectionEvent e){
        boolean checked = button.getSelection();
        MavenPlugin.getDefault().getPreferenceStore().setValue(prefName, checked);
        scheduleSearch(searchText.getText(), false);
      }
    });
  }
  
  public void init(String queryText, String queryType, Set<ArtifactKey> artifacts) {
    this.queryType = queryType;
    
    if(queryText != null) {
      searchText.setText(queryText);
    }
    
    if(artifacts!=null) {
      for(ArtifactKey a : artifacts) {
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
            // int severity = artifactKeys.contains(f.group + ":" + f.artifact) ? IStatus.ERROR : IStatus.OK;
            int severity = IStatus.OK;
            setStatus(severity, f.fname + " " + f.size + " " + f.date);
          } else {
            setStatus(IStatus.OK, "Selected " + selection.size());
          }
        } else {
          setStatus(IStatus.ERROR, "No selection");
        }
      }
    });
    setupClassifiers();
    setStatus(IStatus.ERROR, "");
    scheduleSearch(queryText, false);
  }
  
  protected void setupClassifiers(){
    if(showClassifiers()){
      Composite includesComp = new Composite(this, SWT.NONE);
      includesComp.setLayout(new GridLayout(3, true));
      GridData gd = new GridData(SWT.LEFT, SWT.TOP, true, false);
      includesComp.setLayoutData(gd);
      
      javadocCheckBox = new Button(includesComp, SWT.CHECK);
      setupButton(javadocCheckBox, "Include Javadocs", P_SEARCH_INCLUDE_JAVADOC,0);
      
      sourcesCheckBox = new Button(includesComp, SWT.CHECK);
      setupButton(sourcesCheckBox, "Include Sources", P_SEARCH_INCLUDE_SOURCES,10);
  
      testCheckBox = new Button(includesComp, SWT.CHECK);
      setupButton(testCheckBox, "Include Tests", P_SEARCH_INCLUDE_TESTS,10);
    }
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
    this.status = new Status(severity, IMavenConstants.PLUGIN_ID, 0, message, null);
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
      return ((IndexedArtifact) element).files.iterator().next();
    }
    return (IndexedArtifactFile) element;
  }

  void scheduleSearch(String query, boolean delay) {
    if(query != null && query.length() > 2) {
      if(searchJob == null) {
        IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
        searchJob = new SearchJob(queryType, indexManager);
      } else {
        if(searchJob.isWaiting()){
          searchJob.cancel();
          IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
          searchJob = new SearchJob(queryType, indexManager);
        }
      }
      searchJob.setQuery(query.toLowerCase());
      searchJob.setWaiting();
      if(!searchJob.isRunning()) {
        searchJob.schedule(delay ? LONG_DELAY : SHORT_DELAY);
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
    
    boolean isWaiting = false;

    public SearchJob(String field, IndexManager indexManager) {
      super("Repository search");
      this.field = field;
      this.indexManager = indexManager;
      this.isWaiting = true;
    }

    public boolean isRunning() {
      return isRunning;
    }
    
    public boolean isWaiting(){
      return this.isWaiting;
    }

    public void setQuery(String query) {
      this.query = query;
    }
    
    public void setWaiting(){
      this.isWaiting = true;
    }
    
    public int getClassifier(){
      int classifier = IndexManager.SEARCH_JARS;
      if(MavenPlugin.getDefault().getPreferenceStore().getBoolean(P_SEARCH_INCLUDE_JAVADOC)){
        classifier = classifier + IndexManager.SEARCH_JAVADOCS;
      }
      if(MavenPlugin.getDefault().getPreferenceStore().getBoolean(P_SEARCH_INCLUDE_SOURCES)){
        classifier = classifier + IndexManager.SEARCH_SOURCES;
      }
      if(MavenPlugin.getDefault().getPreferenceStore().getBoolean(P_SEARCH_INCLUDE_TESTS)){
        classifier = classifier + IndexManager.SEARCH_TESTS;
      }
      return classifier;
    }
    
    protected IStatus run(IProgressMonitor monitor) {
      isRunning = true;
      isWaiting = false;
      
      int classifier = showClassifiers() ? getClassifier() : IndexManager.SEARCH_ALL;
      if(searchResultViewer == null || searchResultViewer.getControl() == null || searchResultViewer.getControl().isDisposed()){
        return Status.CANCEL_STATUS;
      }
      while(!monitor.isCanceled() && query != null) {
        String activeQuery = query;
        query = null;
        try {
          setResult(IStatus.OK, "Searching \'" + activeQuery.toLowerCase() + "\'...", null);
          Map<String, IndexedArtifact> res = indexManager.search(activeQuery, field, classifier);
          if(IndexManager.SEARCH_CLASS_NAME.equals(field) && activeQuery.length() < IndexManager.MIN_CLASS_QUERY_LENGTH) {
            setResult(IStatus.WARNING, "Query '" + activeQuery + "' is too short", Collections.<String, IndexedArtifact>emptyMap());
          } else {
            setResult(IStatus.OK, "Results for '" + activeQuery + "' (" + res.size() + ")", res);
          }
        } catch(BooleanQuery.TooManyClauses ex){
          setResult(IStatus.ERROR, "Too many results to display. Enter a more specific search term.", Collections.<String, IndexedArtifact>emptyMap());
        } catch(final RuntimeException ex) {
          setResult(IStatus.ERROR, "Search error: " + ex.toString(), Collections.<String, IndexedArtifact>emptyMap());
        } catch(final Exception ex) {
          setResult(IStatus.ERROR, "Search error: " + ex.getMessage(), Collections.<String, IndexedArtifact>emptyMap());
        }
      } 
      isRunning = false;
      return Status.OK_STATUS;
    }

    private void setResult(final int severity, final String message, final Map<String, IndexedArtifact> result) {
      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          setStatus(severity, message);
          if(result != null) {
            if(!searchResultViewer.getControl().isDisposed()){
              searchResultViewer.setInput(result);
            }
          }
        }
      });
    }

  }

  public static class SearchResultLabelProvider extends LabelProvider implements IColorProvider {
    private final Set<String> artifactKeys;
    private final String queryType;

    public SearchResultLabelProvider(Set<String> artifactKeys, String queryType) {
      this.artifactKeys = artifactKeys;
      this.queryType = queryType;
    }

    public String getText(Object element) {
      if(element instanceof IndexedArtifact) {
        IndexedArtifact a = (IndexedArtifact) element;
        String name = (a.className == null ? "" : a.className + "   " + a.getPackageName() + "   ") + a.group + "   " + a.artifact;
        return name;
      } else if(element instanceof IndexedArtifactFile) {
        IndexedArtifactFile f = (IndexedArtifactFile) element;
        long size_k = (f.size + 512)/1024;
        String displayName = getRepoDisplayName(f.repository);
        return f.version + " - " + f.fname + " - " + size_k + "K - " + f.date + " [" + displayName + "]";
      }
      return super.getText(element);
    }
    
    protected String getRepoDisplayName(String repo){
      try{
        IndexInfo info = MavenPlugin.getDefault().getIndexManager().getIndexInfo(repo);
        if(info != null && info.getDisplayName() != null){
          return info.getDisplayName();
        }
        return repo;
      } catch(Throwable t){
        return repo;
      }
    }
    
    public Color getForeground(Object element) {
      if(element instanceof IndexedArtifactFile) {
        IndexedArtifactFile f = (IndexedArtifactFile) element;
        if(artifactKeys.contains(f.group + ":" + f.artifact + ":" + f.version)) {
          return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
        }
      } else if(element instanceof IndexedArtifact) {
        IndexedArtifact i = (IndexedArtifact) element;
        if(artifactKeys.contains(i.group + ":" + i.artifact)) {
          return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
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
            return MavenImages.IMG_JAVA_SRC;
          }
          return MavenImages.IMG_JAVA;
        }
        if(f.sourcesExists==IndexManager.PRESENT) {
          return MavenImages.IMG_VERSION_SRC;
        }
        return MavenImages.IMG_VERSION;
      } else if(element instanceof IndexedArtifact) {
        // IndexedArtifact i = (IndexedArtifact) element;
        if(IndexManager.SEARCH_CLASS_NAME.equals(queryType)) {
          return MavenImages.IMG_JAVA;
        }
        return MavenImages.IMG_JAR;
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
        return ((Map<?, ?>) inputElement).values().toArray();
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

