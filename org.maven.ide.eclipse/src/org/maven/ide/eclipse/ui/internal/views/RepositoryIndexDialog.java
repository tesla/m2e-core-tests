/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.index.IndexInfo;
import org.maven.ide.eclipse.index.IndexManager;


/**
 * RespositoryIndexDialog
 * 
 * @author Eugene Kuleshov
 */
public class RepositoryIndexDialog extends TitleAreaDialog {

  private static final String DIALOG_SETTINGS = RepositoryIndexDialog.class.getName();

  private static final String KEY_INDEX_NAMES = "indexNames";

  private static final String KEY_REPOSITORY_URLS = "repositoryUrls";

  private static final String KEY_UPDATE_URLS = "updateUrls";

  private static final int MAX_HISTORY = 15;

  private String title;

  private String message;

  Combo repositoryIdCombo;

  Combo repositoryUrlCombo;

  Combo indexUpdateUrlCombo;

  Text lastUpdateText;
  
  private Button retrieveButton;
  
//  private Button fullIndexButton;

  private IDialogSettings dialogSettings;

  private IndexInfo indexInfo;

  protected RepositoryIndexDialog(Shell shell, String title) {
    super(shell);
    this.title = title;
    this.message = "Enter Maven repository URL and repository Id";
    setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

    IDialogSettings pluginSettings = MavenPlugin.getDefault().getDialogSettings();
    dialogSettings = pluginSettings.getSection(DIALOG_SETTINGS);
    if(dialogSettings == null) {
      dialogSettings = new DialogSettings(DIALOG_SETTINGS);
      pluginSettings.addSection(dialogSettings);
    }
  }

  protected Control createContents(Composite parent) {
    Control control = super.createContents(parent);
    setTitle(title);
    setMessage(message);
    return control;
  }

  protected Control createDialogArea(Composite parent) {
    Composite composite1 = (Composite) super.createDialogArea(parent);

    Composite composite = new Composite(composite1, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginTop = 7;
    gridLayout.marginWidth = 12;
    gridLayout.numColumns = 3;
    composite.setLayout(gridLayout);

    Label repositoryUrlLabel = new Label(composite, SWT.NONE);
    repositoryUrlLabel.setText("&Repository URL:*");

    repositoryUrlCombo = new Combo(composite, SWT.NONE);
    GridData repositoryUrlTextData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    repositoryUrlTextData.widthHint = 350;
    repositoryUrlCombo.setLayoutData(repositoryUrlTextData);
    repositoryUrlCombo.setItems(getSavedValues(KEY_REPOSITORY_URLS));

    Label indexUpdateUrlLabel = new Label(composite, SWT.NONE);
    indexUpdateUrlLabel.setText("Index &Update URL:");

    indexUpdateUrlCombo = new Combo(composite, SWT.NONE);
    indexUpdateUrlCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    indexUpdateUrlCombo.setItems(getSavedValues(KEY_UPDATE_URLS));
    
    Label indexNameLabel = new Label(composite, SWT.NONE);
    indexNameLabel.setText("Repository &Id:*");

    repositoryIdCombo = new Combo(composite, SWT.NONE);
    repositoryIdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    repositoryIdCombo.setItems(getSavedValues(KEY_INDEX_NAMES));

    retrieveButton = new Button(composite, SWT.NONE);
    retrieveButton.setText("&Retrieve");
    retrieveButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    retrieveButton.setEnabled(false);
    retrieveButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(final SelectionEvent e) {
        final String repositoryUrl = repositoryUrlCombo.getText();
        final String indexUpdateUrl = indexUpdateUrlCombo.getText();
        new Job("Retrieving index properties") {
          protected IStatus run(IProgressMonitor monitor) {
            try {
              IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
              final Properties properties = indexManager.fetchIndexProperties(repositoryUrl,
                  indexUpdateUrl.length() > 0 ? indexUpdateUrl : null, monitor);
              if(properties!=null) {
                Display.getDefault().asyncExec(new Runnable() {
                  public void run() {
                    String indexId = properties.getProperty(IndexManager.INDEX_ID, null);
                    if(indexId!=null && indexId.length()>0) {
                      repositoryIdCombo.setText(indexId);
                    }
                    String indexTime = properties.getProperty(IndexManager.INDEX_TIMESTAMP, null);
                    if(indexTime!=null) {
                      try {
                        SimpleDateFormat df = new SimpleDateFormat(IndexManager.INDEX_TIME_FORMAT);
                        lastUpdateText.setText(df.parse(indexTime).toString());
                      } catch(ParseException ex) {
                        // ignore
                      }
                    }
                  }
                });
              }
            } catch(final CoreException ex) {
              MavenLogger.log(ex);
              Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                  IStatus status = ex.getStatus();
                  setErrorMessage(status.getMessage() + ";\n" + status.getException().getMessage());
                }
              });
            }
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    });

    Label lastUpdateLabel = new Label(composite, SWT.NONE);
    lastUpdateLabel.setText("Index Update Time:");

    lastUpdateText = new Text(composite, SWT.READ_ONLY);
    lastUpdateText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

