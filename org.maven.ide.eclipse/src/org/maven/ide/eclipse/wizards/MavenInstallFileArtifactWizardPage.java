/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wizards;

import java.io.File;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.WizardPage;
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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.digest.Sha1Digester;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.SelectionUtil;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.project.MavenProjectManager;


/**
 * Wizard page to enter parameters required for artifact installation.
 * 
 * @author Guillaume Sauthier
 * @author Mike Haller
 * @author Eugene Kuleshov
 */
public class MavenInstallFileArtifactWizardPage extends WizardPage {

  Text artifactFileNameText;
  Text pomFileNameText;

  private Combo groupIdCombo;
  private Combo artifactIdCombo;
  private Combo versionCombo;
  private Combo packagingCombo;
  private Combo classifierCombo;

  Button createChecksumButton;
  Button generatePomButton;

  private final IFile file;

  public MavenInstallFileArtifactWizardPage(IFile file) {
    super("mavenInstallFileWizardPage");
    this.file = file;
    this.setTitle("Install file in local repository");
    this.setDescription("Install file in local repository");
  }

  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(3, false));
    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    ModifyListener modifyingListener = new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        pageChanged();
      }
    };

    Label artifactFileNameLabel = new Label(container, SWT.NONE);
    artifactFileNameLabel.setText("Artifact &file:");
    
    artifactFileNameText = new Text(container, SWT.BORDER);
    artifactFileNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    artifactFileNameText.setData("name", "artifactFileNametext");
    artifactFileNameText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateFileName(getArtifactFileName());
        pageChanged();
      }
    });
    
    final Button artifactFileNameButton = new Button(container, SWT.NONE);
    artifactFileNameButton.setLayoutData(new GridData());
    artifactFileNameButton.setData("name", "externalPomFileButton");
    artifactFileNameButton.setText("&Browse...");
    artifactFileNameButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        FileDialog fileDialog = new FileDialog(artifactFileNameButton.getShell());
        fileDialog.setText("Select file");
        fileDialog.setFileName(artifactFileNameText.getText());
        String name = fileDialog.open();
        if(name!=null) {
          updateFileName(name);
        }
      }
    });

    Label pomFileNameLabel = new Label(container, SWT.NONE);
    pomFileNameLabel.setText("&POM file:");

    pomFileNameText = new Text(container, SWT.BORDER);
    pomFileNameText.setData("name", "pomFileNameText");
    pomFileNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    pomFileNameText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        generatePomButton.setSelection(getPomFileName().length()==0);
        pageChanged();
      }
    });

    final Button pomFileNameButton = new Button(container, SWT.NONE);
    pomFileNameButton.setLayoutData(new GridData());
    pomFileNameButton.setData("name", "externalPomFileButton");
    pomFileNameButton.setText("B&rowse...");
    pomFileNameButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        FileDialog fileDialog = new FileDialog(pomFileNameButton.getShell());
        fileDialog.setText("Select file");
        fileDialog.setFileName(pomFileNameText.getText());
        String res = fileDialog.open();
        if(res!=null) {
          pomFileNameText.setText(res);
        }
      }
    });
    
    new Label(container, SWT.NONE);

    generatePomButton = new Button(container, SWT.CHECK);
    generatePomButton.setData("name", "generatePomButton");
    generatePomButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    generatePomButton.setText("Gen&erate POM");
    generatePomButton.setSelection(true);
    new Label(container, SWT.NONE);

    createChecksumButton = new Button(container, SWT.CHECK);
    createChecksumButton.setData("name", "createChecksumButton");
    createChecksumButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    createChecksumButton.setText("Create C&hecksum");
    createChecksumButton.setSelection(true);

    Label separator = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
    GridData separatorData = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
    separatorData.verticalIndent = 5;
    separator.setLayoutData(separatorData);

    Label groupIdlabel = new Label(container, SWT.NONE);
    groupIdlabel.setText("&Group Id:");

    groupIdCombo = new Combo(container, SWT.NONE);
    groupIdCombo.setData("name", "groupIdCombo");
    groupIdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    groupIdCombo.addModifyListener(modifyingListener);
    new Label(container, SWT.NONE);

    Label artifactIdLabel = new Label(container, SWT.NONE);
    artifactIdLabel.setText("&Artifact Id:");

    artifactIdCombo = new Combo(container, SWT.NONE);
    artifactIdCombo.setData("name", "artifactIdCombo");
    artifactIdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    artifactIdCombo.addModifyListener(modifyingListener);
    new Label(container, SWT.NONE);

    Label versionLabel = new Label(container, SWT.NONE);
    versionLabel.setText("&Version:");

    versionCombo = new Combo(container, SWT.NONE);
    versionCombo.setData("name", "versionCombo");
    versionCombo.setText(MavenArtifactComponent.DEFAULT_VERSION);
    GridData versionComboData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    versionComboData.widthHint = 150;
    versionCombo.setLayoutData(versionComboData);
    versionCombo.addModifyListener(modifyingListener);

    Label packagingLabel = new Label(container, SWT.NONE);
    packagingLabel.setText("&Packaging:");

    packagingCombo = new Combo(container, SWT.NONE);
    packagingCombo.setData("name", "packagingCombo");
    packagingCombo.setText(MavenArtifactComponent.DEFAULT_PACKAGING);
    packagingCombo.setItems(MavenArtifactComponent.PACKAGING_OPTIONS);
    GridData packagingComboData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    packagingComboData.widthHint = 150;
    packagingCombo.setLayoutData(packagingComboData);
    packagingCombo.addModifyListener(modifyingListener);
    
    Label classifierLabel = new Label(container, SWT.NONE);
    classifierLabel.setText("&Classifier:");
    
    classifierCombo = new Combo(container, SWT.NONE);
    classifierCombo.setData("name", "classifierText");
    classifierCombo.setItems(new String[] {"sources", "javadoc"});
    GridData classifierTextData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    classifierTextData.widthHint = 150;
    classifierCombo.setLayoutData(classifierTextData);
    classifierCombo.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        generatePomButton.setSelection(getClassifier().length()==0);  
      }
    });

    if(file != null) {
      updateFileName(file.getLocation().toOSString());
    }
    
    setControl(container);
  }

  void updateFileName(String fileName) {
    if(!getArtifactFileName().equals(fileName)) {
      artifactFileNameText.setText(fileName);
    }
    
    File file = new File(fileName);
    if(!file.exists() || !file.isFile()) {
      return;
    }

    if(fileName.endsWith(".jar") || fileName.endsWith(".war") || fileName.endsWith(".ear")) {
      packagingCombo.setText(fileName.substring(fileName.length()-3));
    }

    int n = fileName.lastIndexOf('.');
    if(n>-1) {
      String pomFileName = fileName.substring(0, n) + ".pom";
      if(new File(pomFileName).exists()) {
        pomFileNameText.setText(pomFileName);
      }
    } else {
      pomFileNameText.setText("");
    }
    
    MavenPlugin plugin = MavenPlugin.getDefault();
    try {
      Sha1Digester digester = new Sha1Digester();
      String sha1 = digester.calc(file);
      plugin.getConsole().logMessage("Artifact digest " + sha1 + " for " + fileName);
      
      Map<String, IndexedArtifact> result = plugin.getIndexManager().search(sha1, IndexManager.SEARCH_SHA1);
      if(result.size()==1) {
        IndexedArtifact ia = result.values().iterator().next();
        IndexedArtifactFile iaf = ia.files.iterator().next();

        groupIdCombo.setText(iaf.group);
        artifactIdCombo.setText(iaf.artifact);
        versionCombo.setText(iaf.version);
        if(iaf.classifier!=null) {
          classifierCombo.setText(iaf.classifier);
        }
        
        String name = iaf.group + ":" + iaf.artifact + "-" + iaf.version //
            + (iaf.classifier == null ? "" : iaf.classifier);
        setMessage("Selected artifact corresponds to " + name, WARNING);
        return;
      }
    } catch(DigesterException ex) {
      MavenLogger.log("Digest calculation error", ex);
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }

    if(n>-1) {
      String pomFileName = fileName.substring(0, n) + ".pom";
      if(new File(pomFileName).exists()) {
        pomFileNameText.setText(pomFileName);
        
        // read pom file
        
        MavenProjectManager projectManager = plugin.getMavenProjectManager();
        try {
          MavenEmbedder embedder = projectManager.createWorkspaceEmbedder();
          MavenProject mavenProject = embedder.readProject(new File(pomFileName));

          groupIdCombo.setText(mavenProject.getGroupId());
          artifactIdCombo.setText(mavenProject.getArtifactId());
          versionCombo.setText(mavenProject.getVersion());
          packagingCombo.setText(mavenProject.getPackaging());
          return;
          
        } catch(CoreException ex) {
          MavenLogger.log(ex);
        } catch(Exception ex) {
          MavenLogger.log("Can't read pom file", ex);
        }
      }
    }
    
    ArtifactKey artifactKey = SelectionUtil.getType(file, ArtifactKey.class);
    if(artifactKey!=null) {
      groupIdCombo.setText(artifactKey.getGroupId());
      artifactIdCombo.setText(artifactKey.getArtifactId());
      versionCombo.setText(artifactKey.getVersion());
      if(artifactKey.getClassifier()!=null) {
        classifierCombo.setText(artifactKey.getClassifier());
      }
    }
  }

  void pageChanged() {
    String artifactFileName = getArtifactFileName();
    if(artifactFileName.length() == 0) {
      updateStatus("Artifact file name must be specified");
      return;
    }

    File file = new File(artifactFileName);
    if(!file.exists() || !file.isFile()) {
      updateStatus("Artifact file does not exist");
      return;
    }
    
    String pomFileName = getPomFileName();
    if(pomFileName.length()>0) {
      if(!new File(pomFileName).exists()) {
        updateStatus("POM file does not exist");
        return;
      }
    }

    if(getGroupId().length() == 0) {
      updateStatus("Group Id must be specified");
      return;
    }

    if(getArtifactId().length() == 0) {
      updateStatus("Artifact Id must be specified");
      return;
    }

    if(getVersion().length() == 0) {
      updateStatus("Version must be specified");
      return;
    }

    if(getPackaging().length() == 0) {
      updateStatus("Packaging must be specified");
      return;
    }

    updateStatus(null);
  }

  private void updateStatus(String message) {
    setErrorMessage(message);
    setPageComplete(message == null);
  }

  public String getArtifactFileName() {
    return artifactFileNameText.getText().trim();
  }

  public String getPomFileName() {
    return pomFileNameText.getText().trim();
  }

  public String getGroupId() {
    return groupIdCombo.getText().trim();
  }

  public String getArtifactId() {
    return artifactIdCombo.getText().trim();
  }

  public String getVersion() {
    return versionCombo.getText().trim();
  }

  public String getPackaging() {
    return packagingCombo.getText().trim();
  }

  public String getClassifier() {
    return this.classifierCombo.getText().trim();
  }

  public boolean isGeneratePom() {
    return generatePomButton.getSelection();
  }

  public boolean isCreateChecksum() {
    return createChecksumButton.getSelection();
  }

}
