/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wizards;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.apache.maven.model.Scm;

import org.maven.ide.eclipse.project.ProjectImportConfiguration;
import org.maven.ide.eclipse.scm.ScmHandlerFactory;
import org.maven.ide.eclipse.scm.ScmHandlerUi;
import org.maven.ide.eclipse.scm.ScmUrl;


/**
 * @author Eugene Kuleshov
 */
public class MavenCheckoutLocationPage extends AbstractMavenWizardPage {

  String scmType;
  ScmUrl[] scmUrls;
  String scmParentUrl;
  
  Combo scmTypeCombo;
  
  Combo scmUrlCombo;
  
  Button scmUrlBrowseButton;
  
  Button headRevisionButton;

  Label revisionLabel;
  
  Text revisionText;
  
  Button revisionBrowseButton;
  
  private Button checkoutAllProjectsButton;
  
  Button useDefaultWorkspaceLocationButton;
  
  Label locationLabel;
  
  Combo locationCombo;

  protected MavenCheckoutLocationPage(ProjectImportConfiguration projectImportConfiguration) {
    super("MavenCheckoutLocationPage", projectImportConfiguration);
    setTitle("Target Location");
    setDescription("Select target location and revision");
  }

  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout(5, false);
    gridLayout.verticalSpacing = 0;
    composite.setLayout(gridLayout);
    setControl(composite);

