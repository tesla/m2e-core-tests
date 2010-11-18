/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.dialogs;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.ibm.icu.text.DateFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import org.apache.lucene.search.BooleanQuery;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexManager;
import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.util.ProposalUtil;
import org.eclipse.m2e.core.util.search.Packaging;
import org.eclipse.m2e.core.wizards.MavenPomSelectionComponent;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.PomFactory;


/**
 * A Dialog whose primary goal is to allow the user to select a dependency, either by entering the GAV coordinates
 * manually, or by search through a repository index.
 * 
 * @author rgould
 */
public class AddDependencyDialog extends AbstractMavenDialog {

  protected static final String[] SCOPES = new String[] {"compile", "provided", "runtime", "test", "system"};

  /*
   * dependencies under dependencyManagement are permitted to use an the extra "import" scope
   */
  protected static final String[] DEP_MANAGEMENT_SCOPES = new String[] {"compile", "provided", "runtime", "test",
      "system", "import"};

  protected static final String DIALOG_SETTINGS = AddDependencyDialog.class.getName();

  protected static final long SEARCH_DELAY = 500L; //in milliseconds

  protected String[] scopes;

  protected TreeViewer resultsViewer;

  protected Text queryText;

  protected Text groupIDtext;

  protected Text artifactIDtext;

  protected Text versionText;

  protected Text infoTextarea;

  protected List scopeList;

  protected java.util.List<Dependency> dependencies;

  /*
   * Stores selected files from the results viewer. These are later
   * converted into the above dependencies when OK is pressed.
   */
  protected java.util.List<IndexedArtifactFile> artifactFiles;

  protected SearchJob currentSearch;

  protected IProject project;

  protected DependencyNode dependencyNode;

  /*
   * This is to be run when the dialog is done creating its controls, but
   * before open() is called
   */
  protected Runnable onLoad;

