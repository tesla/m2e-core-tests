/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.metadata.ArtifactMetadata;
import org.apache.maven.artifact.resolver.metadata.MetadataTreeNode;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;


/**
 * @author Eugene Kuleshov
 */
public class DependencyTreePage extends FormPage {

  private static final String DEPENDENCY_HIERARCHY = "Dependency Hierarchy";

  protected static final Object[] EMPTY = new Object[0];

  final MavenPomEditor pomEditor;

  TreeViewer treeViewer;

  TableViewer listViewer;

  SearchControl searchControl;

  SearchMatcher searchMatcher;

  DependencyFilter searchFilter;

  ListSelectionFilter listSelectionFilter;

  ViewerFilter currentFilter;

  ArrayList<DependencyNode> dependencyNodes = new ArrayList<DependencyNode>();

  Color searchHighlightColor;

  MavenProject mavenProject;

  boolean isSettingSelection = false;

  Action hierarchyFilterAction;

  private Job dataLoadingJob;

  String currentScope = Artifact.SCOPE_TEST;

  public DependencyTreePage(MavenPomEditor pomEditor) {
    super(pomEditor, IMavenConstants.PLUGIN_ID + ".pom.dependencyTree", DEPENDENCY_HIERARCHY);
    this.pomEditor = pomEditor;
  }

  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit formToolkit = managedForm.getToolkit();

    searchHighlightColor = new Color(Display.getDefault(), 242, 218, 170);

    ScrolledForm form = managedForm.getForm();
    form.setText(formatFormTitle());
    form.setExpandHorizontal(true);
    form.setExpandVertical(true);

    Composite body = form.getBody();
    body.setLayout(new FillLayout());

    SashForm sashForm = new SashForm(body, SWT.NONE);
    formToolkit.adapt(sashForm);
    formToolkit.adapt(sashForm, true, true);

    createHierarchySection(sashForm, formToolkit);

    createListSection(sashForm, formToolkit);

    sashForm.setWeights(new int[] {1, 1});

    createSearchBar(managedForm);

    // compatibility proxy to support Eclipse 3.2
    FormUtils.decorateHeader(managedForm.getToolkit(), form.getForm());

