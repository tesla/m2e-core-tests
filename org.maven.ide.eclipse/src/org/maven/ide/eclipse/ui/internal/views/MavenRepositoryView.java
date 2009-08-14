/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

import org.maven.ide.eclipse.MavenImages;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.MaterializeAction;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.index.IndexListener;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.internal.index.IndexInfo;
import org.maven.ide.eclipse.internal.index.IndexedArtifactGroup;


/**
 * Maven repository view
 * 
 * @author dyocum
 */
public class MavenRepositoryView extends ViewPart {

  IAction refreshAction;

  IAction collapseAllAction;
  
  BaseSelectionListenerAction openPomAction;

  BaseSelectionListenerAction updateAction;
  
  BaseSelectionListenerAction rebuildAction;

  BaseSelectionListenerAction copyUrlAction;

  BaseSelectionListenerAction materializeProjectAction;
  
  TreeViewer viewer;
  RepositoryViewContentProvider contentProvider;
  RepositoryViewLabelProvider labelProvider;

  private DrillDownAdapter drillDownAdapter;

  public void setFocus() {
    viewer.getControl().setFocus();
  }
  
  public void createPartControl(Composite parent) {
    viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    contentProvider = new RepositoryViewContentProvider();
    viewer.setContentProvider(contentProvider);
    viewer.setLabelProvider(new RepositoryViewLabelProvider());
    viewer.setSorter(new RepositoryViewerSorter());
    
    viewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        
      }
    });
    viewer.setInput(getViewSite());
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

      public void indexAdded(String indexName) {
        refreshView();
      }

      public void indexChanged(String indexName) {

        refreshView();

      }

      public void indexRemoved(String indexName) {
        refreshView();
      }
    });
  }

  private void hookContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu");
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        MavenRepositoryView.this.fillContextMenu(manager);
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
    manager.add(new Separator());
    manager.add(collapseAllAction);
    manager.add(refreshAction);
  }

  void fillContextMenu(IMenuManager manager) {
    manager.add(openPomAction);
    manager.add(copyUrlAction);
    manager.add(materializeProjectAction);
    manager.add(new Separator());
    manager.add(updateAction);
    manager.add(rebuildAction);
    manager.add(new Separator());
    manager.add(collapseAllAction);
    manager.add(new Separator());
    drillDownAdapter.addNavigationActions(manager);
    manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
  }

  private void fillLocalToolBar(IToolBarManager manager) {
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
    collapseAllAction.setImageDescriptor(MavenImages.COLLAPSE_ALL);

//    expandAllAction = new Action("Expand All"){
//      public void run(){
//        Job job = new WorkspaceJob("Expanding Repository Tree") {
//          public IStatus runInWorkspace(IProgressMonitor monitor) {
//            Display.getDefault().asyncExec(new Runnable(){
//              public void run(){
//                viewer.expandAll();
//              }
//            });
//            return Status.OK_STATUS;
//          }
//        };
//        job.schedule();
//      }
//    };
//    
//    expandAllAction.setToolTipText("Expand All");
//    expandAllAction.setImageDescriptor(MavenImages.EXPAND_ALL);
    refreshAction = new Action("Refresh") {
      public void run() {
        viewer.setInput(getViewSite());
      }
    };
    
    refreshAction.setToolTipText("Refresh View");
    refreshAction.setImageDescriptor(MavenImages.REFRESH);

    updateAction = new BaseSelectionListenerAction("Update Index") {
      public void run() {
        List<IndexInfo> infoElements = elementsToUpdate(getStructuredSelection().toList());
        for(IndexInfo info : infoElements){
          updateIndex(info, false);
        }
      }
      protected List<IndexInfo> elementsToUpdate(List elements){
        if(elements == null || elements.size() == 0){
          return null;
        }
        ArrayList<IndexInfo> list = new ArrayList<IndexInfo>();
        for(int i=0;i<elements.size();i++){
          Object elem = elements.get(i);
          if(elem instanceof IndexInfo){
            if(IndexInfo.Type.REMOTE.equals(((IndexInfo)elem).getType()) || IndexInfo.Type.LOCAL.equals(((IndexInfo) elem).getType())){
              list.add((IndexInfo)elem);
            }
          }
        }
        return list;
      }
      protected boolean updateSelection(IStructuredSelection selection) {
        List<IndexInfo> elems = elementsToUpdate(selection.toList());
        if(elems != null && elems.size() > 1){
          setText("Update Indexes");
        } else {
          setText("Update Index");
        }
        return elems != null && elems.size() > 0;
      }
    };
    updateAction.setToolTipText("Update repository index");
    updateAction.setImageDescriptor(MavenImages.UPD_INDEX);

    rebuildAction = new BaseSelectionListenerAction("Rebuild Index") {
      public void run() {
        List<IndexInfo> elemsToUpdate = elementsToUpdate(getStructuredSelection().toList());
        if(elemsToUpdate != null && elemsToUpdate.size() > 0){
          if(elemsToUpdate.size() == 1){
            IndexInfo info = elemsToUpdate.get(0);
            String name = info.getDisplayName() == null ? info.getIndexName() : info.getDisplayName();
            String msg = "Are you sure you want to rebuild the index '"+name+"'";
            boolean res = MessageDialog.openConfirm(getViewSite().getShell(), //
                "Rebuild Index", msg);
            if(res) {
              // TODO request index deletion
              // TODO add deleted index to 
              updateIndex(info, true);
            }
          } else {
            String msg = "Are you sure you want to rebuild the selected indexes?";
            boolean res = MessageDialog.openConfirm(getViewSite().getShell(), //
                "Rebuild Indexes", msg);
            if(res) {
              // TODO request index deletion
              // TODO add deleted index to 
              for(IndexInfo info : elemsToUpdate){
                updateIndex(info, true);
              }
            }            
          }
        }
      }
      
      protected List<IndexInfo> elementsToUpdate(List elements){
        if(elements == null || elements.size() == 0){
          return null;
        }
        ArrayList<IndexInfo> list = new ArrayList<IndexInfo>();
        for(int i=0;i<elements.size();i++){
          Object elem = elements.get(i);
          if(elem instanceof IndexInfo){
            if(IndexInfo.Type.REMOTE.equals(((IndexInfo)elem).getType()) || IndexInfo.Type.LOCAL.equals(((IndexInfo) elem).getType())){
              list.add((IndexInfo)elem);
            }
          }
        }
        return list;
      }

      protected boolean updateSelection(IStructuredSelection selection) {
        List<IndexInfo> elems = elementsToUpdate(selection.toList());
        if(elems != null && elems.size() > 1){
          setText("Rebuild Indexes");
        } else {
          setText("Rebuild Index");
        }
        return elems != null && elems.size() > 0;
      }
    };
    
    rebuildAction.setToolTipText("Force a rebuild of the maven index");
    rebuildAction.setImageDescriptor(MavenImages.REBUILD_INDEX);

    openPomAction = new BaseSelectionListenerAction("Open POM") {
      public void run() {
        ISelection selection = viewer.getSelection();
        Object element = ((IStructuredSelection) selection).getFirstElement();
        if(element instanceof IndexedArtifactFile) {
          IndexedArtifactFile f = (IndexedArtifactFile) element;
          OpenPomAction.openEditor(f.group, f.artifact, f.version, null);
        }
      }

      protected boolean updateSelection(IStructuredSelection selection) {
        return selection.getFirstElement() instanceof IndexedArtifactFile;
      }
    };
    openPomAction.setToolTipText("Open Maven POM");
    openPomAction.setImageDescriptor(MavenImages.POM);

    copyUrlAction = new BaseSelectionListenerAction("Copy URL") {
      public void run() {
        Object element = getStructuredSelection().getFirstElement();
        String url = null;
        if(element instanceof IndexInfo) {
          url = ((IndexInfo) element).getRepositoryUrl();
        } else if(element instanceof IndexedArtifactGroup) {
          IndexedArtifactGroup group = (IndexedArtifactGroup) element;
          String repositoryUrl = group.getRepositoryUrl();
          if(!repositoryUrl.endsWith("/")) {
            repositoryUrl += "/";
          }
          url = repositoryUrl + group.getPrefix().replace('.', '/');
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
    materializeProjectAction.setImageDescriptor(MavenImages.IMPORT_PROJECT);

    viewer.addSelectionChangedListener(openPomAction);
    viewer.addSelectionChangedListener(updateAction);
    viewer.addSelectionChangedListener(rebuildAction);
    viewer.addSelectionChangedListener(copyUrlAction);
    viewer.addSelectionChangedListener(materializeProjectAction);
  }

  public void dispose() {
    viewer.removeSelectionChangedListener(materializeProjectAction);
    viewer.removeSelectionChangedListener(copyUrlAction);
    viewer.removeSelectionChangedListener(rebuildAction);
    viewer.removeSelectionChangedListener(updateAction);
    viewer.removeSelectionChangedListener(openPomAction);

    super.dispose();
  }

  void refreshView() {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        Object[] expandedElems = viewer.getExpandedElements();
        if (!viewer.getControl().isDisposed()) {
          viewer.setInput(getViewSite());
          if(expandedElems != null && expandedElems.length > 0){
            viewer.setExpandedElements(expandedElems);
          }
        }
      }
    });
  };

  void updateIndex(final IndexInfo info, boolean force){
    try{
      IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
      indexManager.scheduleIndexUpdate(info.getIndexName(), force, 1L);
    } catch(CoreException ce){
      MavenLogger.log(ce);
    }
  }

}
