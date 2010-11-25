/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.editor.dialogs;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.dialogs.AbstractMavenDialog;
import org.eclipse.m2e.editor.MavenEditorPlugin;
import org.eclipse.m2e.editor.composites.ListEditorContentProvider;
import org.eclipse.m2e.editor.composites.ManageDependencyLabelProvider;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.DependencyManagement;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.util.PomResourceFactoryImpl;
import org.eclipse.m2e.model.edit.pom.util.PomResourceImpl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;


/**
 * This dialog is used to present the user with a list of dialogs that they can move to being managed under
 * "dependencyManagement". It allows them to pick the destination POM where the dependencies will be managed.
 * 
 * @author rgould
 */
public class ManageDependenciesDialog extends AbstractMavenDialog {

  
  protected static final String DIALOG_SETTINGS = ManageDependenciesDialog.class.getName();
  protected TableViewer dependenciesViewer;
  protected TreeViewer pomsViewer;
  protected Model model;
  LinkedList<MavenProject> projectHierarchy;
  protected EditingDomain editingDomain;
  
  /**
   * Hierarchy is a LinkedList representing the hierarchy relationship between
   * POM represented by model and its parents. The head of the list should be
   * the child, while the tail should be the root parent, with the others 
   * in between.
   */
  public ManageDependenciesDialog(Shell parent, Model model, LinkedList<MavenProject> hierarchy, EditingDomain editingDomain) {
    super(parent, DIALOG_SETTINGS);

    setShellStyle(getShellStyle() | SWT.RESIZE);
    setTitle("Manage Dependencies");
    
    this.model = model;
    this.projectHierarchy = hierarchy;
    this.editingDomain = editingDomain;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  protected Control createDialogArea(Composite parent) {
    readSettings();

    Composite composite = (Composite) super.createDialogArea(parent);

    Label infoLabel = new Label(composite, SWT.WRAP);
    infoLabel
        .setText("This will start managing the selected dependencies for all " +
        		"child POMs. You may select the POM that will manage these " +
        		"dependencies. The version information will then be moved to it.");
    
    Label horizontalBar = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);

    SashForm sashForm = new SashForm(composite, SWT.SMOOTH | SWT.HORIZONTAL);
    Composite dependenciesComposite = new Composite(sashForm, SWT.NONE);
    
    Label selectDependenciesLabel = new Label(dependenciesComposite, SWT.NONE);
    selectDependenciesLabel.setText("Select dependencies to manage:");
    
    final Table dependenciesTable = new Table(dependenciesComposite, SWT.FLAT | SWT.MULTI | SWT.BORDER);
    final TableColumn column = new TableColumn(dependenciesTable, SWT.NONE);
    dependenciesTable.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        column.setWidth(dependenciesTable.getClientArea().width);
      }
    });
    
    Composite pomComposite = new Composite(sashForm, SWT.NONE);
    
    Label selectPomLabel = new Label(pomComposite, SWT.NONE);
    selectPomLabel.setText("Select the POM which will manage the dependencies:");
    
    Tree pomTree = new Tree(pomComposite, SWT.BORDER);
    
    /*
     * Configure layouts
     */
    
    GridLayout layout = new GridLayout(1, false);
    composite.setLayout(layout);
    
    GridData gridData = new GridData(SWT.FILL, SWT.NONE, true, false);
    gridData.widthHint = 300;
    infoLabel.setLayoutData(gridData);
    
    gridData = new GridData(SWT.FILL, SWT.NONE, true, false);
    horizontalBar.setLayoutData(gridData);
    
    gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    sashForm.setLayoutData(gridData);
    
    gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    dependenciesComposite.setLayoutData(gridData);
    
    layout = new GridLayout(1, false);
    dependenciesComposite.setLayout(layout);
    
    gridData = new GridData(SWT.FILL, SWT.NONE, true, false);
    selectDependenciesLabel.setLayoutData(gridData);
    
    gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    dependenciesTable.setLayoutData(gridData);
    
    
    gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    pomComposite.setLayoutData(gridData);
    
    layout = new GridLayout(1, false);
    pomComposite.setLayout(layout);
    
    gridData = new GridData(SWT.FILL, SWT.NONE, true, false);
    selectPomLabel.setLayoutData(gridData);
    
    gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    pomTree.setLayoutData(gridData);
    
    /*
     * Set up list/tree viewers
     */
    
    dependenciesViewer = new TableViewer(dependenciesTable);
    dependenciesViewer.setLabelProvider(new ManageDependencyLabelProvider());
    dependenciesViewer.setContentProvider(new ListEditorContentProvider<Dependency>());
    dependenciesViewer.setInput(model.getDependencies());
    
    pomsViewer = new TreeViewer(pomTree);
    
    pomsViewer.setLabelProvider(new DepLabelProvider());
    
    pomsViewer.setContentProvider(new ContentProvider());
    pomsViewer.setInput(getProjectHierarchy());
    pomsViewer.addSelectionChangedListener(new PomViewerSelectionChangedListener());
    pomsViewer.expandAll();
    
    return composite;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
   */
  protected void computeResult() {
    MavenProject targetPOM = getTargetPOM();
    IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().getMavenProject(targetPOM.getGroupId(), targetPOM.getArtifactId(), targetPOM.getVersion());
    
    /*
     * Load the target model so we can make modifications to it
     */
    Model targetModel = null;
    if (targetPOM.equals(getProjectHierarchy().getFirst())) {
      //Editing the same models in both cases
      targetModel = model;
    } else {
      targetModel = loadTargetModel(facade);
      if (targetModel == null) {
        return;
      }
    }
    
    LinkedList<Dependency> dependencies = getDependenciesList();
    
    /*
     * 1) Remove version values from the dependencies from the current POM
     * 2) Add dependencies to dependencyManagement of targetPOM
     */
    
    CompoundCommand command = new CompoundCommand();
    
    for (Dependency dep : dependencies) {
      Command unset = SetCommand.create(editingDomain, dep, 
          PomPackage.eINSTANCE.getDependency_Version(), SetCommand.UNSET_VALUE);
      command.append(unset);
    }
    
    DependencyManagement management = targetModel.getDependencyManagement();
    if (management == null) {
      management = PomFactory.eINSTANCE.createDependencyManagement();
      Command createDepManagement = SetCommand.create(editingDomain, targetModel,
          PomPackage.eINSTANCE.getModel_DependencyManagement(), management);
      command.append(createDepManagement);
    }
        
    for (Dependency dep : dependencies) {
      Dependency clone = PomFactory.eINSTANCE.createDependency();
      clone.setGroupId(dep.getGroupId());
      clone.setArtifactId(dep.getArtifactId());
      clone.setVersion(dep.getVersion());
      
      Command addDepCommand = AddCommand.create(editingDomain, management, 
          PomPackage.eINSTANCE.getDependencyManagement_Dependencies(), clone);
      
      command.append(addDepCommand);
    }
    editingDomain.getCommandStack().execute(command);
    
  }

  protected Model loadTargetModel(IMavenProjectFacade facade) {
    Model targetModel;
    PomResourceFactoryImpl factory = new PomResourceFactoryImpl();
    PomResourceImpl resource = (PomResourceImpl) factory.createResource(
        URI.createFileURI(facade.getPomFile().getPath()));
    try {
      resource.load(Collections.EMPTY_MAP);
    } catch(IOException e) {
      MavenLogger.log("Can't load model " + facade.getPomFile().getPath(), e);
      return null;
    }
    targetModel = (Model) resource.getContents().get(0);
    return targetModel;
  }

  protected LinkedList<Dependency> getDependenciesList() {
    IStructuredSelection selection = (IStructuredSelection) dependenciesViewer.getSelection();
    
    LinkedList<Dependency> dependencies = new LinkedList<Dependency>();

    for (Object obj : selection.toArray()) {
      dependencies.add((Dependency) obj);
    }
    
    return dependencies;
  }

  protected LinkedList<MavenProject> getProjectHierarchy() {
    return this.projectHierarchy;
  }

  protected MavenProject getTargetPOM() {
    IStructuredSelection selection = (IStructuredSelection) pomsViewer.getSelection();
    return (MavenProject) selection.getFirstElement();
  }
  
  protected class PomViewerSelectionChangedListener implements ISelectionChangedListener {
    @SuppressWarnings("synthetic-access")
    public void selectionChanged(SelectionChangedEvent event) {
      boolean error = false;
      IStructuredSelection selections = (IStructuredSelection) event.getSelection();
      for (Object selection : selections.toArray()) {
        if (selection instanceof MavenProject) {
          MavenProject project = (MavenProject) selection;
          
          IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().getMavenProject(project.getGroupId(), project.getArtifactId(), project.getVersion());
          if (facade == null) {
            error = true;
            updateStatus(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, "The selected project cannot be chosen because it is not present in your workspace."));
          }
        }
      }
      if (!error) {
        updateStatus(new Status(IStatus.OK, MavenEditorPlugin.PLUGIN_ID, ""));
      }
    }
  }

  public static class DepLabelProvider extends LabelProvider implements IColorProvider {
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
     */
    @Override
    public String getText(Object element) {
      MavenProject project = null;
      if (element instanceof MavenProject) {
        project = (MavenProject) element;
      } else if (element instanceof Object[]) {
        project = (MavenProject) ((Object[]) element)[0];
      } else {
        return "";
      }
      
      StringBuffer buffer = new StringBuffer();
      buffer.append(project.getGroupId() + " : " + project.getArtifactId() + " : " +project.getVersion());
      return buffer.toString();
      
    }

    public Color getForeground(Object element) {
      if (element instanceof MavenProject) {
        MavenProject project = (MavenProject) element;
        IMavenProjectFacade search = MavenPlugin.getDefault().getMavenProjectManager().getMavenProject(project.getGroupId(), project.getArtifactId(), project.getVersion());
        if (search == null) {
          //This project is not in the workspace so we can't really modify it.
          return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
        }
      }
      return null;
    }

    public Color getBackground(Object element) {
      return null;
    }
  }

  public class ContentProvider implements ITreeContentProvider {
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public void dispose() {
    }

    public boolean hasChildren(Object element) {
      Object[] children = getChildren(element);
      
      return children.length != 0;
    }

    public Object getParent(Object element) {
      if (element instanceof MavenProject){
        MavenProject project = (MavenProject) element;
        return project.getParent();
      }
      return null;
    }

    /*
     * Return root element
     * (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements(Object inputElement) {
      
      if (inputElement instanceof LinkedList) {
        LinkedList<MavenProject> projects = (LinkedList<MavenProject>) inputElement;
        if (projects.isEmpty()) {
          return new Object[0];
        }
        return new Object[] { projects.getLast() };
      }
      
      return new Object[0];
    }

    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof MavenProject) {
        /*
         * Walk the hierarchy list until we find the parentElement and
         * return the previous element, which is the child.
         */
        MavenProject parent = (MavenProject) parentElement;
        
        if (getProjectHierarchy().size() == 1) {
          //No parent exists, only one element in the tree
          return new Object[0];
        }
        
        if (getProjectHierarchy().getFirst().equals(parent)) {
          //We are the final child
          return new Object[0];
        }
        
        ListIterator<MavenProject> iter = getProjectHierarchy().listIterator();
        while (iter.hasNext()) {
          MavenProject next = iter.next();
          if (next.equals(parent)) {
            iter.previous();
            MavenProject previous = iter.previous();
            return new Object[] { previous };
          }
        }
      }
      return new Object[0];
    }
  }


}
