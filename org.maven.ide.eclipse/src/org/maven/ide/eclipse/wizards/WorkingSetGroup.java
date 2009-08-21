/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wizards;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetNewWizard;

import org.maven.ide.eclipse.project.ProjectImportConfiguration;

/**
 * Working set group
 *
 * @author Eugene Kuleshov
 */
public class WorkingSetGroup {
  
  static final List<String> WORKING_SET_IDS = Arrays.asList( //
      "org.eclipse.ui.resourceWorkingSetPage",
      "org.eclipse.jdt.ui.JavaWorkingSetPage");
  
  ComboViewer workingsetComboViewer;
  Button addToWorkingSetButton;

  final ProjectImportConfiguration configuration;

  final Shell shell;

  public WorkingSetGroup(Composite container, ProjectImportConfiguration configuration, Shell shell) {
    this.configuration = configuration;
    this.shell = shell;
    
    createControl(container);
  }

  private void createControl(Composite container) {
    addToWorkingSetButton = new Button(container, SWT.CHECK);
    GridData gd_addToWorkingSetButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
    gd_addToWorkingSetButton.verticalIndent = 12;
    addToWorkingSetButton.setLayoutData(gd_addToWorkingSetButton);
    addToWorkingSetButton.setSelection(true);
    addToWorkingSetButton.setData("name", "addToWorkingSetButton");
    addToWorkingSetButton.setText("&Add project(s) to working set");
    addToWorkingSetButton.setSelection(false);
    
    final Label workingsetLabel = new Label(container, SWT.NONE);
    GridData gd_workingsetLabel = new GridData();
    gd_workingsetLabel.horizontalIndent = 10;
    workingsetLabel.setLayoutData(gd_workingsetLabel);
    workingsetLabel.setEnabled(false);
    workingsetLabel.setData("name", "workingsetLabel");
    workingsetLabel.setText("Wo&rking set:");
    
    Combo workingsetCombo = new Combo(container, SWT.READ_ONLY);
    workingsetCombo.setEnabled(false);
    workingsetCombo.setData("name", "workingsetCombo");
    workingsetCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    workingsetComboViewer = new ComboViewer(workingsetCombo);
    workingsetComboViewer.setContentProvider(new IStructuredContentProvider() {
      public Object[] getElements(Object input) {
        if(input instanceof IWorkingSet[]) {
          return (IWorkingSet[]) input;
        }
        return new IWorkingSet[0];
      }
      
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public void dispose() {
      }
    });
    workingsetComboViewer.setLabelProvider(new LabelProvider() {
      private ResourceManager images = new LocalResourceManager(JFaceResources.getResources());

      @SuppressWarnings("deprecation")
      public Image getImage(Object element) {
        if(element instanceof IWorkingSet) {
          ImageDescriptor imageDescriptor = ((IWorkingSet) element).getImage();
          if(imageDescriptor!=null) {
            try {
              return (Image) images.create(imageDescriptor);
            } catch(DeviceResourceException ex) {
              return null;
            }
          }
        }
        return super.getImage(element);
      }
      
      public String getText(Object element) {
        if(element instanceof IWorkingSet) {
          return ((IWorkingSet) element).getLabel();
        }        
        return super.getText(element);
      }

      public void dispose() {
        images.dispose();
        super.dispose();
      }
    });
    
    final Button newWorkingSetButton = new Button(container, SWT.NONE);
    newWorkingSetButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    newWorkingSetButton.setData("name", "configureButton");
    newWorkingSetButton.setText("&New...");
    newWorkingSetButton.setEnabled(false);
    newWorkingSetButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(final SelectionEvent e) {
        IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
        IWorkingSetNewWizard wizard = workingSetManager.createWorkingSetNewWizard(WORKING_SET_IDS.toArray(new String[0]));
        WizardDialog dialog = new WizardDialog(shell, wizard);
        if(dialog.open()==Window.OK) {
          IWorkingSet workingSet = wizard.getSelection();
          workingSetManager.addWorkingSet(workingSet);

          Set<IWorkingSet> workingSets = getWorkingSets();
          workingsetComboViewer.setInput(workingSets.toArray(new IWorkingSet[workingSets.size()]));
          workingsetComboViewer.setSelection(new StructuredSelection(workingSet));
        }
      }
    });
    
    Set<IWorkingSet> workingSets = getWorkingSets();
    workingsetComboViewer.setInput(workingSets.toArray(new IWorkingSet[workingSets.size()]));
    
    IWorkingSet workingSet = configuration.getWorkingSet();
    if(workingSet!=null) {
      if(workingSets.contains(workingSet)) {
        addToWorkingSetButton.setSelection(true);
        workingsetLabel.setEnabled(true);
        workingsetComboViewer.getCombo().setEnabled(true);
        newWorkingSetButton.setEnabled(true);
        workingsetComboViewer.setSelection(new StructuredSelection(workingSet));
      }
    }
    
    addToWorkingSetButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        boolean addToWorkingingSet = addToWorkingSetButton.getSelection();
        workingsetLabel.setEnabled(addToWorkingingSet);
        workingsetComboViewer.getCombo().setEnabled(addToWorkingingSet);
        newWorkingSetButton.setEnabled(addToWorkingingSet);
        if(addToWorkingingSet) {
          updateConfiguration();
        }
        else {
          configuration.setWorkingSet(null);
//          workingsetComboViewer.setSelection(null);
        }
      }
    });
    
    workingsetComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        updateConfiguration();
      }
    });
  }

  protected void updateConfiguration() {
    if (addToWorkingSetButton.getSelection()) {
      IStructuredSelection selection = (IStructuredSelection) workingsetComboViewer.getSelection();
      configuration.setWorkingSet((IWorkingSet) selection.getFirstElement());
    }
  }
  
  Set<IWorkingSet> getWorkingSets() {
    Set<IWorkingSet> workingSets = new HashSet<IWorkingSet>();
    
    IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
    for(IWorkingSet workingSet : workingSetManager.getWorkingSets()) {
      if(!workingSet.isEmpty()) {
        IAdaptable[] elements = workingSet.getElements();
        IResource resource = (IResource) elements[0].getAdapter(IResource.class);
        if(resource!=null) {
          workingSets.add(workingSet);
        }
      } else {
        if(WORKING_SET_IDS.contains(workingSet.getId())) {
          workingSets.add(workingSet);
        }
      }
    }
    
    return workingSets;
  }
  
  public void dispose() {
    workingsetComboViewer.getLabelProvider().dispose();
  }

  public void setWorkingSet(IWorkingSet workingSet) {
    workingsetComboViewer.setSelection(new StructuredSelection(workingSet));
  }
}
