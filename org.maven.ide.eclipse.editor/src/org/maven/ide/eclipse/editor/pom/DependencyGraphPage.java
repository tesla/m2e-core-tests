/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.zest.core.viewers.AbstractZoomableViewer;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IZoomableWorkbenchPart;
import org.eclipse.zest.core.viewers.ZoomContributionViewItem;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutAlgorithm;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.CompositeLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.DirectedGraphLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.HorizontalShift;
import org.eclipse.zest.layouts.algorithms.RadialLayoutAlgorithm;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.editor.MavenEditorImages;

/**
 * Dependency graph editor page
 * 
 * @author Eugene Kuleshov
 */
public class DependencyGraphPage extends FormPage implements IZoomableWorkbenchPart {

  protected static final Object[] EMPTY = new Object[0];

  private final MavenPomEditor pomEditor;
  
  GraphViewer viewer;

  private IAction openAction;
  private IAction selectAllAction;

  private IAction showVersionAction;
  private IAction showGroupAction;
  private IAction showScopeAction;
  private IAction showIconAction;
  private IAction wrapLabelAction;

  private IAction showAllScopeAction;
  private IAction showCompileScopeAction;
  private IAction showTestScopeAction;
  private IAction showRuntimeScopeAction;
  private IAction showProvidedScopeAction;
  private IAction showSystemScopeAction;
  private IAction showResolvedAction;
  private IAction radialLayoutAction;
  private IAction showLegendAction;

  DependencyGraphLabelProvider graphLabelProvider;

  private DependencyGraphContentProvider graphContentProvider;

  private ZoomContributionViewItem zoomContributionItem;

  private SearchControl searchControl;


  public DependencyGraphPage(MavenPomEditor pomEditor) {
    super(pomEditor, MavenPlugin.PLUGIN_ID + ".dependency.graph", "Dependency Graph");
    this.pomEditor = pomEditor;
  }
  
  public AbstractZoomableViewer getZoomableViewer() {
    return viewer;
  }

