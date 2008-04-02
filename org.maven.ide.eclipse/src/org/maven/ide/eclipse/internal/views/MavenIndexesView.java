/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.MaterializeAction;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.index.IndexInfo;
import org.maven.ide.eclipse.index.IndexListener;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.index.IndexedArtifactGroup;
import org.maven.ide.eclipse.index.IndexInfo.Type;
import org.maven.ide.eclipse.internal.index.IndexUnpackerJob;


/**
 * Maven indexes view
 * 
 * @author Eugene Kuleshov
 */
public class MavenIndexesView extends ViewPart {

  IAction refreshAction;

  IAction collapseAllAction;

  IAction addIndexAction;

  BaseSelectionListenerAction openPomAction;

  BaseSelectionListenerAction updateAction;

  BaseSelectionListenerAction unpackAction;

  BaseSelectionListenerAction copyUrlAction;

  BaseSelectionListenerAction removeIndexAction;

  BaseSelectionListenerAction editIndexAction;

  BaseSelectionListenerAction materializeProjectAction;
  
  TreeViewer viewer;

  private DrillDownAdapter drillDownAdapter;

  public void setFocus() {
    viewer.getControl().setFocus();
  }

  public void createPartControl(Composite parent) {
    viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    viewer.setContentProvider(new ViewContentProvider());
    viewer.setLabelProvider(new ViewLabelProvider());
    viewer.setSorter(new ViewerSorter() {
      public int compare(Viewer viewer, Object o1, Object o2) {
        if(o1 instanceof IndexInfo && o2 instanceof IndexInfo) {
          IndexInfo i1 = (IndexInfo) o1;
          IndexInfo i2 = (IndexInfo) o2;
          int t1 = getType(i1.getType());
          int t2 = getType(i2.getType());
          if(t1!=t2) {
            return t1-t2;
          }
          return i1.getIndexName().compareTo(i2.getIndexName());
        }
        return super.compare(viewer, o1, o2);
      }

      private int getType(Type type) {
        if(type==IndexInfo.Type.WORKSPACE) {
          return 0;
        } else if(type==IndexInfo.Type.LOCAL) {
          return 1;
        }
        return 2;
      }
    });
    viewer.setInput(getViewSite());
    viewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        if(editIndexAction.isEnabled()) {
          editIndexAction.run();
        }
      }
    });

    drillDownAdapter = new DrillDownAdapter(viewer);

    makeActions();
    hookContextMenu();

    viewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        openPomAction.run();
      }
    });

    contributeToActionBars();

    IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
    indexManager.addIndexListener(new IndexListener() {

      public void indexAdded(IndexInfo info) {
        refreshView();
      }

      public void indexChanged(IndexInfo info) {
        refreshView();
      }

      public void indexRemoved(IndexInfo info) {
        refreshView();
      }
    });
  }

  private void hookContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu");
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        MavenIndexesView.this.fillContextMenu(manager);
      }
    });

    Menu menu = menuMgr.createContextMenu(viewer.getControl());
    viewer.getControl().setMenu(menu);
    getSite().registerContextMenu(menuMgr, viewer);
  }

  private void contributeToActionBars() {
    IActionBars bars = getViewSite().getActionBars();
    fillLocalPullDown(bars.getMenuManager());
    fillLocalToolBar(bars.getToolBarManager());
  }

  private void fillLocalPullDown(IMenuManager manager) {
    manager.add(addIndexAction);
    manager.add(new Separator());
    manager.add(collapseAllAction);
    manager.add(refreshAction);
  }

  void fillContextMenu(IMenuManager manager) {
    manager.add(openPomAction);
    manager.add(copyUrlAction);
    manager.add(materializeProjectAction);
    manager.add(new Separator());
    manager.add(addIndexAction);
    manager.add(updateAction);
    manager.add(unpackAction);
    manager.add(removeIndexAction);
    manager.add(editIndexAction);
    manager.add(new Separator());
    drillDownAdapter.addNavigationActions(manager);
    manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
  }

  private void fillLocalToolBar(IToolBarManager manager) {
    manager.add(addIndexAction);
    manager.add(new Separator());
    manager.add(collapseAllAction);
    manager.add(refreshAction);
    manager.add(new Separator());
    drillDownAdapter.addNavigationActions(manager);
  }

  private void makeActions() {
    collapseAllAction = new Action("Collapse All") {
      public void run() {
        viewer.collapseAll();
      }
    };
    collapseAllAction.setToolTipText("Collapse All");
    collapseAllAction.setImageDescriptor(MavenPlugin.getImageDescriptor("icons/collapseall.gif"));

    refreshAction = new Action("Refresh") {
      public void run() {
        viewer.setInput(getViewSite());
      }
    };
    refreshAction.setToolTipText("Refresh View");
    refreshAction.setImageDescriptor(MavenPlugin.getImageDescriptor("icons/refresh.gif"));

    updateAction = new BaseSelectionListenerAction("Update Index") {
      public void run() {
        Object element = getStructuredSelection().getFirstElement();
        if(element instanceof IndexInfo) {
          updateIndex((IndexInfo) element);
        }
      }

      protected boolean updateSelection(IStructuredSelection selection) {
        Object element = selection.getFirstElement();
        return element instanceof IndexInfo && (IndexInfo.Type.REMOTE.equals(((IndexInfo) element).getType()) //
            || IndexInfo.Type.LOCAL.equals(((IndexInfo) element).getType()));
      }
    };
    updateAction.setToolTipText("Update repository index");
    updateAction.setImageDescriptor(MavenPlugin.getImageDescriptor("icons/update_index.gif"));

    unpackAction = new BaseSelectionListenerAction("Unpack Index") {
      public void run() {
        Object element = getStructuredSelection().getFirstElement();
        if(element instanceof IndexInfo) {
          IndexInfo indexInfo = (IndexInfo) element;
          boolean res = MessageDialog.openConfirm(getViewSite().getShell(), //
              "Unpack Index", "Replace index " + indexInfo.getIndexName() + " from the archive ");
          if(res) {
            IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
            IndexUnpackerJob unpackerJob = new IndexUnpackerJob(indexManager, //
                Collections.singleton(indexInfo));
            unpackerJob.setOverwrite(true);
            unpackerJob.schedule();
          }
        }
      }

      protected boolean updateSelection(IStructuredSelection selection) {
        Object element = selection.getFirstElement();
        if(element instanceof IndexInfo) {
          IndexInfo indexInfo = (IndexInfo) element;
          return IndexInfo.Type.REMOTE.equals(indexInfo.getType()) && indexInfo.getArchiveUrl() != null;
        }
        return false;
      }
    };
    unpackAction.setToolTipText("Unpack repository index");

    addIndexAction = new Action("Add Index") {
      public void run() {
        RepositoryIndexDialog dialog = new RepositoryIndexDialog(getSite().getShell(), "Add Repository Index",
            "icons/add_index.gif");
        int res = dialog.open();
        if(res == Window.OK) {
          IndexInfo indexInfo = dialog.getIndexInfo();
          IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
          indexManager.addIndex(indexInfo, false);
          updateIndex(indexInfo);
        }
      }
    };
    addIndexAction.setToolTipText("Add repository index");
    addIndexAction.setImageDescriptor(MavenPlugin.getImageDescriptor("icons/add_index.gif"));

    editIndexAction = new BaseSelectionListenerAction("Edit Index") {
      public void run() {
        Object element = getStructuredSelection().getFirstElement();
        if(element instanceof IndexInfo) {
          String indexName = ((IndexInfo) element).getIndexName();
          RepositoryIndexDialog dialog = new RepositoryIndexDialog(getSite().getShell(), "Edit Repository Index",
              "icons/maven_index.gif");
          dialog.setIndexInfo((IndexInfo) element);
          int res = dialog.open();
          if(res == Window.OK) {
            IndexInfo indexInfo = dialog.getIndexInfo();
            IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
            indexManager.removeIndex(indexName, false);
            indexManager.addIndex(indexInfo, false);
            updateIndex(indexInfo);
            viewer.refresh();
            viewer.setSelection(new StructuredSelection(indexInfo));
          }
        }
      }

      protected boolean updateSelection(IStructuredSelection selection) {
        Object element = selection.getFirstElement();
        return element instanceof IndexInfo && IndexInfo.Type.REMOTE.equals(((IndexInfo) element).getType());
      }
    };
    editIndexAction.setToolTipText("Add repository index");

    removeIndexAction = new BaseSelectionListenerAction("Remove Index") {
      public void run() {
        Object element = getStructuredSelection().getFirstElement();
        if(element instanceof IndexInfo) {
          IndexInfo info = (IndexInfo) element;
          if(IndexInfo.Type.REMOTE.equals(info.getType())) {
            String indexName = info.getIndexName();
            boolean res = MessageDialog.openConfirm(getViewSite().getShell(), //
                "Remove Index", "Are you sure you want to delete index " + indexName);
            if(res) {
              // TODO request index deletion
              // TODO add deleted index to 
              MavenPlugin.getDefault().getIndexManager().removeIndex(indexName, true);
            }
          }
        }
      }

      protected boolean updateSelection(IStructuredSelection selection) {
        Object element = selection.getFirstElement();
        return element instanceof IndexInfo && IndexInfo.Type.REMOTE.equals(((IndexInfo) element).getType());
      }
    };
    removeIndexAction.setToolTipText("Add repository index");
    // removeIndexAction.setImageDescriptor(MavenPlugin.getImageDescriptor("icons/add_index.gif"));

    openPomAction = new BaseSelectionListenerAction("Open POM") {
      public void run() {
        ISelection selection = viewer.getSelection();
        Object element = ((IStructuredSelection) selection).getFirstElement();
        if(element instanceof IndexedArtifactFile) {
          IndexedArtifactFile f = (IndexedArtifactFile) element;
          OpenPomAction.openEditor(f.group, f.artifact, f.version);
        }
      }

      protected boolean updateSelection(IStructuredSelection selection) {
        return selection.getFirstElement() instanceof IndexedArtifactFile;
      }
    };
    openPomAction.setToolTipText("Open Maven POM");
    openPomAction.setImageDescriptor(MavenPlugin.getImageDescriptor("icons/pom_obj.gif"));

    copyUrlAction = new BaseSelectionListenerAction("Copy URL") {
      public void run() {
        Object element = getStructuredSelection().getFirstElement();
        String url = null;
        if(element instanceof IndexInfo) {
          url = ((IndexInfo) element).getRepositoryUrl();
        } else if(element instanceof IndexedArtifactGroup) {
          IndexedArtifactGroup group = (IndexedArtifactGroup) element;
          String repositoryUrl = group.info.getRepositoryUrl();
          if(!repositoryUrl.endsWith("/")) {
            repositoryUrl += "/";
          }
          url = repositoryUrl + group.prefix.replace('.', '/');
        } else if(element instanceof IndexedArtifact) {
          // 
        } else if(element instanceof IndexedArtifactFile) {
          // 
        }
        if(url != null) {
          Clipboard clipboard = new Clipboard(Display.getCurrent());
          clipboard.setContents(new String[] {url}, new Transfer[] {TextTransfer.getInstance()});
          clipboard.dispose();
        }
      }

      protected boolean updateSelection(IStructuredSelection selection) {
        Object element = selection.getFirstElement();
        return element instanceof IndexInfo || element instanceof IndexedArtifactGroup;
      }
    };
    copyUrlAction.setToolTipText("Copy URL to Clipboard");
    
    materializeProjectAction = new BaseSelectionListenerAction("Materialize Projects") {
      public void run() {
        MaterializeAction action = new MaterializeAction();
        action.selectionChanged(this, getStructuredSelection());
        action.run(this);
      }
      
      protected boolean updateSelection(IStructuredSelection selection) {
        return selection.getFirstElement() instanceof IndexedArtifactFile;
      }
    };
    materializeProjectAction.setImageDescriptor(MavenPlugin.getImageDescriptor("icons/import_m2_project.gif"));

    viewer.addSelectionChangedListener(openPomAction);
    viewer.addSelectionChangedListener(updateAction);
    viewer.addSelectionChangedListener(unpackAction);
    viewer.addSelectionChangedListener(removeIndexAction);
    viewer.addSelectionChangedListener(editIndexAction);
    viewer.addSelectionChangedListener(copyUrlAction);
    viewer.addSelectionChangedListener(materializeProjectAction);
  }

  public void dispose() {
    viewer.removeSelectionChangedListener(materializeProjectAction);
    viewer.removeSelectionChangedListener(copyUrlAction);
    viewer.removeSelectionChangedListener(editIndexAction);
    viewer.removeSelectionChangedListener(removeIndexAction);
    viewer.removeSelectionChangedListener(unpackAction);
    viewer.removeSelectionChangedListener(updateAction);
    viewer.removeSelectionChangedListener(openPomAction);

    super.dispose();
  }

  void refreshView() {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        refreshAction.run();
      }
    });
  };

  void updateIndex(final IndexInfo info) {
    IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
    indexManager.scheduleIndexUpdate(info.getIndexName(), false, 0L);
    
//    Job job;
//    if(IndexInfo.Type.REMOTE.equals(info.getType())) {
//      job = new IndexUpdaterJob(info, plugin.getIndexManager(), plugin.getConsole());
//    } else if(IndexInfo.Type.LOCAL.equals(info.getType())) {
//      job = new IndexerJob(info.getIndexName(), plugin.getIndexManager(), plugin.getConsole());
//    } else {
//      return;
//    }
//
//    job.addJobChangeListener(new JobChangeAdapter() {
//      public void done(IJobChangeEvent event) {
//        refreshView();
//      }
//    });
//    
//    job.schedule();
  }

  public class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {

    private final IndexManager indexManager;

    ViewContentProvider() {
      this.indexManager = MavenPlugin.getDefault().getIndexManager();
    }

    public void inputChanged(Viewer v, Object oldInput, Object newInput) {
    }

    public void dispose() {
    }

    public Object[] getElements(Object parent) {
      if(parent.equals(getViewSite())) {
        Map indexes = indexManager.getIndexes();
        return indexes.values().toArray(new IndexInfo[indexes.size()]);
      }
      return getChildren(parent);
    }

    public Object getParent(Object child) {
      if(child instanceof IndexInfo) {
        return getViewSite();
      } else if(child instanceof IndexedArtifactGroup) {
        return ((IndexedArtifactGroup) child).info;
      } else if(child instanceof IndexedArtifact) {
        return null;
      } else if(child instanceof IndexedArtifactFile) {
        return null;
      }
      return null;
    }

    public boolean hasChildren(Object parent) {
      if(parent instanceof IndexInfo) {
        return true;
      } else if(parent instanceof IndexedArtifactGroup) {
        return true;
      } else if(parent instanceof IndexedArtifact) {
        return true;
      }
      return false;
    }

    public Object[] getChildren(Object parent) {
      if(parent instanceof IndexInfo) {
        String indexName = ((IndexInfo) parent).getIndexName();
        try {
          return indexManager.getRootGroups(indexName);
        } catch(IOException ex) {
          MavenPlugin.log("Can't retrieve root groups for " + indexName, ex);
          return new Object[0];
        }

      } else if(parent instanceof IndexedArtifactGroup) {
        IndexedArtifactGroup g = indexManager.resolveGroup((IndexedArtifactGroup) parent);

        ArrayList results = new ArrayList();
        results.addAll(g.nodes.values()); // IndexedArtifactGroup
        results.addAll(g.files.values()); // IndexedArtifact
        return results.toArray(new Object[results.size()]);

      } else if(parent instanceof IndexedArtifact) {
        Set files = ((IndexedArtifact) parent).files;
        return files.toArray(new IndexedArtifactFile[files.size()]);
      }

      return new Object[0];
    }

//    private Map getGroups(IndexInfo info) {
//      IndexManager indexManager = MavenPlugin.getDefault().getMavenRepositoryIndexManager();
//      List files = indexManager.getRootGroups(info.getIndexName());
//
//      long l1 = System.currentTimeMillis();
//      LinkedHashMap nodes = new LinkedHashMap();
//      for(Iterator it = files.iterator(); it.hasNext();) {
//        IndexedArtifactFile f = (IndexedArtifactFile) it.next();
//        addGroup(nodes, f, f.group, info);
//      }
//      long l2 = System.currentTimeMillis();
//      System.err.println("## grouping time: " + (l2-l1)/1000f + " " + files.size());
//
//      return nodes;
//    }
//
//    private IndexedArtifactGroup addGroup(Map nodes, IndexedArtifactFile f, String groupId, IndexInfo info) {
//      int n = groupId.indexOf('.');
//      String key = n == -1 ? groupId : groupId.substring(0, n);
//      IndexedArtifactGroup group = (IndexedArtifactGroup) nodes.get(key);
//      if(group == null) {
//        group = new IndexedArtifactGroup(info, key);
//        nodes.put(key, group);
//      }
//      if(n > -1) {
//        addGroup(group.nodes, f, groupId.substring(n + 1), info);
//      } else {
//        IndexedArtifact artifact = (IndexedArtifact) group.files.get(f.artifact);
//        if(artifact == null) {
//          artifact = new IndexedArtifact(f.group, f.artifact, null, null);
//          group.files.put(f.artifact, artifact);
//        }
//        artifact.addFile(f);
//      }
//      return group;
//    }

  }

  class ViewLabelProvider extends LabelProvider {

    public String getText(Object obj) {
      if(obj instanceof IndexInfo) {
        IndexInfo info = (IndexInfo) obj;
        Type type = info.getType();
        if(IndexInfo.Type.WORKSPACE.equals(type)) {
          return info.getIndexName();
        } else if(IndexInfo.Type.LOCAL.equals(type)) {
          return info.getIndexName() + " : " + info.getRepositoryDir().getAbsolutePath();
        } else {
          return info.getIndexName() + " : " + info.getRepositoryUrl();
        }

      } else if(obj instanceof IndexedArtifactGroup) {
        String prefix = ((IndexedArtifactGroup) obj).prefix;
        int n = prefix.lastIndexOf('.');
        return n < 0 ? prefix : prefix.substring(n + 1);

      } else if(obj instanceof IndexedArtifact) {
        IndexedArtifact a = (IndexedArtifact) obj;
        // return a.group + ":" + a.artifact;
        return a.artifact + " - " + a.packaging;

      } else if(obj instanceof IndexedArtifactFile) {
        IndexedArtifactFile f = (IndexedArtifactFile) obj;
        String label = f.artifact;
        if(f.classifier != null) {
          label += " : " + f.classifier;
        }
        if(f.version != null) {
          label += " : " + f.version;
        }
        return label;

      }
      return obj.toString();
    }

    public Image getImage(Object obj) {
      if(obj instanceof IndexInfo) {
        return MavenPlugin.getImage("icons/maven_index.gif");

      } else if(obj instanceof IndexedArtifactGroup) {
        return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

      } else if(obj instanceof IndexedArtifact) {
        return MavenPlugin.getImage("icons/jar_obj.gif");

      } else if(obj instanceof IndexedArtifactFile) {
        IndexedArtifactFile f = (IndexedArtifactFile) obj;
        if(f.sourcesExists == IndexManager.PRESENT) {
          return MavenPlugin.getImage("icons/jar_src_version.gif");
        }
        return MavenPlugin.getImage("icons/jar_version.gif");

      }

      return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
    }

  }

  class NameSorter extends ViewerSorter {
  }
}