    loadData(false);
  }

  String formatFormTitle() {
    return DEPENDENCY_HIERARCHY + " [" + currentScope + "]";
  }

  void loadData(final boolean force) {
    // form.setMessage() forces the panel layout, which messes up the viewers
    // (e.g. long entries in the tree cause it to expand horizontally so much
    // doesn't fit into the editor anymore). Clearing the input in the viewers
    // helps to ensure they won't change the size when the message is set.
    treeViewer.setInput(null);
    listViewer.setInput(null);
    FormUtils.setMessage(getManagedForm().getForm(), "Resolving dependencies...", IMessageProvider.WARNING);

    dataLoadingJob = new Job("Loading pom.xml") {
      protected IStatus run(IProgressMonitor monitor) {
        try {
          final DependencyNode dependencyNode = pomEditor.readDependencies(force, monitor, currentScope);

          dependencyNode.accept(new DependencyNodeVisitor() {
            public boolean visit(DependencyNode node) {
              dependencyNodes.add(node);
              return true;
            }

            public boolean endVisit(DependencyNode dependencynode) {
              return true;
            }
          });

          mavenProject = pomEditor.readMavenProject(false, monitor);

          getPartControl().getDisplay().syncExec(new Runnable() {
            public void run() {
              FormUtils.setMessage(getManagedForm().getForm(), null, IMessageProvider.NONE);
              treeViewer.setInput(dependencyNode);
              treeViewer.expandAll();
              listViewer.setInput(mavenProject);
            }
          });
        } catch(final CoreException ex) {
          MavenLogger.log(ex);
          getPartControl().getDisplay().asyncExec(new Runnable() {
            public void run() {
              FormUtils.setMessage(getManagedForm().getForm(), ex.getMessage(), IMessageProvider.ERROR);
            }
          });
        } catch(final MavenEmbedderException ex) {
          MavenLogger.log("Can't load dependency hierarchy", ex);
          getPartControl().getDisplay().asyncExec(new Runnable() {
            public void run() {
              FormUtils.setMessage(getManagedForm().getForm(), ex.getMessage(), IMessageProvider.ERROR);
            }
          });
        }

        return Status.OK_STATUS;
      }
    };
    dataLoadingJob.schedule();
  }

  private void createHierarchySection(Composite sashForm, FormToolkit formToolkit) {
    Composite hierarchyComposite = formToolkit.createComposite(sashForm, SWT.NONE);
    hierarchyComposite.setLayout(new GridLayout());

    Section hierarchySection = formToolkit.createSection(hierarchyComposite, ExpandableComposite.TITLE_BAR);
    hierarchySection.marginHeight = 1;
    hierarchySection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    hierarchySection.setText(DEPENDENCY_HIERARCHY);
    formToolkit.paintBordersFor(hierarchySection);

    Tree tree = formToolkit.createTree(hierarchySection, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
    hierarchySection.setClient(tree);

    treeViewer = new TreeViewer(tree);
    treeViewer.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.TRUE);

    DependencyTreeLabelProvider2 treeLabelProvider = new DependencyTreeLabelProvider2();
    treeViewer.setContentProvider(new DependencyTreeContentProvider2());
    treeViewer.setLabelProvider(treeLabelProvider);

    treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        if(!isSettingSelection) {
          isSettingSelection = true;
          IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
          selectListElements(new DependencyNodeMatcher(selection));
          isSettingSelection = false;
        }
      }
    });

    treeViewer.addOpenListener(new IOpenListener() {
      public void open(OpenEvent event) {
        IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
        for(Iterator<?> it = selection.iterator(); it.hasNext();) {
          Object o = it.next();
          if(o instanceof DependencyNode) {
            Artifact a = ((DependencyNode) o).getArtifact();
            OpenPomAction.openEditor(a.getGroupId(), a.getArtifactId(), a.getVersion());
          }
        }
      }
    });

    createHierarchyToolbar(hierarchySection, treeLabelProvider, formToolkit);
  }

  private void createHierarchyToolbar(Section hierarchySection, final DependencyTreeLabelProvider2 treeLabelProvider,
      FormToolkit formToolkit) {
    ToolBarManager hiearchyToolBarManager = new ToolBarManager(SWT.FLAT);

    hiearchyToolBarManager.add(new Action("Collapse All", MavenEditorImages.COLLAPSE_ALL) {
      public void run() {
        treeViewer.collapseAll();
      }
    });

    hiearchyToolBarManager.add(new Action("Expand All", MavenEditorImages.EXPAND_ALL) {
      public void run() {
        treeViewer.expandAll();
      }
    });

    hiearchyToolBarManager.add(new Separator());

    hiearchyToolBarManager.add(new Action("Sort", MavenEditorImages.SORT) {
      public int getStyle() {
        return AS_CHECK_BOX;
      }

      public void run() {
        if(treeViewer.getComparator() == null) {
          treeViewer.setComparator(new ViewerComparator());
        } else {
          treeViewer.setComparator(null);
        }
      }
    });

    hiearchyToolBarManager.add(new Action("Show GroupId", MavenEditorImages.SHOW_GROUP) {
      public int getStyle() {
        return AS_CHECK_BOX;
      }

      public void run() {
        treeLabelProvider.setShowGroupId(isChecked());
        treeViewer.refresh();
      }
    });

    hierarchyFilterAction = new Action("Filter Search Results", MavenEditorImages.FILTER) {
      public int getStyle() {
        return AS_CHECK_BOX;
      }

      public void run() {
        if(isChecked()) {
          setTreeFilter(currentFilter, true);
//          treeViewer.setFilters(new ViewerFilter[] {searchFilter, listSelectionFilter});
        } else {
          treeViewer.removeFilter(searchFilter);
        }
        treeViewer.refresh();
        treeViewer.expandAll();
      }
    };
    hierarchyFilterAction.setChecked(true);
    hiearchyToolBarManager.add(hierarchyFilterAction);

    Composite toolbarComposite = formToolkit.createComposite(hierarchySection);
    toolbarComposite.setBackground(null);
    RowLayout rowLayout = new RowLayout();
    rowLayout.wrap = false;
    rowLayout.marginRight = 0;
    rowLayout.marginLeft = 0;
    rowLayout.marginTop = 0;
    rowLayout.marginBottom = 0;
    toolbarComposite.setLayout(rowLayout);

    hiearchyToolBarManager.createControl(toolbarComposite);
    hierarchySection.setTextClient(toolbarComposite);
  }

  private void createListSection(SashForm sashForm, FormToolkit formToolkit) {
    Composite listComposite = formToolkit.createComposite(sashForm, SWT.NONE);
    listComposite.setLayout(new GridLayout());

    Section listSection = formToolkit.createSection(listComposite, ExpandableComposite.TITLE_BAR);
    listSection.marginHeight = 1;
    listSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    listSection.setText("Resolved Dependencies");
    formToolkit.paintBordersFor(listSection);

    final DependencyListLabelProvider listLabelProvider = new DependencyListLabelProvider();

    Table table = formToolkit.createTable(listSection, SWT.FLAT | SWT.MULTI);
    listSection.setClient(table);

    // listViewer = new TableViewer(listSection, SWT.FLAT | SWT.MULTI);
    listViewer = new TableViewer(table);
    listViewer.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.TRUE);
    listViewer.setContentProvider(new DependencyListContentProvider());
    listViewer.setLabelProvider(listLabelProvider);
    listViewer.setComparator(new ViewerComparator()); // by default is sorted

    listSelectionFilter = new ListSelectionFilter();
    listViewer.addSelectionChangedListener(listSelectionFilter);
    listViewer.getTable().addFocusListener(listSelectionFilter);

    listViewer.addOpenListener(new IOpenListener() {
      public void open(OpenEvent event) {
        IStructuredSelection selection = (IStructuredSelection) listViewer.getSelection();
        for(Iterator<?> it = selection.iterator(); it.hasNext();) {
          Object o = it.next();
          if(o instanceof Artifact) {
            Artifact a = (Artifact) o;
            OpenPomAction.openEditor(a.getGroupId(), a.getArtifactId(), a.getVersion());
          }
        }
      }
    });

    createListToolbar(listSection, listLabelProvider, formToolkit);

  }

  private void createListToolbar(Section listSection, final DependencyListLabelProvider listLabelProvider,
      FormToolkit formToolkit) {
    ToolBarManager listToolBarManager = new ToolBarManager(SWT.FLAT);

    listToolBarManager.add(new Action("Sort", MavenEditorImages.SORT) {
      {
        setChecked(true);
      }

      public int getStyle() {
        return AS_CHECK_BOX;
      }

      public void run() {
        if(listViewer.getComparator() == null) {
          listViewer.setComparator(new ViewerComparator());
        } else {
          listViewer.setComparator(null);
        }
      }
    });

    listToolBarManager.add(new Action("Show GroupId", MavenEditorImages.SHOW_GROUP) {
      public int getStyle() {
        return AS_CHECK_BOX;
      }

      public void run() {
        listLabelProvider.setShowGroupId(isChecked());
        listViewer.refresh();
      }
    });

    listToolBarManager.add(new Action("Filter Search Results", MavenEditorImages.FILTER) {
      public int getStyle() {
        return AS_CHECK_BOX;
      }

      public void run() {
        if(listViewer.getFilters() == null || listViewer.getFilters().length == 0) {
          listViewer.addFilter(searchFilter);
        } else {
          listViewer.removeFilter(searchFilter);
        }
      }
    });

    Composite toolbarComposite = formToolkit.createComposite(listSection);
    toolbarComposite.setBackground(null);
    RowLayout rowLayout = new RowLayout();
    rowLayout.wrap = false;
    rowLayout.marginRight = 0;
    rowLayout.marginLeft = 0;
    rowLayout.marginTop = 0;
    rowLayout.marginBottom = 0;
    toolbarComposite.setLayout(rowLayout);

    listToolBarManager.createControl(toolbarComposite);
    listSection.setTextClient(toolbarComposite);
  }

  private void createSearchBar(IManagedForm managedForm) {
    searchControl = new SearchControl("Find", managedForm);
    searchMatcher = new SearchMatcher(searchControl);
    searchFilter = new DependencyFilter(new SearchMatcher(searchControl));
    treeViewer.addFilter(searchFilter); // by default is filtered

    ScrolledForm form = managedForm.getForm();

    IToolBarManager toolBarManager = form.getForm().getToolBarManager();
    toolBarManager.add(searchControl);
    
    class ScopeDropdown extends Action implements IMenuCreator {
      private Menu menu;

      public ScopeDropdown() {
        setText("Scope");
        setImageDescriptor(MavenEditorImages.SCOPE);
        setMenuCreator(this);
      }
      
      public Menu getMenu(Menu parent) {
        return null;
      }

      public Menu getMenu(Control parent) {
        if (menu != null) {
          menu.dispose();
        }
        
        menu = new Menu(parent);
        addToMenu(menu, "all (test)", Artifact.SCOPE_TEST, currentScope);
        addToMenu(menu, "compile", Artifact.SCOPE_COMPILE, currentScope);
        addToMenu(menu, "runtime", Artifact.SCOPE_RUNTIME, currentScope);
        addToMenu(menu, "provided", Artifact.SCOPE_PROVIDED, currentScope);
        addToMenu(menu, "system", Artifact.SCOPE_SYSTEM, currentScope);
        return menu;
      }
      
      protected void addToMenu(Menu parent, String text, String scope, String currentScope) {
        ScopeAction action = new ScopeAction(text, scope);
        action.setChecked(scope.equals(currentScope));
        new ActionContributionItem(action).fill(parent, -1);
      }
      
      public void dispose() {
        if (menu != null)  {
          menu.dispose();
          menu = null;
        }
      }
    }
    toolBarManager.add(new ScopeDropdown());
    
    toolBarManager.add(new Separator());
    toolBarManager.add(new Action("Refresh", MavenEditorImages.REFRESH) {
      public void run() {
        loadData(true);
      }
    });

    List<Viewer> viewers = new ArrayList<Viewer>();
    viewers.add(this.treeViewer);
    viewers.add(this.listViewer);
    toolBarManager.add(new DependencyExcludeAction(viewers, (MavenPomEditor) getEditor()));

    form.updateToolBar();

    searchControl.getSearchText().addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
        isSettingSelection = true;
        selectListElements(searchMatcher);
        selectTreeElements(searchMatcher);
        setTreeFilter(searchFilter, false);
        isSettingSelection = false;
      }
    });

    searchControl.getSearchText().addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        isSettingSelection = true;
        selectListElements(searchMatcher);
        selectTreeElements(searchMatcher);
        setTreeFilter(searchFilter, false);
        isSettingSelection = false;
      }
    });
  }

  protected void setTreeFilter(ViewerFilter filter, boolean force) {
    currentFilter = filter;
    if(filter != null && (force || (treeViewer.getFilters().length > 0 && treeViewer.getFilters()[0] != filter))) {
      treeViewer.addFilter(filter);
    }
  }

  protected void selectListElements(Matcher matcher) {
    DependencyListLabelProvider listLabelProvider = (DependencyListLabelProvider) listViewer.getLabelProvider();
    listLabelProvider.setMatcher(matcher);
    listViewer.refresh();

    if(!matcher.isEmpty() && mavenProject != null) {
      @SuppressWarnings("unchecked")
      Set<Artifact> projectArtifacts = mavenProject.getArtifacts();
      for(Artifact a : projectArtifacts) {
        if(matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId())) {
          listViewer.reveal(a);
          break;
        }
      }
    }
  }

  void selectTreeElements(Matcher matcher) {
    DependencyTreeLabelProvider2 treeLabelProvider = (DependencyTreeLabelProvider2) treeViewer.getLabelProvider();
    treeLabelProvider.setMatcher(matcher);
    treeViewer.refresh();
    treeViewer.expandAll();

    if(!matcher.isEmpty()) {
      for(DependencyNode node : dependencyNodes) {
        Artifact a = node.getArtifact();
        if(matcher.isMatchingArtifact(a.getGroupId(), a.getGroupId())) {
          treeViewer.reveal(node);
          break;
        }
      }
    }
  }

  static class DependencyFilter extends ViewerFilter {
    protected Matcher matcher;

    public DependencyFilter(Matcher matcher) {
      this.matcher = matcher;
    }

    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(matcher != null && !matcher.isEmpty()) {
        // matcher = new TextMatcher(searchControl.getSearchText().getText());
        if(element instanceof Artifact) {
          Artifact a = (Artifact) element;
          return matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId());

        } else if(element instanceof DependencyNode) {
          DependencyNode node = (DependencyNode) element;
          node.getArtifact();
          Artifact a = node.getArtifact();
          if(matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId())) {
            return true;
          }

          class ChildMatcher implements DependencyNodeVisitor {
            protected boolean foundMatch = false;

            public boolean visit(DependencyNode node) {
              node.getArtifact();
              Artifact a = node.getArtifact();
              if(matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId())) {
                foundMatch = true;
                return false;
              }
              return true;
            }

            public boolean endVisit(DependencyNode node) {
              return true;
            }
          }
          ;

          ChildMatcher childMatcher = new ChildMatcher();
          node.accept(childMatcher);
          return childMatcher.foundMatch;
        }
      }
      return true;
    }

  }

  class ListSelectionFilter extends DependencyFilter implements ISelectionChangedListener, FocusListener {

    public ListSelectionFilter() {
      super(null); // no filter by default
    }

    // ISelectionChangedListener

    public void selectionChanged(SelectionChangedEvent event) {
      if(!isSettingSelection) {
        isSettingSelection = true;
        IStructuredSelection selection = (IStructuredSelection) listViewer.getSelection();
        matcher = new ArtifactMatcher(selection);
        selectTreeElements(matcher);
        setTreeFilter(this, false);
        isSettingSelection = false;
      }
    }

    // FocusListener

    public void focusGained(FocusEvent e) {
      if(hierarchyFilterAction.isChecked()) {
        setTreeFilter(this, false);
//        treeViewer.addFilter(this);
      }
    }

    public void focusLost(FocusEvent e) {
//      treeViewer.removeFilter(this);
      matcher = null;
    }
  }

  static class DependencyTreeContentProvider implements ITreeContentProvider {

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public Object[] getElements(Object element) {
      return getChildren(element);
    }

    public Object[] getChildren(Object parent) {
      if(parent instanceof MetadataTreeNode) {
        MetadataTreeNode[] children = ((MetadataTreeNode) parent).getChildren();
        if(children != null) {
          return children;
        }
      }
      return EMPTY;
    }

    public boolean hasChildren(Object element) {
      if(element instanceof MetadataTreeNode) {
        return ((MetadataTreeNode) element).hasChildren();
      }
      return false;
    }

    public Object getParent(Object element) {
      if(element instanceof MetadataTreeNode) {
        return ((MetadataTreeNode) element).getParent();
      }
      return null;
    }

    public void dispose() {
    }
  }

  static class DependencyTreeLabelProvider extends LabelProvider {

    public String getText(Object element) {
      if(element instanceof MetadataTreeNode) {
        MetadataTreeNode node = (MetadataTreeNode) element;
        ArtifactMetadata md = node.getMd();
        String label = md.getGroupId() + " : " + md.getArtifactId() + " : " + md.getVersion();

        if(md.getClassifier() != null) {
          label += " - " + md.getClassifier();
        }

        label += " [" + md.getScope() + "]";

        return label;
      }
      return element.toString();
    }

  }

  final class DependencyTreeContentProvider2 implements ITreeContentProvider {

    public Object[] getElements(Object input) {
      return getChildren(input);
    }

    public Object[] getChildren(Object element) {
      if(element instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) element;
        @SuppressWarnings("unchecked")
        List<DependencyNode> children = node.getChildren();
        return children.toArray(new DependencyNode[children.size()]);
      }
      return new Object[0];
    }

    public Object getParent(Object element) {
//      if(element instanceof DependencyNode) {
//        DependencyNode node = (DependencyNode) element;
//        return node.getParent();
//      }
      return null;
    }

    public boolean hasChildren(Object element) {
      if(element instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) element;
        return node.hasChildren();
      }
      return false;
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

  }

  final class DependencyTreeLabelProvider2 extends LabelProvider implements IColorProvider {

    private boolean showGroupId = false;

    private Matcher matcher = null;

    public void setMatcher(Matcher matcher) {
      this.matcher = matcher;
    }

    public void setShowGroupId(boolean showGroupId) {
      this.showGroupId = showGroupId;
    }

    // IColorProvider

    public Color getForeground(Object element) {
      if(element instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) element;
        Artifact a = node.getState() == DependencyNode.OMITTED_FOR_CONFLICT ? node.getRelatedArtifact() //
            : node.getArtifact();
        String scope = a.getScope();
        if(scope != null && !"compile".equals(scope)) {
          return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
        }
      }
      return null;
    }

    public Color getBackground(Object element) {
      if(matcher != null && !matcher.isEmpty() && element instanceof DependencyNode) {
        Artifact a = ((DependencyNode) element).getArtifact();
        if(matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId())) {
          return searchHighlightColor;
        }
      }
      return null;
    }

    // LabelProvider

    @Override
    public String getText(Object element) {
      if(element instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) element;

        Artifact a = node.getState() == DependencyNode.OMITTED_FOR_CONFLICT ? node.getRelatedArtifact() //
            : node.getArtifact();

        String label = "";

        if(showGroupId) {
          label += a.getGroupId() + " : ";
        }

        label += a.getArtifactId() + " : ";

        if(a.getVersion() != null || a.getBaseVersion() != null) {
          label += a.getBaseVersion() == null ? a.getVersion() : a.getBaseVersion();
        } else {
          label += a.getVersionRange().toString();
        }

        switch(node.getState()) {
          case DependencyNode.INCLUDED:
          case DependencyNode.OMITTED_FOR_CYCLE:
          case DependencyNode.OMITTED_FOR_DUPLICATE:
            if(node.getPremanagedVersion() != null) {
              label += " (from " + node.getPremanagedVersion() + ")";
            }
            if(node.getVersionSelectedFromRange() != null) {
              label += " (from " + node.getVersionSelectedFromRange().toString() //
                  + " available " + node.getAvailableVersions().toString() + ")";
            }
            break;

          case DependencyNode.OMITTED_FOR_CONFLICT:
            label += " (conflicted " + node.getArtifact().getVersion() + ")";
            break;
        }

        if(a.getClassifier() != null) {
          label += " - " + a.getClassifier();
        }

        label += " [" + (a.getScope() == null ? "compile" : a.getScope()) + "]";

        if(node.getPremanagedScope() != null) {
          label += " (from " + node.getPremanagedScope() + ")";
        }

        if(node.getOriginalScope() != null) {
          label += " (from " + node.getOriginalScope() + ")";
        }

        if(node.getFailedUpdateScope() != null) {
          label += " (not updated from " + node.getFailedUpdateScope() + ")";
        }

        return label;
      }
      return element.toString();
    }

