/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.launch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.core.Messages;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;


public class MavenGoalSelectionDialog extends ElementTreeSelectionDialog {

  Button isQualifiedNameButton;
  
  boolean isQualifiedName = true;

  public MavenGoalSelectionDialog(Shell parent) {
    super(parent, new GoalsLabelProvider(), new GoalsContentProvider());

    setTitle(Messages.getString("launch.goalsDialog.title"));
    setMessage("Select goal:");
    setValidator(new GoalsSelectionValidator());
    setInput(new Object());
  }

  protected Control createDialogArea(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

    Label selectGoalLabel = new Label(composite, SWT.NONE);
    selectGoalLabel.setText("&Select Goal:");

    final GoalsFilter filter = new GoalsFilter();

    final Text filterText = new Text(composite, SWT.BORDER);
    GridData gd_filterText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_filterText.widthHint = 200;
    filterText.setLayoutData(gd_filterText);
    filterText.setFocus();
    
    final TreeViewer treeViewer = createTreeViewer(composite);
    treeViewer.addFilter(filter);
    
    GridData data = new GridData(GridData.FILL_BOTH);
    data.widthHint = 500;
    data.heightHint = 400;
    // data.widthHint = convertWidthInCharsToPixels(fWidth);
    // data.heightHint = convertHeightInCharsToPixels(fHeight);

    final Tree tree = treeViewer.getTree();
    tree.setLayoutData(data);
    tree.setFont(parent.getFont());

    filterText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        String text = filterText.getText();
        filter.setFilter(text);
        treeViewer.refresh();
        if(text.trim().length()==0) {
          treeViewer.collapseAll();
        } else {
          treeViewer.expandAll();
        }
      }
    });

    filterText.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if(e.keyCode==SWT.ARROW_DOWN) {
          tree.setFocus();
          tree.setSelection(tree.getTopItem().getItem(0));

          Object[] elements = ((ITreeContentProvider) treeViewer.getContentProvider()).getElements(null);
          treeViewer.setSelection(new StructuredSelection(elements[0]));
        }
        
      }
    });

    isQualifiedNameButton = new Button(composite, SWT.CHECK);
    isQualifiedNameButton.setText("Use &Qualified Name");
    isQualifiedNameButton.setSelection(true);
    isQualifiedNameButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        isQualifiedName = isQualifiedNameButton.getSelection();
      }
    });
    
//    if (fIsEmpty) {
//        messageLabel.setEnabled(false);
//        treeWidget.setEnabled(false);
//    }

    return composite;
  }
  
  public boolean isQualifiedName() {
    return isQualifiedName;
  }

  /**
   * GoalsContentProvider
   */
  static class GoalsContentProvider implements ITreeContentProvider {
    private static Object[] EMPTY = new Object[0];

    private final List<Group> groups = new ArrayList<Group>();

    public GoalsContentProvider() {
      MavenEmbedderManager embedderManager = MavenPlugin.getDefault().getMavenEmbedderManager();
      try {
        List<Entry> entries = new ArrayList<Entry>();
        @SuppressWarnings("unchecked")
        List<String> phases = embedderManager.getWorkspaceEmbedder().getLifecyclePhases();
        for(int i = 0; i < phases.size(); i++ ) {
          entries.add(new Entry(phases.get(i), null, null));
        }
        groups.add(new Group(Messages.getString("launch.goalsDialog.lifecyclePhases"), null, null, entries)); //$NON-NLS-1$
      } catch(Exception e) {
        MavenLogger.log("Unable to get lifecycle phases", e);
      }

      IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
      try {
        Map<String, IndexedArtifact> result = indexManager.search("*", IndexManager.SEARCH_PLUGIN);
        TreeMap<String, Group> map = new TreeMap<String, Group>();
        for(IndexedArtifact a : result.values()) {
          IndexedArtifactFile f = a.files.iterator().next();
          if(f.prefix != null && f.prefix.length()>0 && f.goals != null) {
            List<Entry> goals = new ArrayList<Entry>();
            for(String goal : f.goals) {
              if(goal.length()>0) {
                goals.add(new Entry(goal, f.prefix, f));
              }
            }
            if(goals.size()>0) {
              map.put(f.prefix + ":" + f.group, new Group(f.prefix, f.group, f.artifact, goals));
            }
          }
        }
        groups.addAll(map.values());
      } catch(CoreException e) {
        MavenLogger.log(e);
      }
    }

    public Object[] getElements(Object inputElement) {
      return groups.toArray();
    }

    public Object[] getChildren(Object parent) {
      if(parent instanceof Group) {
        return ((Group) parent).entries.toArray();
      }
      return EMPTY;
    }

    public boolean hasChildren(Object element) {
      return element instanceof Group;
    }

    public Object getParent(Object element) {
      return null;
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public void dispose() {
    }

  }

  /**
   * GoalsLabelProvider
   */
  static class GoalsLabelProvider extends LabelProvider {
    public String getText(Object element) {
      if(element instanceof Group) {
        Group g = (Group) element;
        if(g.groupId==null) {
          return g.name;
        }
        return g.name + " - " + g.groupId + ":" + g.artifactId;
        
      } else if(element instanceof Entry) {
        return ((Entry) element).name;
        
      }
      return super.getText(element);
    }
  }

  /**
   * GoalsFilter
   */
  static class GoalsFilter extends ViewerFilter {
    private String filter;
  
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(filter == null || filter.trim().length() == 0) {
        return true;
      }
      if(element instanceof Group) {
        Group g = (Group) element;
        if(g.name.indexOf(filter) > -1) {
          return true;
        }
        for(Iterator<Entry> it = g.entries.iterator(); it.hasNext();) {
          Entry e = it.next();
          if(e.name.indexOf(filter) > -1) {
            return true;
          }
        }
  
      } else if(element instanceof Entry) {
        Entry e = (Entry) element;
        return e.name.indexOf(filter) > -1 || (e.prefix!=null && e.prefix.indexOf(filter) > -1);

      }
      return false;
    }

    public void setFilter(String filter) {
      this.filter = filter;
    }
  }

  /**
   * GoalsSelectionValidator
   */
  static class GoalsSelectionValidator implements ISelectionStatusValidator {
    public IStatus validate(Object[] selection) {
      if(selection.length == 0) {
        return new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Should select at least one goal", null);
      }
      for(int j = 0; j < selection.length; j++ ) {
        if(selection[j] instanceof Entry) {
          continue;
        }
        return new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "", null);
      }
      return Status.OK_STATUS;
    }
  }

  /**
   * Group
   */
  static class Group {
    public final String name;
    public final String groupId;
    public final String artifactId;

    public final List<Entry> entries;

    public Group(String name, String groupId, String artifactId, List<Entry> entries) {
      this.name = name;
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.entries = entries;
    }
  }

  /**
   * Entry
   */
  public static class Entry {
    public final String prefix;

    public final String name;

    private final IndexedArtifactFile f;

    public Entry(String name, String prefix, IndexedArtifactFile f) {
      this.prefix = prefix;
      this.name = name;
      this.f = f;
    }

    public String getName() {
      return prefix == null ? name : prefix + ":" + name;
    }

    public String getQualifiedName() {
      // return prefix == null ? name : prefix + ":" + name;
      return prefix == null ? name : f.group + ":" + f.artifact + ":" + f.version + ":" + name;
    }
  
  }

}