    if(indexInfo != null) {
      repositoryIdCombo.setText(indexInfo.getIndexName());
      
      if(indexInfo.getRepositoryUrl() != null) {
        repositoryUrlCombo.setText(indexInfo.getRepositoryUrl());
      }
      if(indexInfo.getIndexUpdateUrl() != null) {
        indexUpdateUrlCombo.setText(indexInfo.getIndexUpdateUrl());
      }

      Date updateTime = indexInfo.getUpdateTime();
      if(updateTime!=null) {
        lastUpdateText.setText(updateTime.toString());
      } else {
        lastUpdateText.setText("unknown");
      }
      
      retrieveButton.setEnabled(isRetrieveEnabled());
    }

//    fullIndexButton = new Button(composite, SWT.CHECK);
//    fullIndexButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
//    fullIndexButton.setText("&Full Index");
//    fullIndexButton.setSelection(true);

    ModifyListener modifyListener = new ModifyListener() {
      public void modifyText(final ModifyEvent e) {
        update();
      }
    };
    repositoryUrlCombo.addModifyListener(modifyListener);
    indexUpdateUrlCombo.addModifyListener(modifyListener);
    repositoryIdCombo.addModifyListener(modifyListener);
    
    return composite;
  }

  private String[] getSavedValues(String key) {
    String[] array = dialogSettings.getArray(key);
    return array == null ? new String[0] : array;
  }

  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(title);
    // shell.setImage(MavenPlugin.getImage(icon));
  }

  public void create() {
    super.create();
    getButton(IDialogConstants.OK_ID).setEnabled(false);
  }

  protected void okPressed() {
    String indexName = repositoryIdCombo.getText().trim();
    String repositoryUrl = repositoryUrlCombo.getText().trim();
    String indexUpdateUrl = indexUpdateUrlCombo.getText().trim();

    URL archiveUrl = indexInfo==null ? null : indexInfo.getArchiveUrl();
    // boolean isShort = !fullIndexButton.getSelection();

    this.indexInfo = new IndexInfo(indexName, null, repositoryUrl, IndexInfo.Type.REMOTE, false);
    this.indexInfo.setArchiveUrl(archiveUrl);
    if(indexUpdateUrl.trim().length()==0) {
      this.indexInfo.setIndexUpdateUrl(null);
    } else {
      this.indexInfo.setIndexUpdateUrl(indexUpdateUrl);
    }

    saveValue(KEY_INDEX_NAMES, indexName);
    saveValue(KEY_REPOSITORY_URLS, repositoryUrl);
    saveValue(KEY_UPDATE_URLS, indexUpdateUrl);

    super.okPressed();
  }

  private void saveValue(String key, String value) {
    List<String> dirs = new ArrayList<String>();
    dirs.addAll(Arrays.asList(getSavedValues(key)));

    dirs.remove(value);
    dirs.add(0, value);

    if(dirs.size() > MAX_HISTORY) {
      dirs = dirs.subList(0, MAX_HISTORY);
    }

    dialogSettings.put(key, dirs.toArray(new String[dirs.size()]));
  }

  void update() {
    boolean isValid = isValid();
    retrieveButton.setEnabled(isRetrieveEnabled());
    getButton(IDialogConstants.OK_ID).setEnabled(isValid);
  }

  private boolean isRetrieveEnabled() {
    return repositoryUrlCombo.getText().length()>0 || indexUpdateUrlCombo.getText().length()>0;
  }

  private boolean isValid() {
    setErrorMessage(null);
    setMessage(null, IStatus.WARNING);

    IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
    
    String repositoryUrl = repositoryUrlCombo.getText();
    if(repositoryUrl == null || repositoryUrl.trim().length() == 0) {
      setErrorMessage("Repository URL is required");
      return false;
    }
    repositoryUrl = repositoryUrl.trim();
    if(!repositoryUrl.startsWith("http://") && !repositoryUrl.startsWith("https://")) {
      // TODO support other Wagon protocols
      setErrorMessage("Repository URL should use http:// or https:// protocol");
      return false;
    }
    
    String indexUpdateUrl = indexUpdateUrlCombo.getText();
    if(indexUpdateUrl.length()>0) {
      if(!indexUpdateUrl.startsWith("http://") && !indexUpdateUrl.startsWith("https://")) {
        // TODO support other Wagon protocols
        setErrorMessage("Index Update URL should use http:// or https:// protocol");
        return false;
      }
    }
    
    String indexName = repositoryIdCombo.getText();
    if(indexName == null || indexName.trim().length() == 0) {
      setErrorMessage("Specify Repository Id or use 'Retrieve' button to fetch repository id.");
      return false;
    }
    indexName = indexName.trim();
    if(!indexName.matches("(\\w|\\.|\\-)*")) {
      setErrorMessage("Repository Id can ony contain alpanumenric characters, '-' and '.'");
      return false;
    }
    if(indexInfo == null && indexManager.getIndexes().containsKey(indexName)) {
      setErrorMessage("Repository '" + indexName + "' already exist");
      return false;
    }

    if(indexInfo == null) {
      IndexInfo info = indexManager.getIndexInfoByUrl(repositoryUrl);
      if(info != null) {
        setMessage("Repository '" + info.getIndexName() + "' is using the same repository url", IStatus.WARNING);
        return true;
      }
    }

    setMessage(message);
    return true;
  }

  public IndexInfo getIndexInfo() {
    return indexInfo;
  }

  public void setIndexInfo(IndexInfo indexInfo) {
    this.indexInfo = indexInfo;
  }

}
