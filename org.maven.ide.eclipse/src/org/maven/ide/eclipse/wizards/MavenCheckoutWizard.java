/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wizards;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import org.apache.maven.model.Scm;

import org.maven.ide.eclipse.actions.SelectionUtil;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.project.MavenProjectScmInfo;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;
import org.maven.ide.eclipse.scm.ScmUrl;


/**
 * Maven checkout wizard
 * 
 * @author Eugene Kuleshov
 */
public class MavenCheckoutWizard extends Wizard implements IImportWizard, INewWizard {

  private final ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration();
  
  private ScmUrl[] urls;

  private String parentUrl;
  
  private MavenCheckoutLocationPage scheckoutPage;

  private MavenProjectWizardLocationPage locationPage;
  
  private IStructuredSelection selection;

  
  public MavenCheckoutWizard() {
    this(null);
    setNeedsProgressMonitor(true);
  }

  public MavenCheckoutWizard(ScmUrl[] urls) {
    setUrls(urls);
    setNeedsProgressMonitor(true);
    setWindowTitle("Checkout as Maven project from SCM");
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.selection = selection;
    
    importConfiguration.setWorkingSet(SelectionUtil.getSelectedWorkingSet(selection));
    
    ArrayList<ScmUrl> urls = new ArrayList<ScmUrl>();
    IAdapterManager adapterManager = Platform.getAdapterManager();
    for(Iterator<?> it = selection.iterator(); it.hasNext();) {
      ScmUrl url = (ScmUrl) adapterManager.getAdapter(it.next(), ScmUrl.class);
      if(url!=null) {
        urls.add(url);
      }
    }
    setUrls(urls.toArray(new ScmUrl[urls.size()]));
  }
  
  private void setUrls(ScmUrl[] urls) {
    if(urls!=null && urls.length>0) {
      this.urls = urls;
      this.parentUrl = getParentUrl(urls);
    }
  }

  private String getParentUrl(ScmUrl[] urls) {
    if(urls.length==1) {
      return urls[0].getUrl();
    }
    
    String parent = urls[0].getParentUrl();
    for(int i = 1; parent!=null && i < urls.length; i++ ) {
      String url = urls[i].getParentUrl();
      if(!parent.equals(url)) {
        parent = null;
      }
    }
    return parent;
  }
  
  public void addPages() {
    scheckoutPage = new MavenCheckoutLocationPage(importConfiguration);
    scheckoutPage.setUrls(urls);
    scheckoutPage.setParent(parentUrl);
    
    locationPage = new MavenProjectWizardLocationPage(importConfiguration, //
        "Select Project Location",
        "Select project location and working set");
    locationPage.setLocationPath(SelectionUtil.getSelectedLocation(selection));
    
    addPage(scheckoutPage);
    addPage(locationPage);
  }
  
//  /** Adds the listeners after the page controls are created. */
//  public void createPageControls(Composite pageContainer) {
//    super.createPageControls(pageContainer);
//
//    locationPage.addListener(new SelectionAdapter() {
//      public void widgetSelected(SelectionEvent e) {
//        projectsPage.setScms(locationPage.getScms(new NullProgressMonitor()));
//      }
//    });
//    
//    projectsPage.setScms(locationPage.getScms(new NullProgressMonitor()));
//  }

  public boolean canFinish() {
    if(scheckoutPage.isCheckoutAllProjects() && scheckoutPage.isPageComplete()) {
      return true;
    }
    return super.canFinish();
  }

  public boolean performFinish() {
    if(!canFinish()) {
      return false;
    }

    final boolean checkoutAllProjects = scheckoutPage.isCheckoutAllProjects();

    Scm[] scms = scheckoutPage.getScms();
    
    final Collection<MavenProjectScmInfo> mavenProjects = new ArrayList<MavenProjectScmInfo>();
    for(int i = 0; i < scms.length; i++ ) {
      String url = scms[i].getConnection();
      String revision = scms[i].getTag();
      
      if(url.endsWith("/")) {
        url = url.substring(0, url.length()-1);
      }
      
      int n = url.lastIndexOf("/");
      String label = (n == -1 ? url : url.substring(n)) + "/" + IMavenConstants.POM_FILE_NAME;
      MavenProjectScmInfo projectInfo = new MavenProjectScmInfo(label, null, //
          null, revision, url, url);
      mavenProjects.add(projectInfo);
    }

    MavenProjectCheckoutJob job = new MavenProjectCheckoutJob(importConfiguration, checkoutAllProjects) {
      protected Collection<MavenProjectScmInfo> getProjects(IProgressMonitor monitor) {
        return mavenProjects;
      }
    };

    if(!locationPage.isInWorkspace()) {
      job.setLocation(locationPage.getLocationPath().toFile());
    }
    
    job.schedule();

    return true;
  }

}