  protected void createFormContent(final IManagedForm managedForm) {
    ScrolledForm form = managedForm.getForm();
    form.setText("Dependency Graph");
    form.setExpandHorizontal(true);
    form.setExpandVertical(true);

    class MavenGraphViewer extends GraphViewer {
      public MavenGraphViewer(Composite parent, int style) {
        super(parent, style);
        setControl(new Graph(parent, style) {
          public Point computeSize(int wHint, int hHint, boolean changed) {
            return new Point(0, 0);
          }
        });
      }
    }

    Composite body = form.getBody();
    GridLayout layout = new GridLayout(1, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    body.setLayout(layout);

    viewer = new MavenGraphViewer(body, SWT.NONE);
    viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    // viewer.setNodeStyle(ZestStyles.NODES_FISHEYE);
    viewer.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED);
    

    // graphViewer.getGraphControl().setScrollBarVisibility(SWT.NONE);

    // graphViewer.setLayoutAlgorithm(new
    // HorizontalTreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING));
    // graphViewer.setLayoutAlgorithm(new
    // SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING));
    // graphViewer.setLayoutAlgorithm(new
    // RadialLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING |
    // LayoutStyles.ENFORCE_BOUNDS));

    // graphViewer.setLayoutAlgorithm(new CompositeLayoutAlgorithm(
    // LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS, //
    // new LayoutAlgorithm[] { //
    // // new RadialLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING |
    // LayoutStyles.ENFORCE_BOUNDS), //
    // // new VerticalLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING |
    // LayoutStyles.ENFORCE_BOUNDS), //
    // // new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING |
    // LayoutStyles.ENFORCE_BOUNDS), //
    // // new HorizontalShift(LayoutStyles.NO_LAYOUT_NODE_RESIZING |
    // LayoutStyles.ENFORCE_BOUNDS), //
    // // new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING |
    // LayoutStyles.ENFORCE_BOUNDS), //
    // }));

    viewer.setLayoutAlgorithm(new CompositeLayoutAlgorithm(
        LayoutStyles.NO_LAYOUT_NODE_RESIZING | LayoutStyles.ENFORCE_BOUNDS, // 
        new LayoutAlgorithm[] { // 
            new DirectedGraphLayoutAlgorithm(
                LayoutStyles.NO_LAYOUT_NODE_RESIZING
                    | LayoutStyles.ENFORCE_BOUNDS), // 
            new HorizontalShift(LayoutStyles.NO_LAYOUT_NODE_RESIZING
                | LayoutStyles.ENFORCE_BOUNDS) }));

    graphContentProvider = new DependencyGraphContentProvider();
    viewer.setContentProvider(graphContentProvider);
    
    graphLabelProvider = new DependencyGraphLabelProvider(viewer, graphContentProvider);
    viewer.setLabelProvider(graphLabelProvider);
    viewer.addSelectionChangedListener(graphLabelProvider);
    viewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        openAction.setEnabled(!viewer.getSelection().isEmpty());
      }
    });

    IToolBarManager toolBarManager = form.getForm().getToolBarManager();

    searchControl = new SearchControl("Find", managedForm);
    
    toolBarManager.add(searchControl);

    toolBarManager.add(new Separator());

    toolBarManager.add(new Action("Refresh", MavenEditorImages.REFRESH) {
      public void run() {
        viewer.getGraphControl().applyLayout();
        // graphViewer.setInput(graph);
      }
    });
    
    createActions();
    initPopupMenu();

    // compatibility proxy to support Eclipse 3.2
    FormUtils.proxy(managedForm.getToolkit(), //
        FormUtils.FormTooliktStub.class).decorateFormHeading(form.getForm());
    
    form.updateToolBar();

    searchControl.getSearchText().addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
        selectElements();
      }
    });

    searchControl.getSearchText().addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        selectElements();
      }
    });
    
    
    updateGraphAsync(false);
  }

  private void selectElements() {
    ArrayList elements = new ArrayList();
    String text = searchControl.getSearchText().getText();
    if(text.length()>0) {
      Object[] nodeElements = viewer.getNodeElements();
      for(int i = 0; i < nodeElements.length; i++ ) {
        Artifact a = (Artifact) nodeElements[i];
        if(a.getGroupId().indexOf(text)>-1 || a.getArtifactId().indexOf(text)>-1) {
          elements.add(a);
        }
      }
    }
    viewer.setSelection(new StructuredSelection(elements), true);
  }

  private void createActions() {
    openAction = new Action("&Open") {
      public void run() {
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        if(!selection.isEmpty()) {
          for(Iterator it = selection.iterator(); it.hasNext();) {
            Object o = (Object) it.next();
            if(o instanceof Artifact) {
              Artifact a = (Artifact) o;
              OpenPomAction.openEditor(a.getGroupId(), a.getArtifactId(), a.getVersion());
            }
            
          }
        }
      }
    };
    openAction.setEnabled(false);

    showVersionAction = new Action("Show &Version", SWT.CHECK) {
      public void run() {
        graphLabelProvider.setShowVersion(isChecked());
      }
    };
    showVersionAction.setChecked(true);

    showGroupAction = new Action("Show &Group", SWT.CHECK) {
      public void run() {
        graphLabelProvider.setShowGroup(isChecked());
      }
    };
    showGroupAction.setChecked(false);

    showScopeAction = new Action("Show &Scope", SWT.CHECK) {
      public void run() {
        graphLabelProvider.setShowScope(isChecked());
      }
    };
    showScopeAction.setChecked(true);

    showIconAction = new Action("Show &Icon", SWT.CHECK) {
      public void run() {
        graphLabelProvider.setShowIcon(isChecked());
      }
    };
    showIconAction.setChecked(true);
    
    wrapLabelAction = new Action("&Wrap Label", SWT.CHECK) {
      public void run() {
        graphLabelProvider.setWarpLabel(isChecked());
      }
    };
    wrapLabelAction.setChecked(true);

    showResolvedAction = new Action("Show Resolved", SWT.CHECK) {
      public void run() {
        graphContentProvider.setShowResolved(isChecked());
        updateGraphAsync(false);
      }
    };
    showResolvedAction.setChecked(true);
    
    radialLayoutAction = new Action("&Radial Layout", SWT.CHECK) {
      public void run() {
        if (isChecked()) {
          viewer.setLayoutAlgorithm(new CompositeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING, // 
              new LayoutAlgorithm[] { // 
                new RadialLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING),
                new HorizontalShift(LayoutStyles.NO_LAYOUT_NODE_RESIZING),
              }));

        } else {
          viewer.setLayoutAlgorithm(new CompositeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING /*| LayoutStyles.ENFORCE_BOUNDS*/, // 
              new LayoutAlgorithm[] { // 
                new DirectedGraphLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING /*| LayoutStyles.ENFORCE_BOUNDS*/), // 
                new HorizontalShift(LayoutStyles.NO_LAYOUT_NODE_RESIZING)
              }));
        }
        viewer.getGraphControl().applyLayout();
      }
    };
    radialLayoutAction.setChecked(false);

    selectAllAction = new Action("Select &All") {
      public void run() {
        viewer.setSelection(new StructuredSelection(viewer.getNodeElements()));
      }
    };

    showLegendAction = new Action("Show UI Legend") {
      public void run() {
        DependencyGraphLegendPopup popup = new DependencyGraphLegendPopup(getEditor().getSite().getShell());
        popup.open();
      }
    };
    