  /**
   * The AddDependencyDialog differs slightly in behaviour depending on context. If it is being used to apply a
   * dependency under the "dependencyManagement" context, the extra "import" scope is available. Set @param
   * isForDependencyManagement to true if this is case.
   * 
   * @param parent
   * @param isForDependencyManagement
   * @param project the project which contains this POM. Used for looking up indices
   */
  public AddDependencyDialog(Shell parent, boolean isForDependencyManagement, IProject project) {
    super(parent, DIALOG_SETTINGS);
    this.project = project;

    setShellStyle(getShellStyle() | SWT.RESIZE);
    setTitle("Add Dependency");

    if(!isForDependencyManagement) {
      this.scopes = SCOPES;
    } else {
      this.scopes = DEP_MANAGEMENT_SCOPES;
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.Dialog#createDialogArea()
   */
  protected Control createDialogArea(Composite parent) {
    readSettings();

    Composite composite = (Composite) super.createDialogArea(parent);

    Composite gavControls = createGAVControls(composite);
    gavControls.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    Composite searchControls = createSearchControls(composite);
    searchControls.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    Display.getDefault().asyncExec(this.onLoad);
    
    return composite;
  }

  /**
   * Sets the up group-artifact-version controls
   */
  private Composite createGAVControls(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridData gridData = null;

    GridLayout gridLayout = new GridLayout(4, false);
    composite.setLayout(gridLayout);

    Label groupIDlabel = new Label(composite, SWT.NONE);
    groupIDlabel.setText("Group ID:");

    groupIDtext = new Text(composite, SWT.BORDER);
    groupIDtext.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    Label scopeLabel = new Label(composite, SWT.NONE);
    scopeLabel.setText("Scope: ");
    gridData = new GridData();
    gridData.verticalSpan = 3;
    gridData.verticalAlignment = SWT.TOP;
    scopeLabel.setLayoutData(gridData);

    scopeList = new List(composite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
    scopeList.setItems(scopes);
    gridData = new GridData();
    gridData.grabExcessVerticalSpace = true;
    gridData.verticalAlignment = SWT.TOP;
    gridData.verticalSpan = 3;
    scopeList.setLayoutData(gridData);
    scopeList.setSelection(0);

    Label artifactIDlabel = new Label(composite, SWT.NONE);
    artifactIDlabel.setText("Artifact ID:");

    artifactIDtext = new Text(composite, SWT.BORDER);
    artifactIDtext.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    Label versionLabel = new Label(composite, SWT.NONE);
    versionLabel.setText("Version: ");
    versionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

    versionText = new Text(composite, SWT.BORDER);
    versionText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

    ProposalUtil.addGroupIdProposal(project, groupIDtext, Packaging.ALL);
    ProposalUtil.addArtifactIdProposal(project, groupIDtext, artifactIDtext, Packaging.ALL);
    ProposalUtil.addVersionProposal(project, groupIDtext, artifactIDtext, versionText, Packaging.ALL);
    
    artifactIDtext.addModifyListener(new ModifyListener() {
      
      public void modifyText(ModifyEvent e) {
        updateInfo();
      }
    });
    
    groupIDtext.addModifyListener(new ModifyListener() {
      
      public void modifyText(ModifyEvent e) {
        updateInfo();
      }
    });

    return composite;
  }
  
  void updateInfo() {
    infoTextarea.setText("");
    if (dependencyNode == null) {
      return;
    }
    dependencyNode.accept(new DependencyVisitor() {
      
      public boolean visitLeave(DependencyNode node) {
        if (node.getDependency() != null && node.getDependency().getArtifact() != null) {
          Artifact artifact = node.getDependency().getArtifact();
          if (artifact.getGroupId().equalsIgnoreCase(groupIDtext.getText().trim()) 
              && artifact.getArtifactId().equalsIgnoreCase(artifactIDtext.getText().trim())) {
            infoTextarea.setText(artifact.getGroupId() + "-" + artifact.getArtifactId() + "-" + artifact.getVersion()
                + " is already a transitive dependency.\n");
          }
          return false;
        }
        return true;
      }
      
      public boolean visitEnter(DependencyNode node) {
        return true;
      }
    });
    
  }

  private Composite createSearchControls(Composite parent) {
    SashForm sashForm = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);
    sashForm.setLayout(new FillLayout());

    Composite resultsComposite = new Composite(sashForm, SWT.NONE);
    FormData data = null;

    resultsComposite.setLayout(new FormLayout());

    Label queryLabel = new Label(resultsComposite, SWT.NONE);
    queryLabel.setText("Query:");
    data = new FormData();
    data.left = new FormAttachment(0, 0);
    queryLabel.setLayoutData(data);

    queryText = new Text(resultsComposite, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH);
    data = new FormData();
    data.left = new FormAttachment(10, 0);
    data.right = new FormAttachment(100, -5);
    queryText.setLayoutData(data);

    Label hint = new Label(resultsComposite, SWT.NONE);
    hint.setText("(coordinate, sha1 prefix, project name)");
    data = new FormData();
    data.left = new FormAttachment(10, 0);
    data.top = new FormAttachment(queryText, 5);
    hint.setLayoutData(data);

    Label resultsLabel = new Label(resultsComposite, SWT.NONE);
    resultsLabel.setText("Results:");
    data = new FormData();
    data.left = new FormAttachment(0, 0);
    data.top = new FormAttachment(hint, 5);
    resultsLabel.setLayoutData(data);

    Tree resultsTree = new Tree(resultsComposite, SWT.MULTI | SWT.BORDER);
    data = new FormData();
    data.left = new FormAttachment(10, 0);
    data.top = new FormAttachment(hint, 5);
    data.right = new FormAttachment(100, -5);
    data.bottom = new FormAttachment(100, -5);
    resultsTree.setLayoutData(data);

    Composite infoComposite = new Composite(sashForm, SWT.NONE);
    infoComposite.setLayout(new FormLayout());

    Label infoLabel = new Label(infoComposite, SWT.NONE);
    FormData formData = new FormData();
    formData.left = new FormAttachment(0, 0);
    infoLabel.setLayoutData(formData);
    infoLabel.setText("Info: ");

    infoTextarea = new Text(infoComposite, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    formData = new FormData();
    formData.left = new FormAttachment(10, 0);
    formData.bottom = new FormAttachment(100, -5);
    formData.top = new FormAttachment(0, 0);
    formData.right = new FormAttachment(100, -5);
    infoTextarea.setLayoutData(formData);

    sashForm.setWeights(new int[] {70, 30});

    /*
     * Set up TreeViewer for search results
     */

    resultsViewer = new TreeViewer(resultsTree);
    resultsViewer.setContentProvider(new MavenPomSelectionComponent.SearchResultContentProvider());
    resultsViewer.setLabelProvider(new MavenPomSelectionComponent.SearchResultLabelProvider(Collections.EMPTY_SET,
        IIndex.SEARCH_ARTIFACT));

    /*
     * Hook up events
     */

    resultsViewer.addSelectionChangedListener(new ISelectionChangedListener() {

      public void selectionChanged(SelectionChangedEvent event) {
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        if(selection.isEmpty()) {
          infoTextarea.setText("");
          artifactFiles = null;
        } else {
          String artifact = null;
          String group = null;
          String version = null;

          artifactFiles = new LinkedList<IndexedArtifactFile>();
          StringBuffer buffer = new StringBuffer();
          Iterator iter = selection.iterator();
          while(iter.hasNext()) {
            Object obj = iter.next();
            IndexedArtifactFile file = null;
            
            if(obj instanceof IndexedArtifact) {
              file = ((IndexedArtifact) obj).getFiles().iterator().next();
            } else {
              file = (IndexedArtifactFile) obj;
            }
            
            appendFileInfo(buffer, file);
            artifactFiles.add(file);
            
            artifact = chooseWidgetText(artifact, file.artifact);
            group = chooseWidgetText(group, file.group);
            version = chooseWidgetText(version, file.version);
          }
          setInfo(OK, artifactFiles.size() + " items selected.");
          infoTextarea.setText(buffer.toString());
          artifactIDtext.setText(artifact);
          groupIDtext.setText(group);
          versionText.setText(version);

          boolean enabled = !(artifactFiles.size() > 1);
          artifactIDtext.setEnabled(enabled);
          groupIDtext.setEnabled(enabled);
          versionText.setEnabled(enabled);
        }
      }
    });

    queryText.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if(e.keyCode == SWT.ARROW_DOWN) {
          resultsViewer.getTree().setFocus();
        }
      }
    });

    queryText.addModifyListener(new ModifyListener() {

      public void modifyText(ModifyEvent e) {
        search(queryText.getText());
      }
    });

    return sashForm;
  }

