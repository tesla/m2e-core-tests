/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.forms.editor.FormPage;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.PomPackage;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;


/**
 * This class provides basic page editor functionality (event listeners, readonly, etc)
 * 
 * @author akraev
 */
public abstract class EMFEditorPage extends FormPage implements Adapter {

  //parent editor
  protected final MavenPomEditor pomEditor;

  //model
  protected Model model;

  //Notifier target
  protected Notifier target;

  //are we already updating model
  protected boolean updatingModel;

  //whether view is readonly
  protected boolean readonly;

  //have we loaded data?
  private boolean dataLoaded;

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;

  protected static Map<Object, List<ModifyListener>> modifyListeners = new HashMap<Object, List<ModifyListener>>();

  public EMFEditorPage(MavenPomEditor pomEditor, String id, String title) {
    super(pomEditor, id, title);
    this.pomEditor = pomEditor;
  }

  public void setReadonly(boolean readonly) {
    if(this.readonly != readonly) {
      this.readonly = readonly;
      FormUtils.setReadonly((Composite) getPartControl(), readonly);
    }
  }

  public void setActive(boolean active) {
    super.setActive(active);
    doLoadData(active);
  }

  private void doLoadData(boolean active) {
    if(active && !dataLoaded) {
      dataLoaded = true;
      new Job("Loading pom.xml") {
        protected IStatus run(IProgressMonitor monitor) {
          try {
            model = pomEditor.readProjectDocument();
            getPartControl().getDisplay().asyncExec(new Runnable() {
              public void run() {
                updatingModel = true;
                try {
                  loadData();
                  registerListeners();
                } catch(Throwable e) {
                  MavenPlugin.log("Error loading data", e);
                } finally {
                  updatingModel = false;
                }
              }
            });
          } catch(final CoreException ex) {
            MavenPlugin.log(ex);
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
  }

  public Notifier getTarget() {
    return target;
  }

  public boolean isAdapterForType(Object type) {
    return false;
  }
  
  public void reload() {
    deRegisterListeners();
    dataLoaded = false;
    if (isActive())
      doLoadData(true);
  }

  public synchronized void notifyChanged(Notification notification) {
    if(updatingModel) {
      return;
    }
    
    updatingModel = true;
    try {
      switch(notification.getEventType()) {
        //TODO: fine-grained notification?
        case Notification.ADD:
        case Notification.MOVE:
        case Notification.REMOVE:
        case Notification.SET:
          updateView(notification);
          break;
          
        // case Notification.UNSET:
        // case Notification.ADD_MANY:
        //case Notification.REMOVE_MANY:
      }

    } catch(Exception ex) {
      MavenPlugin.log("Can't update view", ex);
    } finally {
      updatingModel = false;
    }
    
  }

  public void dispose() {
    deRegisterListeners();
    super.dispose();
  }

  public void setTarget(Notifier newTarget) {
    this.target = newTarget;
  }

  public Model getModel() {
    return model;
  }

  public EditingDomain getEditingDomain() {
    return pomEditor.getEditingDomain();
  }
  
  public abstract void loadData();

  public abstract void updateView(Notification notification);

  protected void registerListeners() {
    Iterator<?> it = model.eAllContents();
    while (it.hasNext()) {
      Object next = it.next();
      if (next instanceof EObject) {
        EObject object = (EObject) next;
        if (!object.eAdapters().contains(this))
          object.eAdapters().add(this);
      }
    }
  }

  protected void deRegisterListeners() {
    if(model!=null) {
      Iterator<?> it = model.eAllContents();
      while(it.hasNext()) {
        Object next = it.next();
        if(next instanceof EObject) {
          EObject object = (EObject) next;
          object.eAdapters().remove(this);
        }
      }
    }
  }

  public void setModifyListener(final Text textControl, final EObject object, final EStructuralFeature feature) {
    List<ModifyListener> listeners = getModifyListeners(textControl);
    Iterator<ModifyListener> it = listeners.iterator();
    while (it.hasNext()) {
      textControl.removeModifyListener(it.next());
    }
    ModifyListener listener = addModifyListener(new TextAdapter() {
      public String getText() {
        return textControl.getText();
      }
      public void addModifyListener(ModifyListener listener) {
        textControl.addModifyListener(listener);
      }
    }, object, feature);
    listeners.add(listener);
  }

  public void setModifyListener(final Combo control, final EObject object, final EStructuralFeature feature) {
    List<ModifyListener> listeners = getModifyListeners(control);
    ModifyListener listener = addModifyListener(new TextAdapter() {
      public String getText() {
        return control.getText();
      }
      public void addModifyListener(ModifyListener listener) {
        control.addModifyListener(listener);
      }
    }, object, feature);
    listeners.add(listener);
  }

  public void setModifyListener(final CCombo control, final EObject object, final EStructuralFeature feature) {
    List<ModifyListener> listeners = getModifyListeners(control);
    ModifyListener listener = addModifyListener(new TextAdapter() {
      public String getText() {
        return control.getText();
      }
      public void addModifyListener(ModifyListener listener) {
        control.addModifyListener(listener);
      }
    }, object, feature);
    listeners.add(listener);
  }
  
  private ModifyListener addModifyListener(final TextAdapter adapter, final EObject object, final EStructuralFeature feature) {
    ModifyListener listener = new ModifyListener() {
      public void modifyText(ModifyEvent e) {
            getEditingDomain().getCommandStack().execute(SetCommand.create(getEditingDomain(), object, feature, adapter.getText()));
          }
    };
    adapter.addModifyListener(listener);
    return listener;
  }
  
  public interface TextAdapter {
    String getText();
    void addModifyListener(ModifyListener listener);
  }

  public void addModifyListener(final Button control, final EObject object, final EStructuralFeature feature, final boolean deleteOthers) {
    //TODO: button doesn't have ModifyListener...
  }

  public void removeNotifyListener(Text control) {
    List<ModifyListener> listeners = getModifyListeners(control);
    Iterator<ModifyListener> it = listeners.iterator();
    while (it.hasNext()) {
      control.removeModifyListener(it.next());
    }
  }

  private List<ModifyListener> getModifyListeners(Object control) {
    List<ModifyListener> listeners = modifyListeners.get(control);
    if (listeners == null) {
      listeners = new ArrayList<ModifyListener>();
      modifyListeners.put(control, listeners);
    }
    return listeners;
  }

  public void removeNotifyListener(CCombo control) {
    List<ModifyListener> listeners = getModifyListeners(control);
    Iterator<ModifyListener> it = listeners.iterator();
    while (it.hasNext()) {
      control.removeModifyListener(it.next());
    }
  }

  public void removeNotifyListener(Combo control) {
    List<ModifyListener> listeners = getModifyListeners(control);
    Iterator<ModifyListener> it = listeners.iterator();
    while (it.hasNext()) {
      control.removeModifyListener(it.next());
    }
  }

  public void removeNotifyListener(Button optionalButton) {
    //TODO: button doesn't have ModifyListener...
  }

  public MavenProjectFacade findModuleProject(String moduleName) {
    IEditorInput editorInput = getEditorInput();
    if(editorInput instanceof IFileEditorInput) {
      // XXX is there a better way to get edited file?
      IFile pomFile = ((IFileEditorInput) editorInput).getFile();
      return findModuleProject(pomFile, moduleName);
    }
    return null;
  }

  private MavenProjectFacade findModuleProject(IFile pomFile, String module) {
    IPath modulePath = pomFile.getParent().getLocation().append(module).append("pom.xml");
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    MavenProjectFacade[] facades = projectManager.getProjects();
    for(int i = 0; i < facades.length; i++ ) {
      if(facades[i].getPom().getLocation().equals(modulePath)) {
        return facades[i];
      }
    }
    return null;
  }
  
}