//    showAllScopeAction = new Action("Show All Scopes", SWT.CHECK) {
//      public void run() {
//        updateGraph(null);
//        updateScopeActions(this);
//      }
//    };
//    showAllScopeAction.setChecked(true);
//    
//    showCompileScopeAction = new Action("Show Compile Scope", SWT.CHECK) {
//      public void run() {
//        updateGraph(ArtifactScopeEnum.compile);
//        updateScopeActions(this);
//      }
//    };
//    showCompileScopeAction.setChecked(false);
//
//    showTestScopeAction = new Action("Show Test Scope", SWT.CHECK) {
//      public void run() {
//        updateGraph(ArtifactScopeEnum.test);
//        updateScopeActions(this);
//      }
//    };
//    showTestScopeAction.setChecked(false);
//
//    showRuntimeScopeAction = new Action("Show Runtime Scope", SWT.CHECK) {
//      public void run() {
//        updateGraph(ArtifactScopeEnum.runtime);
//        updateScopeActions(this);
//      }
//    };
//    showRuntimeScopeAction.setChecked(false);
//
//    showProvidedScopeAction = new Action("Show Provided Scope", SWT.CHECK) {
//      public void run() {
//        updateGraph(ArtifactScopeEnum.provided);
//        updateScopeActions(this);
//      }
//    };
//    showProvidedScopeAction.setChecked(false);
//
//    showSystemScopeAction = new Action("Show System Scope", SWT.CHECK) {
//      public void run() {
//        updateGraph(ArtifactScopeEnum.system);
//        updateScopeActions(this);
//      }
//    };
//    showSystemScopeAction.setChecked(false);

  }

  private void initPopupMenu() {
    zoomContributionItem = new ZoomContributionViewItem(this);

    MenuManager menuMgr = new MenuManager("#PopupMenu");
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        fillContextMenu(manager);
      }
    });

    Menu menu = menuMgr.createContextMenu(viewer.getControl());
    viewer.getControl().setMenu(menu);
    // getSite().registerContextMenu(menuMgr, viewer);
  }

  void updateScopeActions(IAction action) {
    showAllScopeAction.setChecked(showAllScopeAction==action);
    showCompileScopeAction.setChecked(showCompileScopeAction==action);
    showTestScopeAction.setChecked(showTestScopeAction==this);
    showRuntimeScopeAction.setChecked(showRuntimeScopeAction==action);
    showProvidedScopeAction.setChecked(showProvidedScopeAction==action);
    showSystemScopeAction.setChecked(showSystemScopeAction==action);
  }

  private void updateGraphAsync(final boolean force) {
    new Job("Loading pom.xml") {
      protected IStatus run(IProgressMonitor monitor) {
        try {
          final DependencyNode dependencyNode = pomEditor.readDependencies(force, monitor);
          getPartControl().getDisplay().asyncExec(new Runnable() {
            public void run() {
              updateGraph(dependencyNode);
            }
          });
        } catch(final CoreException ex) {
          getPartControl().getDisplay().asyncExec(new Runnable() {
            public void run() {
              getManagedForm().getForm().setMessage(ex.getMessage(), IMessageProvider.ERROR);
            }
          });
        }
        
        return Status.OK_STATUS;
      }
    }.schedule();
  }

  private void updateGraph(DependencyNode node) {
    // MetadataGraph graph = scope==null && result!=null ? result.getGraph() : result.getGraph(scope);
    // MetadataGraph graph = result.getGraph(ArtifactScopeEnum.DEFAULT_SCOPE);
    // MetadataGraph graph = result.getGraph(MetadataResolutionRequestTypeEnum.versionedGraph);
    // MetadataGraph graph = result.getGraph(MetadataResolutionRequestTypeEnum.scopedGraph);

    viewer.setInput(node);
    
    IStructuredContentProvider contentProvider = (IStructuredContentProvider) viewer.getContentProvider();
    viewer.setSelection(new StructuredSelection(contentProvider.getElements(node)));
  }

  void fillContextMenu(IMenuManager manager) {
    manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

    if (!viewer.getSelection().isEmpty()) {
      manager.add(openAction);
      manager.add(new Separator());
    }

    manager.add(selectAllAction);
    manager.add(new Separator());
    manager.add(showGroupAction);
    manager.add(showVersionAction);
    manager.add(showScopeAction);
    manager.add(showIconAction);
    manager.add(wrapLabelAction);
    
//    manager.add(new Separator());
//    manager.add(showAllScopeAction);
//    manager.add(showCompileScopeAction);
//    manager.add(showTestScopeAction);
//    manager.add(showRuntimeScopeAction);
//    manager.add(showProvidedScopeAction);
//    manager.add(showSystemScopeAction);

    manager.add(new Separator());
    manager.add(showResolvedAction);
    manager.add(radialLayoutAction);

    manager.add(new Separator());
    manager.add(showLegendAction);
    
    manager.add(new Separator());
    manager.add(zoomContributionItem);
  }