    SelectionAdapter selectionAdapter = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updatePage();
      }
    };

    if(scmUrls == null || scmUrls.length < 2) {
      Label urlLabel = new Label(composite, SWT.NONE);
      urlLabel.setLayoutData(new GridData());
      urlLabel.setText("SCM &URL:");

      scmTypeCombo = new Combo(composite, SWT.READ_ONLY);
      scmTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      String[] types = ScmHandlerFactory.getTypes();
      for(int i = 0; i < types.length; i++ ) {
        scmTypeCombo.add(types[i]);
      }
      scmTypeCombo.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          String newScmType = scmTypeCombo.getText();
          if(!newScmType.equals(scmType)) {
            scmType = newScmType;
            scmUrlCombo.setText("");
            updatePage();
          }
        }
      });
      
      if(scmUrls!=null && scmUrls.length == 1) {
        try {
          scmType = ScmHandlerFactory.getType(scmUrls[0].getUrl());
        } catch(CoreException ex) {
        }
      }

      scmUrlCombo = new Combo(composite, SWT.NONE);
      scmUrlCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

      scmUrlBrowseButton = new Button(composite, SWT.NONE);
      scmUrlBrowseButton.setLayoutData(new GridData());
      scmUrlBrowseButton.setText("&Browse...");
      scmUrlBrowseButton.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          String scmType = scmTypeCombo.getText();
          ScmHandlerUi handlerUi = ScmHandlerFactory.getHandlerUiByType(scmType);
          ScmUrl scmUrl = handlerUi.selectUrl(getShell(), new ScmUrl(scmUrlCombo.getText()));
          if(scmUrl!=null) {
            scmUrlCombo.setText(scmUrl.getProviderUrl());
            if(scmUrls==null) {
              scmUrls = new ScmUrl[1];
            }
            scmUrls[0] = scmUrl;
            scmParentUrl = scmUrl.getUrl();
            updatePage();
          }
        }
      });
      
      scmUrlCombo.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          final String url = scmUrlCombo.getText();
          if(url.startsWith("scm:")) {
            try {
              final String type = ScmHandlerFactory.getType(url);
              scmTypeCombo.setText(type);
              scmType = type;
              Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                  scmUrlCombo.setText(url.substring(type.length() + 5));
                }
              });
            } catch(CoreException ex) {
            }
            return;
          }
          
          if(scmUrls==null) {
            scmUrls = new ScmUrl[1];
          }
          
          ScmUrl scmUrl = new ScmUrl("scm:" + scmType + ":" + url);
          scmUrls[0] = scmUrl;
          scmParentUrl = scmUrl.getUrl();
          updatePage();
        }
      });
      
      // TODO this should include the SCM type
      // addFieldWithHistory("scmUrl", scmUrlCombo);  
    }

    headRevisionButton = new Button(composite, SWT.CHECK);
    GridData headRevisionButtonData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 5, 1);
    headRevisionButtonData.verticalIndent = 5;
    headRevisionButton.setLayoutData(headRevisionButtonData);
    headRevisionButton.setText("Check out &Head Revision");
    headRevisionButton.setSelection(true);
    headRevisionButton.addSelectionListener(selectionAdapter);

    revisionLabel = new Label(composite, SWT.RADIO);
    GridData revisionButtonData = new GridData();
    revisionButtonData.horizontalIndent = 10;
    revisionLabel.setLayoutData(revisionButtonData);
    revisionLabel.setText("&Revision:");
    // revisionButton.addSelectionListener(selectionAdapter);

    revisionText = new Text(composite, SWT.BORDER);
    GridData revisionTextData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    revisionTextData.widthHint = 115;
    revisionTextData.verticalIndent = 3;
    revisionText.setLayoutData(revisionTextData);
    revisionText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updatePage();
      }
    });

    revisionBrowseButton = new Button(composite, SWT.NONE);
    GridData gd_revisionBrowseButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    gd_revisionBrowseButton.verticalIndent = 3;
    revisionBrowseButton.setLayoutData(gd_revisionBrowseButton);
    revisionBrowseButton.setText("&Select...");
    revisionBrowseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        String url = scmParentUrl;
        if(url==null) {
          return;
        }
        
        String scmType = scmTypeCombo.getText();
        
        ScmHandlerUi handlerUi = ScmHandlerFactory.getHandlerUiByType(scmType);
        String revision = handlerUi.selectRevision(getShell(), scmUrls[0], revisionText.getText());
        if(revision!=null) {
          revisionText.setText(revision);
          headRevisionButton.setSelection(false);
          updatePage();
        }
      }
    });

    checkoutAllProjectsButton = new Button(composite, SWT.CHECK);
    GridData checkoutAllProjectsData = new GridData(SWT.LEFT, SWT.TOP, true, false, 5, 1);
    checkoutAllProjectsData.verticalIndent = 10;
    checkoutAllProjectsButton.setLayoutData(checkoutAllProjectsData);
    checkoutAllProjectsButton.setText("Check out &All projects");
    checkoutAllProjectsButton.setSelection(true);
    checkoutAllProjectsButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updatePage();
      }
    });

    Label separatorLabel = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
    GridData labelData = new GridData(SWT.FILL, SWT.CENTER, false, false, 5, 1);
    labelData.verticalIndent = 7;
    separatorLabel.setLayoutData(labelData);

    useDefaultWorkspaceLocationButton = new Button(composite, SWT.CHECK);
    GridData useDefaultWorkspaceLocationButtonData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 5, 1);
    useDefaultWorkspaceLocationButtonData.verticalIndent = 15;
    useDefaultWorkspaceLocationButton.setLayoutData(useDefaultWorkspaceLocationButtonData);
    useDefaultWorkspaceLocationButton.setText("Use &default Workspace location");
    useDefaultWorkspaceLocationButton.addSelectionListener(selectionAdapter);
    useDefaultWorkspaceLocationButton.setSelection(true);

    locationLabel = new Label(composite, SWT.NONE);
    GridData locationLabelData = new GridData();
    locationLabelData.horizontalIndent = 10;
    locationLabel.setLayoutData(locationLabelData);
    locationLabel.setText("&Location:");

    locationCombo = new Combo(composite, SWT.NONE);
    GridData locationComboData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
    locationComboData.verticalIndent = 3;
    locationCombo.setLayoutData(locationComboData);
    addFieldWithHistory("location", locationCombo);
    locationCombo.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updatePage();
      }
    });

    Button locationBrowseButton = new Button(composite, SWT.NONE);
    GridData locationBrowseButtonData = new GridData();
    locationBrowseButtonData.verticalIndent = 3;
    locationBrowseButton.setLayoutData(locationBrowseButtonData);
    locationBrowseButton.setText("Brows&e...");
    locationBrowseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        DirectoryDialog dialog = new DirectoryDialog(getShell());
        dialog.setText("Select Location");
        
        String path = locationCombo.getText();
        if(path.length()==0) {
          path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString();
        }
        dialog.setFilterPath(path);
        
        String selectedDir = dialog.open();
        if(selectedDir != null) {
          locationCombo.setText(selectedDir);
          useDefaultWorkspaceLocationButton.setSelection(false);
          updatePage();
        }
      }
    });

    GridData advancedSettingsData = new GridData(SWT.FILL, SWT.TOP, true, false, 4, 1);
    advancedSettingsData.verticalIndent = 10;
    createAdvancedSettings(composite, advancedSettingsData);

    if(scmUrls!=null && scmUrls.length == 1) {
      scmTypeCombo.setText(scmType == null ? "" : scmType);
      scmUrlCombo.setText(scmUrls[0].getProviderUrl());
    }
    
    updatePage();
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.wizards.AbstractMavenWizardPage#setVisible(boolean)
   */
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    
    if(dialogSettings!=null) {
      String[] items = dialogSettings.getArray("scmUrl");
      if(items != null) {
        String text = scmUrlCombo.getText();
        scmUrlCombo.setItems(items);
        if (text.length() > 0) {
          // setItems() clears the text input, so we need to restore it
          scmUrlCombo.setText(text);
        }
      }
    }
  }
  
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.wizards.AbstractMavenWizardPage#dispose()
   */
  public void dispose() {
    if(dialogSettings != null) {
      
      Set history = new LinkedHashSet(MAX_HISTORY);
      
      String lastValue = scmUrlCombo.getText();
      if ( lastValue!=null && lastValue.trim().length() > 0 ) {
        history.add("scm:" + scmType + ":" + lastValue);
      }

      String[] items = scmUrlCombo.getItems();
      for(int j = 0; j < items.length && history.size() < MAX_HISTORY; j++ ) {
        history.add(items[j]);
      }
      
      dialogSettings.put("scmUrl", (String[]) history.toArray(new String[history.size()]));
    }
    
    super.dispose();
  }
  
  public IWizardContainer getContainer() {
    return super.getContainer();
  }
  
  void updatePage() {
    boolean canSelectUrl = false ;
    boolean canSelectRevision = false;
    ScmHandlerUi handlerUi = ScmHandlerFactory.getHandlerUiByType(scmType);
    if(handlerUi!=null) {
      canSelectUrl = handlerUi.canSelectUrl();
      canSelectRevision = handlerUi.canSelectRevision();
    }
    
    if(scmUrlBrowseButton!=null) {
      scmUrlBrowseButton.setEnabled(canSelectUrl);
    }

    revisionBrowseButton.setEnabled(canSelectRevision);

    boolean isHeadRevision = isHeadRevision();
    revisionLabel.setEnabled(!isHeadRevision);
    revisionText.setEnabled(!isHeadRevision);
    
    boolean defaultWorkspaceLocation = isDefaultWorkspaceLocation();
    locationLabel.setEnabled(!defaultWorkspaceLocation);
    locationCombo.setEnabled(!defaultWorkspaceLocation);
    
    setPageComplete(isPageValid());
  }

  private boolean isPageValid() {
    setErrorMessage(null);
    
    if(scmUrls != null && scmUrls.length < 2) { 
      if(scmType == null) {
        setErrorMessage("Select SCM type and URL");
        return false;
      }
    }

    ScmHandlerUi handlerUi = ScmHandlerFactory.getHandlerUiByType(scmType);
    
    if(scmUrls == null || scmUrls.length < 2) {
      if(scmUrls == null || scmUrls.length == 0) {
        setErrorMessage("SCM URL field is required");
        return false;
      }
      
      if(handlerUi!=null && !handlerUi.isValidUrl(scmUrls[0].getUrl())) {
        setErrorMessage("SCM URL is invalid");
        return false;
      }
    }
    
    if(!isHeadRevision()) {
      String revision = revisionText.getText().trim();
      if(revision.length()==0) {
        setErrorMessage("SCM revision fied is required");
        return false;
      }
      
      if(handlerUi!=null && !handlerUi.isValidRevision(null, revision)) {
        setErrorMessage("SCM revision is invalid");
        return false;
      }      
    }
    
    if(!isDefaultWorkspaceLocation()) {
      if(locationCombo.getText().trim().length()==0) {
        setErrorMessage("Location fied is required");
        return false;
      }
    }
    
    return true;
  }
  