//    private String getState(DependencyNode node) {
//      switch(node.getState()) {
//        case DependencyNode.INCLUDED:
//          return "included";
//        
//        case DependencyNode.OMITTED_FOR_CONFLICT:
//          return "conflict";
//          
//        case DependencyNode.OMITTED_FOR_CYCLE:
//          return "cycle";
//          
//        case DependencyNode.OMITTED_FOR_DUPLICATE:
//          return "duplicate";
//      }
//      return "?";
//    }

    @Override
    public Image getImage(Object element) {
      if(element instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) element;
        Artifact artifact = node.getArtifact();
        MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
        IMavenProjectFacade projectFacade = projectManager.getMavenProject(artifact.getGroupId(), artifact.getGroupId(), artifact.getVersion());
        return projectFacade == null ? MavenEditorImages.IMG_JAR : MavenEditorImages.IMG_PROJECT;
      }
      return null;
    }
  }

  public class DependencyListLabelProvider extends LabelProvider implements IColorProvider {

    private boolean showGroupId = false;

    private Matcher matcher = null;

    public void setMatcher(Matcher matcher) {
      this.matcher = matcher;
    }

    public void setShowGroupId(boolean showGroupId) {
      this.showGroupId = showGroupId;
    }

    // IColorProvider

    public Color getForeground(Object element) {
      if(element instanceof Artifact) {
        Artifact a = (Artifact) element;
        String scope = a.getScope();
        if(scope != null && !"compile".equals(scope)) {
          return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
        }
      }
      return null;
    }

    public Color getBackground(Object element) {
      if(matcher != null && !matcher.isEmpty() && element instanceof Artifact) {
        Artifact a = (Artifact) element;
        if(matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId())) {
          return searchHighlightColor;
        }
      }
      return null;
    }

    // LabelProvider

    @Override
    public String getText(Object element) {
      if(element instanceof Artifact) {
        Artifact a = (Artifact) element;
        String label = "";

        if(showGroupId) {
          label += a.getGroupId() + " : ";
        }

        label += a.getArtifactId() + " : " + a.getVersion();

        if(a.getClassifier() != null) {
          label += " - " + a.getClassifier();
        }

        if(a.getScope() != null) {
          label += " [" + a.getScope() + "]";
        }

        return label;
      }
      return super.getText(element);
    }

    @Override
    public Image getImage(Object element) {
      if(element instanceof Artifact) {
        Artifact a = (Artifact) element;
        MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
        IMavenProjectFacade projectFacade = projectManager.getMavenProject(a.getGroupId(), a.getArtifactId(), a.getVersion());
        return projectFacade == null ? MavenEditorImages.IMG_JAR : MavenEditorImages.IMG_PROJECT;
      }
      return null;
    }

  }

  public class DependencyListContentProvider implements IStructuredContentProvider {

    @SuppressWarnings("unchecked")
    public Object[] getElements(Object input) {
      if(input instanceof MavenProject) {
        MavenProject project = (MavenProject) input;
        List<Artifact> artifacts = new ArrayList<Artifact>();
        if(Artifact.SCOPE_COMPILE.equals(currentScope)) {
          artifacts = project.getCompileArtifacts();
        } else if(Artifact.SCOPE_TEST.equals(currentScope)) {
          artifacts = project.getTestArtifacts();
        } else if(Artifact.SCOPE_RUNTIME.equals(currentScope)) {
          artifacts = project.getRuntimeArtifacts();
        } else if(Artifact.SCOPE_SYSTEM.equals(currentScope)) {
          artifacts = project.getSystemArtifacts();
        } else if(Artifact.SCOPE_PROVIDED.equals(currentScope)) {
          for(Artifact artifact : (Set<Artifact>) project.getArtifacts()) {
            if(Artifact.SCOPE_PROVIDED.equals(artifact.getScope())) {
              artifacts.add(artifact);
            }
          }
        }
        return artifacts.toArray(new Artifact[artifacts.size()]);
      }
      return null;
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public void dispose() {
    }

  }

  public static class ArtifactMatcher extends Matcher {

    protected final HashSet<String> artifactKeys = new HashSet<String>();

    public ArtifactMatcher(IStructuredSelection selection) {
      for(Iterator<?> it = selection.iterator(); it.hasNext();) {
        addArtifactKey(it.next());
      }
    }

    public boolean isEmpty() {
      return artifactKeys.isEmpty();
    }

    public boolean isMatchingArtifact(String groupId, String artifactId) {
      return artifactKeys.contains(getKey(groupId, artifactId));
    }

    protected void addArtifactKey(Object o) {
      if(o instanceof Artifact) {
        Artifact a = (Artifact) o;
        artifactKeys.add(getKey(a.getGroupId(), a.getArtifactId()));
      }
    }

    protected String getKey(String groupId, String artifactId) {
      return groupId + ":" + artifactId;
    }

  }

  public static class DependencyNodeMatcher extends ArtifactMatcher {

    public DependencyNodeMatcher(IStructuredSelection selection) {
      super(selection);
    }

    @Override
    protected void addArtifactKey(Object o) {
      if(o instanceof DependencyNode) {
        Artifact a = ((DependencyNode) o).getArtifact();
        artifactKeys.add(getKey(a.getGroupId(), a.getArtifactId()));
      }
    }

  }

  @Override
  public void dispose() {
    if(searchHighlightColor != null) {
      searchHighlightColor.dispose();
    }
    super.dispose();
  }
  
  public void selectDepedency(ArtifactKey artifactKey) {
    if(dataLoadingJob!=null && dataLoadingJob.getState()==Job.RUNNING) {
      try {
        dataLoadingJob.join();
      } catch(InterruptedException ex) {
        // ignore
      }
    }

    if(mavenProject!=null) {
      Artifact artifact = getArtifact(artifactKey);
      if(artifact!=null) {
        listViewer.getTable().setFocus();
        listViewer.setSelection(new StructuredSelection(artifact), true);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Artifact getArtifact(ArtifactKey artifactKey) {
    Set<Artifact> artifacts = mavenProject.getArtifacts();
    for(Artifact artifact : artifacts) {
      if(artifactKey.equals(new ArtifactKey(artifact))) {
        return artifact;
      }
    }
    return null;
  }
  

  public class ScopeAction extends Action {

    private final String scope;

    public ScopeAction(String text, String scope) {
      super(text, IAction.AS_RADIO_BUTTON);
      this.scope = scope;
    }

    public void run() {
      if(isChecked()) {
        currentScope = scope;
        IManagedForm managedForm = DependencyTreePage.this.getManagedForm();
        managedForm.getForm().setText(formatFormTitle());
        loadData(false);
      }
    }
  }
  
}