/*  
  private static final class MavenViewerFilter extends ViewerFilter {
    private final GraphViewer graphViewer;

    private boolean showCompile = true;
    private boolean showTest = true;
    private boolean showRuntime = true;
    private boolean showProvided = true;
    private boolean showSystem = true;

    public MavenViewerFilter(GraphViewer graphViewer) {
      this.graphViewer = graphViewer;
    }

    public void setShowCompile(boolean showCompile) {
      this.showCompile = showCompile;
      update();
    }

    public void setShowTest(boolean showTest) {
      this.showTest = showTest;
      update();
    }

    public void setShowRuntime(boolean showRuntime) {
      this.showRuntime = showRuntime;
      update();
    }

    public void setShowProvided(boolean showProvided) {
      this.showProvided = showProvided;
      update();
    }

    public void setShowSystem(boolean showSystem) {
      this.showSystem = showSystem;
      update();
    }

    private void update() {
      graphViewer.refresh(true);
      graphViewer.applyLayout();
    }

    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(element instanceof MetadataGraphVertex) {
        MetadataGraphVertex v = (MetadataGraphVertex) element;
        if(!select(v.getMd())) {
          return false;
        }
      }
      return true;
    }

    private boolean select(ArtifactMetadata md) {
      ArtifactScopeEnum artifactScope = md.getArtifactScope();
      if (artifactScope == ArtifactScopeEnum.compile) {
        return showCompile;
      } else if (artifactScope == ArtifactScopeEnum.test) {
        return showTest;
      } else if (artifactScope == ArtifactScopeEnum.runtime) {
        return showRuntime;
      } else if (artifactScope == ArtifactScopeEnum.provided) {
        return showProvided;
      } else if (artifactScope == ArtifactScopeEnum.system) {
        return showSystem;
      }
      return true;
    }

  }
*/
  
}