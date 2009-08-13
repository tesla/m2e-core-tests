/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views;

import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.internal.index.IndexInfo;


/**
 * RespositoryIndexDialog
 * 
 * @author Eugene Kuleshov
 */
public class RepositoryIndexDialog extends TitleAreaDialog {

  private static final String DIALOG_SETTINGS = RepositoryIndexDialog.class.getName();

  private static final String KEY_REPOSITORY_URLS = "repositoryUrls";

  private static final int MAX_HISTORY = 15;

  private String title;

  private String message;

  private Combo repositoryUrlCombo;

  private Text displayNameText;
  private IDialogSettings dialogSettings;

  private IndexInfo indexInfo;

  protected RepositoryIndexDialog(Shell shell, String title) {
    super(shell);
    this.title = title;
    this.message = "Enter Maven repository URL";
    setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL |SWT.RESIZE);
   
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
    gridLayout.numColumns = 2;
    composite.setLayout(gridLayout);
    
    Label repositoryUrlLabel = new Label(composite, SWT.NONE);
    repositoryUrlLabel.setText("&Repository URL:*");

    repositoryUrlCombo = new Combo(composite, SWT.NONE);
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
    gd.widthHint = 350;
    repositoryUrlCombo.setData("name", "repositoryUrlCombo");
    repositoryUrlCombo.setLayoutData(gd);
    repositoryUrlCombo.setItems(getSavedValues(KEY_REPOSITORY_URLS));

    if(indexInfo != null && indexInfo.getRepositoryUrl() != null) {
      repositoryUrlCombo.setText(indexInfo.getRepositoryUrl());
    }
    ModifyListener modifyListener = new ModifyListener() {
      public void modifyText(final ModifyEvent e) {
        update();
      }
    };
    repositoryUrlCombo.addModifyListener(modifyListener);
    

    Label displayNameLabel = new Label(composite, SWT.NONE);
    displayNameLabel.setText("Display name (optional):");

    displayNameText = new Text(composite, SWT.BORDER);
    displayNameText.setData("name", "displayName");
    gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
    displayNameText.setLayoutData(gd);
    if(indexInfo != null && indexInfo.hasDisplayName()){
      displayNameText.setText(indexInfo.getSimpleDisplayName());
    }
    return composite;
  }

  private String[] getSavedValues(String key) {
    String[] array = dialogSettings.getArray(key);
    return array == null ? new String[0] : array;
  }

  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(title);
  }

  public void create() {
    super.create();
    this.getShell().setSize(600,240);
    getButton(IDialogConstants.OK_ID).setEnabled(false);
  }

  protected void okPressed() {
    String repositoryUrl = repositoryUrlCombo.getText().trim();
    String indexName = getMD5HashForUrl(repositoryUrl);
    String displayName = displayNameText.getText().trim();
    URL archiveUrl = indexInfo==null ? null : indexInfo.getArchiveUrl();

    this.indexInfo = new IndexInfo(indexName, null, repositoryUrl, IndexInfo.Type.REMOTE, false);
    this.indexInfo.setArchiveUrl(archiveUrl);
    this.indexInfo.setIndexUpdateUrl(null);
    this.indexInfo.setDisplayName(displayName);
    
    saveValue(KEY_REPOSITORY_URLS, repositoryUrl);
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

  public void update() {
    boolean isValid = isValid();
    getButton(IDialogConstants.OK_ID).setEnabled(isValid);
  }


  private String getMD5HashForUrl(String repositoryUrl){
    String indexName = null;
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      digest.update(repositoryUrl.getBytes());
      byte messageDigest[] = digest.digest();
      StringBuffer hexString = new StringBuffer();
      for (int i = 0; i < messageDigest.length; i++)
      {
          String hex = Integer.toHexString(0xFF & messageDigest[i]);
          if (hex.length() == 1)
          {
              hexString.append('0');
          }
          hexString.append(hex);
      }
      indexName = hexString.toString();
    } catch(NoSuchAlgorithmException ex) {
      //this shouldn't happen with MD5
      indexName = repositoryUrl;
    }
    return indexName;
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

    String indexName = getMD5HashForUrl(repositoryUrl);
    if(indexInfo == null && indexManager.getIndexes().containsKey(indexName)) {
      setErrorMessage("Repository '" + repositoryUrl + "' already exists");
      return false;
    }

    //TODO: do we need this check anymore? I think we can't create them, so no.
//    if(indexInfo == null) {
//      IndexInfo info = indexManager.getIndexInfoByUrl(repositoryUrl);
//      if(info != null) {
//        setMessage("Repository '" + info.getDisplayName() + "' is using the same repository url", IStatus.WARNING);
//        return true;
//      }
//    }

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