//  public boolean canFlipToNextPage() {
//    return !isCheckoutAllProjects();
//  }
//
//  public IWizardPage getNextPage() {
//    if(isCheckoutAllProjects()) {
//      return null;
//    } else {
//      return super.getNextPage();
//    }
//  }

  public void setParent(String parentUrl) {
    this.scmParentUrl = parentUrl;
  }
  
  public void setUrls(ScmUrl[] urls) {
    this.scmUrls = urls;
  }
  
  public ScmUrl[] getUrls() {
    return scmUrls;
  }
  
  public Scm[] getScms() {
    if(scmUrls==null) {
      return new Scm[0];
    }
    
    String revision = getRevision();
    Scm[] scms = new Scm[scmUrls.length];
    for(int i = 0; i < scms.length; i++ ) {
      Scm scm = new Scm();
      scm.setConnection(scmUrls[i].getUrl());
      scm.setTag(revision);
      scms[i] = scm;
    }
    return scms;
  }
  
  public boolean isDefaultWorkspaceLocation() {
    return useDefaultWorkspaceLocationButton.getSelection();
  }
  
  public File getLocation() {
    if(isDefaultWorkspaceLocation()) {
      return ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
    }
    return new File(locationCombo.getText());
  }
  
  public boolean isCheckoutAllProjects() {
    return checkoutAllProjectsButton.getSelection();
  }

  public boolean isHeadRevision() {
    return headRevisionButton.getSelection();
  }

  public String getRevision() {
    if(isHeadRevision()) {
      return "HEAD";
    }
    return revisionText.getText().trim();
  }

  public void addListener(final SelectionListener listener) {
    ModifyListener listenerProxy = new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        Event event = new Event();
        event.widget = e.widget;
        listener.widgetSelected(new SelectionEvent(event));
      }
    };
    scmUrlCombo.addModifyListener(listenerProxy);
    revisionText.addModifyListener(listenerProxy);
    headRevisionButton.addSelectionListener(listener);
  }

}
