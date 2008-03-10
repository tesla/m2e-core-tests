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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.project.BuildPathManager;
import org.maven.ide.eclipse.project.MavenProjectPomScanner;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;
import org.maven.ide.eclipse.scm.MavenProjectCheckoutJob;


/**
 * A wizard used to import projects for Maven artifacts 
 * 
 * @author Eugene Kuleshov
 */
public class MavenMaterializePomWizard extends Wizard implements IImportWizard, INewWizard {

  ProjectImportConfiguration importConfiguration;
  
  MavenDependenciesWizardPage selectionPage;
  
  private Dependency[] dependencies;


  public MavenMaterializePomWizard() {
    importConfiguration = new ProjectImportConfiguration();
    setNeedsProgressMonitor(true);
    setWindowTitle("Import Maven Projects");
  }

  public void setDependencies(Dependency[] dependencies) {
    this.dependencies = dependencies;
  }
  
  public Dependency[] getDependencies() {
    return dependencies;
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    ArrayList dependencies = new ArrayList();

    BuildPathManager buildpathManager = MavenPlugin.getDefault().getBuildpathManager();
    for(Iterator it = selection.iterator(); it.hasNext();) {
      Object element = it.next();
      try {
        if(element instanceof IPackageFragmentRoot) {
          IPackageFragmentRoot fragment = (IPackageFragmentRoot) element;
          IProject project = fragment.getJavaProject().getProject();
          if(project.isAccessible() && fragment.isArchive()) {
            Artifact a = buildpathManager.findArtifact(project, fragment.getPath());
            if(a!=null) {
              Dependency d = new Dependency();
              d.setGroupId(a.getGroupId());
              d.setArtifactId(a.getArtifactId());
              d.setVersion(a.getVersion());
              d.setClassifier(a.getClassifier());
              d.setType(a.getType());
              d.setScope(a.getScope());
              dependencies.add(d);
            }
          }
        } else if(element instanceof IndexedArtifactFile) {
          dependencies.add(((IndexedArtifactFile) element).getDependency());
          
        }
      } catch(CoreException ex) {
        MavenPlugin.log(ex);
      }
    }
    
    setDependencies((Dependency[]) dependencies.toArray(new Dependency[dependencies.size()]));
  }

  public void addPages() {
    selectionPage = new MavenDependenciesWizardPage(importConfiguration, //
        "Select Maven projects", //
        "Select Maven artifacts to import");
    selectionPage.showLocation(true);
    selectionPage.setDependencies(dependencies);
    addPage(selectionPage);
  }
  
  /** Adds the listeners after the page controls are created. */
//  public void createPageControls(Composite pageContainer) {
//    super.createPageControls(pageContainer);
//
//    selectionPage.addListener(new ISelectionChangedListener() {
//      public void selectionChanged(SelectionChangedEvent event) {
//        projectsPage.setDependencies(selectionPage.getDependencies());
//      }
//    });
//  }

  public boolean canFinish() {
//    if(locationPage.isCheckoutAllProjects() && locationPage.isPageComplete()) {
//      return true;
//    }
    return super.canFinish();
  }

  public boolean performFinish() {
    if(!canFinish()) {
      return false;
    }

    final Dependency[] dependencies = selectionPage.getDependencies();
    
    final boolean checkoutAllProjects = selectionPage.isCheckoutAllProjects();
    final boolean developer = selectionPage.isDeveloperConnection();
    
    MavenProjectCheckoutJob job = new MavenProjectCheckoutJob(importConfiguration, checkoutAllProjects) {
      protected Collection getProjects(IProgressMonitor monitor) throws InterruptedException {
        MavenPlugin plugin = MavenPlugin.getDefault();
        MavenProjectPomScanner scanner = new MavenProjectPomScanner(developer, dependencies, //
            plugin.getMavenModelManager(), plugin.getMavenEmbedderManager(), //
            plugin.getIndexManager(), plugin.getConsole());
        scanner.run(monitor);
        // XXX handle errors/warnings
        
        return scanner.getProjects();
      }
    };
    
    if(!selectionPage.isDefaultWorkspaceLocation()) {
      job.setLocation(selectionPage.getLocation());
    }
    
    job.schedule();

    return true;
  }
  
//  public Scm[] getScms(IProgressMonitor monitor) {
//    ArrayList scms = new ArrayList();
//    
//    MavenPlugin plugin = MavenPlugin.getDefault();
//    MavenEmbedderManager embedderManager = plugin.getMavenEmbedderManager();
//    IndexManager indexManager = plugin.getMavenRepositoryIndexManager();
//    MavenConsole console = plugin.getConsole();
//        
//    MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();
//
//    for(int i = 0; i < dependencies.length; i++ ) {
//      try {
//        Dependency d = dependencies[i];
//        
//        Artifact artifact = embedder.createArtifact(d.getGroupId(), //
//            d.getArtifactId(), d.getVersion(), null, "pom");
//        
//        List remoteRepositories = indexManager.getArtifactRepositories(null, null);
//        
//        embedder.resolve(artifact, remoteRepositories, embedder.getLocalRepository());
//        
//        File file = artifact.getFile();
//        if(file != null) {
//          MavenProject project = embedder.readProject(file);
//          
//          Scm scm = project.getScm();
//          if(scm == null) {
//            String msg = project.getId() + " doesn't specify SCM info";
//            console.logError(msg);
//            continue;
//          }
//          
//          String connection = scm.getConnection();
//          String devConnection = scm.getDeveloperConnection();
//          String tag = scm.getTag();
//          String url = scm.getUrl();
//
//          console.logMessage(project.getArtifactId());
//          console.logMessage("Connection: " + connection);
//          console.logMessage("       dev: " + devConnection);
//          console.logMessage("       url: " + url);
//          console.logMessage("       tag: " + tag);
//          
//          if(connection==null) {
//            if(devConnection==null) {
//              String msg = project.getId() + " doesn't specify SCM connection";
//              console.logError(msg);
//              continue;
//            }
//            scm.setConnection(devConnection);
//          }
//
//          // connection: scm:svn:https://svn.apache.org/repos/asf/incubator/wicket/branches/wicket-1.2.x/wicket
//          //        dev: scm:svn:https://svn.apache.org/repos/asf/incubator/wicket/branches/wicket-1.2.x/wicket
//          //        url: http://svn.apache.org/viewvc/incubator/wicket/branches/wicket-1.2.x/wicket
//          //        tag: HEAD  
//
//          // TODO add an option to select all modules/projects and optimize scan 
//          
//          scms.add(scm);
//          
////          if(!connection.startsWith(SCM_SVN_PROTOCOL)) {
////            String msg = project.getId() + " SCM type is not supported " + connection;
////            console.logError(msg);
////            addError(new Exception(msg));
////          } else {
////            String svnUrl = connection.trim().substring(SCM_SVN_PROTOCOL.length());
////          }
//        }
//
//      } catch(Exception ex) {
//        console.logError(ex.getMessage());
//      }
//    }
//    
//    return (Scm[]) scms.toArray(new Scm[scms.size()]);
//  }

}
