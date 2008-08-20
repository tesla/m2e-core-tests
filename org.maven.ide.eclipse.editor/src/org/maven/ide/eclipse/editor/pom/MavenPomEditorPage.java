/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import static org.maven.ide.eclipse.editor.pom.FormUtils.isEmpty;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.Parent;
import org.maven.ide.components.pom.PomPackage;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;


/**
 * This class provides basic page editor functionality (event listeners, readonly, etc)
 * 
 * @author Anton Kraev
 * @author Eugene Kuleshov
 */
public abstract class MavenPomEditorPage extends FormPage implements Adapter {

  // parent editor
  protected final MavenPomEditor pomEditor;

  // model
  protected Model model;

  // Notifier target
  protected Notifier target;

  // are we already updating model
  protected boolean updatingModel;

  // have we loaded data?
  private boolean dataLoaded;

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;

  protected static Map<Object, List<ModifyListener>> modifyListeners = new HashMap<Object, List<ModifyListener>>();

  public MavenPomEditorPage(MavenPomEditor pomEditor, String id, String title) {
    super(pomEditor, id, title);
    this.pomEditor = pomEditor;
  }
  
  public MavenPomEditor getPomEditor() {
    return pomEditor;
  }

  protected void createFormContent(IManagedForm managedForm) {
    ScrolledForm form = managedForm.getForm();
    IToolBarManager toolBarManager = form.getToolBarManager();

    toolBarManager.add(new Action("Open Parent POM", MavenEditorImages.PARENT_POM) {
      public void run() {
        // XXX listen to parent modification and accordingly enable/disable action
        final Parent parent = model.getParent();
        if(parent!=null && !isEmpty(parent.getGroupId()) && !isEmpty(parent.getArtifactId()) && !isEmpty(parent.getVersion())) {
          new Job("Opening POM") {
            protected IStatus run(IProgressMonitor arg0) {
              OpenPomAction.openEditor(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
              return Status.OK_STATUS;
            }
          }.schedule();
        }
      }
    });
    
    toolBarManager.add(new Action("Show Effective POM", MavenEditorImages.EFFECTIVE_POM) {
      public void run() {
        new Job("Opening Effective POM") {
          protected IStatus run(IProgressMonitor monitor) {
            try {
              StringWriter sw = new StringWriter();
              
              MavenProject mavenProject = pomEditor.readMavenProject(false, monitor);
              new MavenXpp3Writer().write(sw, mavenProject.getModel());
              
              String effectivePom = sw.toString();
              
              // XXX workaround to make EMF recognize namespace (may not be needed anymore)
//              effectivePom = effectivePom.replaceAll("<project>", "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"" + 
//                  " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + 
//                  " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">");
              
              String name = pomEditor.getPartName() + " [effective]";
              IEditorInput editorInput = new OpenPomAction.MavenEditorStorageInput(name, name, null, //
                  effectivePom.getBytes("UTF-8"));
              OpenPomAction.openEditor(editorInput, "pom.xml");
              
            } catch(CoreException ex) {
              MavenLogger.log(ex);
            } catch(MavenEmbedderException ex) {
              MavenLogger.log("Unable to read Maven pom", ex);
            } catch(IOException ex) {
              MavenLogger.log("Unable to create Effective POM", ex);
            }
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    });
    
    toolBarManager.add(new Action("Refresh", MavenEditorImages.REFRESH) {
      public void run() {
        pomEditor.reload();
      }
    });
    
    form.updateToolBar();

    // compatibility proxy to support Eclipse 3.2
    FormUtils.decorateHeader(managedForm.getToolkit(), form.getForm());
  }
  
  public void setActive(boolean active) {
    super.setActive(active);
    doLoadData(active);

    FormUtils.setReadonly((Composite) getPartControl(), isReadOnly());
  }

  public boolean isReadOnly() {
    return pomEditor.isReadOnly();
  }

  private void doLoadData(boolean active) {
    if(active && !dataLoaded) {
      dataLoaded = true;
//      new Job("Loading pom.xml") {
//        protected IStatus run(IProgressMonitor monitor) {
          try {
            model = pomEditor.readProjectDocument();
            if(model != null) {
              if (getPartControl() != null) {
                getPartControl().getDisplay().asyncExec(new Runnable() {
                  public void run() {
                    updatingModel = true;
                    try {
                      loadData();
                      registerListeners();
                    } catch(Throwable e) {
                      MavenLogger.log("Error loading data", e);
                    } finally {
                      updatingModel = false;
                    }
                  }
                });
              }
            }
          } catch(final CoreException ex) {
            MavenLogger.log(ex);
            getPartControl().getDisplay().asyncExec(new Runnable() {
              public void run() {
                FormUtils.setMessage(getManagedForm().getForm(), ex.getMessage(), IMessageProvider.ERROR);
              }
            });
          }

//          return Status.OK_STATUS;
//        }
//      }.schedule();
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
    boolean oldDataLoaded = dataLoaded;
    dataLoaded = false;
    doLoadData(oldDataLoaded);
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
          if (getManagedForm() != null)
          updateView(notification);
          break;
          
        default:
          break;
          
        // case Notification.UNSET:
        // case Notification.ADD_MANY:
        // case Notification.REMOVE_MANY:
      }

    } catch(Exception ex) {
      MavenLogger.log("Can't update view", ex);
    } finally {
      updatingModel = false;
    }
    
    registerListeners();
  }

  public void dispose() {
    deRegisterListeners();
    
    for(Map.Entry<Object, List<ModifyListener>> e : modifyListeners.entrySet()) {
      Object control = e.getKey();
      for(ModifyListener listener : e.getValue()) {
        if(control instanceof Text) {
          Text textControl = (Text) control;
          if(!textControl.isDisposed()) {
            textControl.removeModifyListener(listener);
          }
        } else if(control instanceof Combo) {
          Combo comboControl = (Combo) control;
          if(!comboControl.isDisposed()) {
            comboControl.removeModifyListener(listener);
          }
        } else if(control instanceof CCombo) {
          CCombo comboControl = (CCombo) control;
          if(!comboControl.isDisposed()) {
            comboControl.removeModifyListener(listener);
          }
        } else if(control instanceof Combo) {
          Button buttonControl = (Button) control;
          if(!buttonControl.isDisposed()) {
            buttonControl.removeSelectionListener((SelectionListener) listener);
          }
        }
      }
    }
    
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

  public void registerListeners() {
    if(model!=null) {
      if(!model.eAdapters().contains(this))
        model.eAdapters().add(this);
      for(Iterator<?> it = model.eAllContents(); it.hasNext();) {
        Object next = it.next();
        if (next instanceof EObject) {
          EObject object = (EObject) next;
          if (!object.eAdapters().contains(this)) {
            object.eAdapters().add(this);
          }
        }
      }
    }
  }

  public void deRegisterListeners() {
    if(model!=null) {
      model.eAdapters().remove(this);
      for(Iterator<?> it = model.eAllContents(); it.hasNext(); ) {
        Object next = it.next();
        if(next instanceof EObject) {
          EObject object = (EObject) next;
          object.eAdapters().remove(this);
        }
      }
    }
  }

  public <T> void setModifyListener(final Text textControl, ValueProvider<T> owner, EStructuralFeature feature,
      String defaultValue) {
    if(textControl!=null && !textControl.isDisposed()) {
      List<ModifyListener> listeners = getModifyListeners(textControl);
      for(ModifyListener listener : listeners) {
        textControl.removeModifyListener(listener);
      }
      listeners.clear();
      ModifyListener listener = setModifyListener(new TextAdapter() {
        public String getText() {
          return textControl.getText();
        }
        public void addModifyListener(ModifyListener listener) {
          textControl.addModifyListener(listener);
        }
      }, owner, feature, defaultValue);
      listeners.add(listener);
    }
  }

  public <T> void setModifyListener(final Combo control, ValueProvider<T> owner, EStructuralFeature feature) {
    if(control!=null && !control.isDisposed()) {
      List<ModifyListener> listeners = getModifyListeners(control);
      for(ModifyListener listener : listeners) {
        control.removeModifyListener(listener);
      }
      listeners.clear();
      ModifyListener listener = setModifyListener(new TextAdapter() {
        public String getText() {
          return control.getText();
        }
        public void addModifyListener(ModifyListener listener) {
          control.addModifyListener(listener);
        }
      }, owner, feature, null);
      listeners.add(listener);
    }
  }

  public <T> void setModifyListener(final CCombo control, ValueProvider<T> owner, EStructuralFeature feature,
      String defaultValue) {
    if(control!=null && !control.isDisposed()) {
      List<ModifyListener> listeners = getModifyListeners(control);
      for(ModifyListener listener : listeners) {
        control.removeModifyListener(listener);
      }
      listeners.clear();
      ModifyListener listener = setModifyListener(new TextAdapter() {
        public String getText() {
          return control.getText();
        }
        public void addModifyListener(ModifyListener listener) {
          control.addModifyListener(listener);
        }
      }, owner, feature, defaultValue);
      listeners.add(listener);
    }
  }
  
  private <T> ModifyListener setModifyListener(final TextAdapter adapter, final ValueProvider<T> provider,
      final EStructuralFeature feature, final String defaultValue) {
    ModifyListener listener = new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        T owner = provider.getValue();
        if(owner==null && !provider.isEmpty()) {
          CompoundCommand compoundCommand = new CompoundCommand();
          provider.create(getEditingDomain(), compoundCommand);
          getEditingDomain().getCommandStack().execute(compoundCommand);
          owner = provider.getValue();
        }
        
        Command command;
        if(adapter.getText().equals(defaultValue) || isEmpty(adapter.getText())) {
          command = SetCommand.create(getEditingDomain(), owner, feature, null);
        } else {
          command = SetCommand.create(getEditingDomain(), owner, feature, adapter.getText());
        }
        getEditingDomain().getCommandStack().execute(command);
        registerListeners();
      }
    };
    adapter.addModifyListener(listener);
    return listener;
  }
  
  public <T> void setModifyListener(final Button control, final ValueProvider<T> provider,
      final EStructuralFeature feature, final String defaultValue) {
    if(control!=null && !control.isDisposed()) {
      List<ModifyListener> listeners = getModifyListeners(control);
      for(ModifyListener listener : listeners) {
        control.removeSelectionListener((SelectionListener) listener);
      }
  
      listeners.clear();

      class ButtonModifyListener extends SelectionAdapter implements ModifyListener {
        public void widgetSelected(SelectionEvent e) {
          T owner = provider.getValue();
          if(owner == null && !provider.isEmpty()) {
            CompoundCommand compoundCommand = new CompoundCommand();
            provider.create(getEditingDomain(), compoundCommand);
            getEditingDomain().getCommandStack().execute(compoundCommand);
            owner = provider.getValue();
          }
  
          String value = control.getSelection() ? "true" : "false";
          Command command = SetCommand.create(getEditingDomain(), owner, feature, //
              defaultValue.equals(value) ? null : value);
          getEditingDomain().getCommandStack().execute(command);
          registerListeners();
        }
  
        public void modifyText(ModifyEvent e) {
          widgetSelected(null);
        }
      };
  
      ButtonModifyListener listener = new ButtonModifyListener();
      control.addSelectionListener(listener);
  
      listeners.add(listener);
    }
  }

  public void removeNotifyListener(Text control) {
    List<ModifyListener> listeners = getModifyListeners(control);
    for(ModifyListener listener : listeners) {
      if(!control.isDisposed()) {
        control.removeModifyListener(listener);
      }
    }
    listeners.clear();
  }

  public void removeNotifyListener(CCombo control) {
    List<ModifyListener> listeners = getModifyListeners(control);
    for(ModifyListener listener : listeners) {
      if(!control.isDisposed()) {
        control.removeModifyListener(listener);
      }
    }
    listeners.clear();
  }

  public void removeNotifyListener(Combo control) {
    List<ModifyListener> listeners = getModifyListeners(control);
    for(ModifyListener listener : listeners) {
      if(!control.isDisposed()) {
        control.removeModifyListener(listener);
      }
    }
    listeners.clear();
  }

  public void removeNotifyListener(Button button) {
    List<ModifyListener> listeners = getModifyListeners(button);
    for(ModifyListener listener : listeners) {
      if(!button.isDisposed()) {
        button.removeSelectionListener((SelectionAdapter) listener);
      }
    }
    listeners.clear();
  }

  private List<ModifyListener> getModifyListeners(Object control) {
    List<ModifyListener> listeners = modifyListeners.get(control);
    if (listeners == null) {
      listeners = new ArrayList<ModifyListener>();
      modifyListeners.put(control, listeners);
    }
    return listeners;
  }
  
  public IMavenProjectFacade findModuleProject(String moduleName) {
    IEditorInput editorInput = getEditorInput();
    if(editorInput instanceof IFileEditorInput) {
      // XXX is there a better way to get edited file?
      IFile pomFile = ((IFileEditorInput) editorInput).getFile();
      return findModuleProject(pomFile, moduleName);
    }
    return null;
  }

  private IMavenProjectFacade findModuleProject(IFile pomFile, String module) {
    IPath modulePath = pomFile.getParent().getLocation().append(module).append("pom.xml");
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    IMavenProjectFacade[] facades = projectManager.getProjects();
    for(int i = 0; i < facades.length; i++ ) {
      if(facades[i].getPom().getLocation().equals(modulePath)) {
        return facades[i];
      }
    }
    return null;
  }
  /*
   * returns added/removed/updated EObject from notification (convenience method for detail forms)
   */
  public static Object getFromNotification(Notification notification) {
    if(notification.getFeature() != null && !(notification.getFeature() instanceof EAttribute)) {
      // for structuralFeatures, return new value (for insert/delete)
      return notification.getNewValue();
    } else {
      // for attributes, return the notifier as it contains all new attributes (attribute modified)
      return notification.getNotifier();
    }
  }

  /**
   * Adapter for Text, Combo and CCombo widgets 
   */
  public interface TextAdapter {
    String getText();
    void addModifyListener(ModifyListener listener);
  }
  
}