  /**
   * Just a short helper method to determine what to display in the text widgets when the user selects multiple objects
   * in the tree viewer. If the objects have the same value, then we should show that to them, otherwise we show
   * something like "(multiple selected)"
   * 
   * @param current
   * @param newValue
   * @return
   */
  String chooseWidgetText(String current, String newValue) {
    if(current == null) {
      return newValue;
    } else if(!current.equals(newValue)) {
      return "(multiple values selected)";
    }
    return current;
  }

  void appendFileInfo(final StringBuffer buffer, final IndexedArtifactFile file) {
    buffer.append(" * " + file.fname);
    if(file.size != -1) {
      buffer.append(", size: ");
      if((file.size / 1024 / 1024) > 0) {
        buffer.append((file.size / 1024 / 1024) + "MB");
      } else {
        buffer.append(Math.max(1, file.size / 1024) + "KB");
      }
    }
    buffer.append(", date: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(file.date));
    buffer.append("\n");

    if(dependencyNode != null) {
      dependencyNode.accept(new DependencyVisitor() {

        public boolean visitEnter(DependencyNode node) {
          return true;
        }

        public boolean visitLeave(DependencyNode node) {
          if (node.getDependency() == null || node.getDependency().getArtifact() == null) {
            return true;
          }
          Artifact artifact = node.getDependency().getArtifact();
          if(artifact.getGroupId().equalsIgnoreCase(file.group) && artifact.getArtifactId().equalsIgnoreCase(file.artifact)) {
            buffer.append("  + " + artifact.getGroupId() + "-" + artifact.getArtifactId() + "-" + artifact.getVersion()
                + " is already a transitive dependency.\n");
            /*
             * DependencyNodes don't know their parents. Determining which transitive dependency 
             * is using the selected dependency is non trivial :(
             */
            return false;
          }
          return true;
        }
      });
    }
  }

  protected void search(String query) {
    if(query == null || query.length() <= 2) {
      if(this.currentSearch != null) {
        this.currentSearch.cancel();
      }
    } else {
      IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();

      if(this.currentSearch != null) {
        this.currentSearch.cancel();
      }

      this.currentSearch = new SearchJob(query.toLowerCase(), indexManager);
      this.currentSearch.schedule(SEARCH_DELAY);
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
   * This is called when OK is pressed. There's no obligation to do anything.
   */
  protected void computeResult() {
    String scope = "";
    if(scopeList.getSelection().length != 0) {
      scope = scopeList.getSelection()[0];
    }

    if(artifactFiles == null || artifactFiles.size() == 1) {
      Dependency dependency = createDependency(groupIDtext.getText().trim(), artifactIDtext.getText().trim(),
          versionText.getText().trim(), scope, "");
      this.dependencies = Collections.singletonList(dependency);
    } else {
      this.dependencies = new LinkedList<Dependency>();
      for(IndexedArtifactFile file : artifactFiles) {
        Dependency dep = createDependency(file.group, file.artifact, file.version, scope, file.type);
        this.dependencies.add(dep);
      }
    }
  }

  private Dependency createDependency(String groupID, String artifactID, String version, String scope, String type) {
    Dependency dependency = PomFactory.eINSTANCE.createDependency();
    dependency.setGroupId(groupID);
    dependency.setArtifactId(artifactID);
    dependency.setVersion(version);

    /*
     * For scope and type, if the values are the default, don't save them.
     * This reduces clutter in the XML file (although forces people who don't
     * know what the defaults are to look them up).
     */
    dependency.setScope("compile".equals(scope) ? "" : scope);
    dependency.setType("jar".equals(type) ? "" : type);

    return dependency;
  }

  public java.util.List<Dependency> getDependencies() {
    return this.dependencies;
  }

  void setInfo(int status, String message) {
    updateStatus(new Status(status, IMavenConstants.PLUGIN_ID, message));
  }

  private class SearchJob extends Job {

    private String query;

    private IndexManager indexManager;

    private boolean cancelled = false;

    public SearchJob(String query, IndexManager indexManager) {
      super("Searching for " + query);
      this.query = query;
      this.indexManager = indexManager;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    protected IStatus run(IProgressMonitor monitor) {
      if(this.cancelled || resultsViewer == null || resultsViewer.getControl() == null
          || resultsViewer.getControl().isDisposed()) {
        return Status.CANCEL_STATUS;
      }

      try {
        setResults(IStatus.OK, "Searching...", Collections.<String, IndexedArtifact> emptyMap());
        Map<String, IndexedArtifact> results = indexManager.search(query, IIndex.SEARCH_ARTIFACT, IIndex.SEARCH_ALL);
        setResults(IStatus.OK, "Done. " + results.size() + " results found.", results);
      } catch(BooleanQuery.TooManyClauses exception) {
        setResults(IStatus.ERROR, "Too many results. Please refine your search.",
            Collections.<String, IndexedArtifact> emptyMap());
      } catch(RuntimeException exception) {
        setResults(IStatus.ERROR, "Error while searching: " + exception.toString(),
            Collections.<String, IndexedArtifact> emptyMap());
      } catch(CoreException ex) {
        setResults(IStatus.ERROR, "Error while searching: " + ex.getMessage(),
            Collections.<String, IndexedArtifact> emptyMap());
        MavenLogger.log(ex);
      }

      return Status.OK_STATUS;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.jobs.Job#canceling()
     */
    protected void canceling() {
      this.cancelled = true;
      super.canceling();
    }

    private void setResults(final int status, final String infoMessage, final Map<String, IndexedArtifact> results) {
      if(cancelled) {
        return;
      }

      Display.getDefault().syncExec(new Runnable() {

        public void run() {
          setInfo(status, infoMessage);
          if(results != null && resultsViewer != null && resultsViewer.getControl() != null
              && !resultsViewer.getControl().isDisposed()) {
            resultsViewer.setInput(results);
          }
        }
      });
    }

  }

  public void setDepdencyNode(DependencyNode node) {
    this.dependencyNode = node;
  }

  /**
   * The provided runnable will be called after createDialogArea is done,
   * but before it returns. This provides a way for long running operations to 
   * be executed in such a way as to not block the UI.
   * 
   * This is primarily intended to allow the loading of the dependencyTree.
   * The runnable should load the tree and then call setDependencyNode()
   * 
   * @param runnable
   */
  public void onLoad(Runnable runnable) {
    this.onLoad = runnable;
  }
}
