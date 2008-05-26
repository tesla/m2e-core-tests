/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;


public class MavenDependencyResolver implements IQuickAssistProcessor {

  public boolean hasAssists(IInvocationContext context) {
    return true;
  }

  public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations) {
    List proposals = new ArrayList();
    for(int i = 0; i < locations.length; i++ ) {
      IProblemLocation location = locations[i];
      String[] arguments = location.getProblemArguments();
      String name;
      if(arguments != null) {
        name = arguments[0];
      } else {
        ASTNode coveringNode = context.getCoveringNode();
        if(coveringNode==null || coveringNode.getNodeType()!=ASTNode.SIMPLE_NAME) {
          continue;
        }
        name = coveringNode.toString();
      }
      int id = location.getProblemId();
      switch(id) {
        case IProblem.UndefinedType:
        case IProblem.UndefinedName:
          proposals.add(new OpenBuildPathCorrectionProposal(name, context, 0, true));
          break;

        case IProblem.IsClassPathCorrect:
        case IProblem.ImportNotFound:
          proposals.add(new OpenBuildPathCorrectionProposal(name, context, 0, false));
          break;
      }
    }
    return (IJavaCompletionProposal[]) proposals.toArray(new IJavaCompletionProposal[proposals.size()]);

    /*
    if (coveringNode == null) {
      return null; 
    }
    if(coveringNode.getNodeType()==ASTNode.SIMPLE_NAME) {
      ASTNode parent = coveringNode.getParent();
      if(parent!=null) {
        switch(parent.getNodeType()) {
          case ASTNode.ASSIGNMENT:
          case ASTNode.SIMPLE_TYPE:
          case ASTNode.QUALIFIED_NAME:
          case ASTNode.MARKER_ANNOTATION:
            return new IJavaCompletionProposal[] { 
                new OpenBuildPathCorrectionProposal(coveringNode.toString(), context, 1, null) 
              };
        }
      }
    }
    
    return null;
    */
  }

  static public final class OpenBuildPathCorrectionProposal extends ChangeCorrectionProposal {
    private final String query;

    private final IInvocationContext context;

    private final boolean addImport;

    OpenBuildPathCorrectionProposal(String query, IInvocationContext context, int relevance, boolean addImport) {
      super("Search dependency for " + query, null, relevance, MavenPlugin.getImage("icons/mjar.gif"));
      this.query = query;
      this.context = context;
      this.addImport = addImport;
    }

    public void apply(IDocument document) {
      ICompilationUnit cu = context.getCompilationUnit();
      IJavaProject javaProject = cu.getJavaProject();

      IResource resource;
      try {
        resource = cu.getCorrespondingResource();
      } catch(CoreException ex) {
        MessageDialog.openError(Display.getCurrent().getActiveShell(), "Add Dependency", //
            "Unable to retrieve corresponding resource");
        return;
      }

      IFile projectPom = javaProject.getProject().getFile(new Path(MavenPlugin.POM_FILE_NAME));
      if(projectPom == null || !projectPom.isAccessible()) {
        MessageDialog.openError(Display.getCurrent().getActiveShell(), "Add Dependency",
            "Project does not have pom.xml");
        return;
      }

      boolean hasMavenNature = false;
      try {
        hasMavenNature = javaProject.getProject().hasNature(MavenPlugin.NATURE_ID);
      } catch(CoreException ex1) {
        MessageDialog.openError(Display.getCurrent().getActiveShell(), "Add Dependency", //
            "Unable to read project natures");
        return;
      }

      MavenPlugin plugin = MavenPlugin.getDefault();
      MavenProjectManager projectManager = plugin.getMavenProjectManager();
      MavenModelManager modelManager = plugin.getMavenModelManager();

      IFile pomFile = projectPom;
      Set artifacts = Collections.EMPTY_SET;

      if(hasMavenNature) {
        MavenProjectFacade projectFacade = projectManager.create(projectPom, false, new NullProgressMonitor());
        if(projectFacade == null) {
          MessageDialog.openError(Display.getCurrent().getActiveShell(), "Add Dependency", //
              "Unable to read Maven project");
          return;
        }

        try {
          MavenProjectFacade facade = getModuleFacade(resource, projectFacade);
          if(facade != null) {
            pomFile = facade.getPom();
            artifacts = facade.getMavenProject().getArtifacts();

          } else {
            MessageDialog.openError(Display.getCurrent().getActiveShell(), "Add Dependency",
                "Unable to locate pom.xml for " + cu.getPath().toString());
            return;
          }
        } catch(Exception ex) {
          String msg = "Unable to locate Maven project";
          MavenPlugin.log(msg, ex);
          MessageDialog.openError(Display.getCurrent().getActiveShell(), "Add Dependency",
              "Unable to locate pom.xml for " + cu.getPath().toString());
          return;
        }
      } else {
        try {
          pomFile = findModulePom(resource, pomFile, modelManager);
          MavenProjectFacade facade = projectManager.create(pomFile, true, new NullProgressMonitor());
          if(facade != null) {
            artifacts = facade.getMavenProject().getArtifacts();
          }
        } catch(CoreException ex) {
          String msg = "Unable to locate Maven project";
          MavenPlugin.log(msg, ex);
          MessageDialog.openError(Display.getCurrent().getActiveShell(), "Add Dependency",
              "Unable to locate pom.xml for " + cu.getPath().toString());
          return;
        }
      }

      IWorkbench workbench = plugin.getWorkbench();
      Shell shell = workbench.getDisplay().getActiveShell();

      MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(shell, //
          "Search in Maven repositories", IndexManager.SEARCH_CLASS_NAME, artifacts);
      dialog.setQuery(query);

      if(dialog.open() == Window.OK) {
        IndexedArtifactFile iaf = dialog.getSelectedIndexedArtifactFile();

        modelManager.addDependency(pomFile, iaf.getDependency());

        // add import for selected class
        if(addImport) {
          IndexedArtifact ia = dialog.getSelectedIndexedArtifact();
          String packageName = ia.packageName;
          String className = ia.className;

          if(addImportDeclaration(document, context, packageName, className)) {
            IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
            IWorkbenchPage page = window.getActivePage();
            IEditorPart activeEditor = page.getActiveEditor();
            activeEditor.doSave(null);
          }
        }

//        if(organizeImports) {
//          IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
//          IWorkbenchPage page = window.getActivePage();
//
//          try {
//            // plugin.update(pomFile, null);
//
//            // organize imports
//            IEditorPart activeEditor = page.getActiveEditor();
//            OrganizeImportsAction organizeImportsAction = new OrganizeImportsAction(activeEditor.getEditorSite());
//            organizeImportsAction.run(cu);
//            activeEditor.doSave(null);
//
//          } catch(Exception e) {
//            MavenPlugin.getDefault().getConsole().logError("Build error; " + e.getMessage());
//            return;
//
//          }
//        }
      }
    }

    private boolean addImportDeclaration(IDocument document, IInvocationContext context, String packageName, String className) {
      CompilationUnit ast = context.getASTRoot();
      try {
        int startPosition;
        List imports = ast.imports();
        if(!imports.isEmpty()) {
          ImportDeclaration importDeclaration = (ImportDeclaration) imports.get(imports.size() - 1);
          startPosition = importDeclaration.getStartPosition() + importDeclaration.getLength();
        } else {
          PackageDeclaration packageDeclaration = ast.getPackage();
          if(packageDeclaration != null) {
            startPosition = packageDeclaration.getStartPosition() + packageDeclaration.getLength();
          } else {
            startPosition = 0;
          }
        }

        InsertEdit edit = new InsertEdit(startPosition, //
            document.getLineDelimiter(0) + "import " + packageName + "." + className + ";");
        edit.apply(document, TextEdit.CREATE_UNDO);
        return true;
      } catch(Exception ex) {
        MavenPlugin.log("Unable to update imports", ex);
        MessageDialog.openError(Display.getCurrent().getActiveShell(), "Add Dependency",
            "Unable to add import statement for " + className);
        return false;
      }
    }

    private IFile findModulePom(IResource resource, IFile pomFile, MavenModelManager modelManager) throws CoreException {
      Model model = modelManager.readMavenModel(pomFile);

      for(Iterator moduleIterator = model.getModules().iterator(); moduleIterator.hasNext();) {
        String module = (String) moduleIterator.next();
        IFile modulePom = pomFile.getParent().getFile(new Path(module).append(MavenPlugin.POM_FILE_NAME));
        if(modulePom.exists() && modulePom.isAccessible()) {
          IPath modulePath = modulePom.getLocation();
          if(modulePath.matchingFirstSegments(resource.getLocation()) == modulePath.segmentCount() - 1) {
            IFile nestedModulePom = findModulePom(resource, modulePom, modelManager);
            return nestedModulePom == null ? modulePom : nestedModulePom;
          }
        }
      }

      for(Iterator it = model.getProfiles().iterator(); it.hasNext();) {
        Profile profile = (Profile) it.next();
        for(Iterator moduleIterator = profile.getModules().iterator(); moduleIterator.hasNext();) {
          String module = (String) moduleIterator.next();
          IFile modulePom = pomFile.getParent().getFile(new Path(module).append(MavenPlugin.POM_FILE_NAME));
          IPath modulePath = modulePom.getLocation();
          if(modulePath.matchingFirstSegments(resource.getLocation()) == modulePath.segmentCount()) {
            IFile nestedModulePom = findModulePom(resource, modulePom, modelManager);
            return nestedModulePom == null ? modulePom : nestedModulePom;
          }
        }
      }

      return null;
    }

    private MavenProjectFacade getModuleFacade(IResource resource, MavenProjectFacade projectFacade) throws CoreException {
      final IPath resourcePath = resource.getLocation();

      class ModulePomLocator implements IMavenProjectVisitor {
        MavenProjectFacade facade;

        public boolean visit(MavenProjectFacade projectFacade) {
          if(facade == null) {
            facade = projectFacade;
          }

          IPath location = projectFacade.getPom().getParent().getLocation();
          if(location.matchingFirstSegments(resourcePath) == location.segmentCount()
              && location.segmentCount() > facade.getPom().getParent().getLocation().segmentCount()) {
            facade = projectFacade;
          }
          return true;
        }

        public void visit(MavenProjectFacade mavenProject, Artifact artifact) {
        }
      }

      ModulePomLocator locator = new ModulePomLocator();
      projectFacade.accept(locator, IMavenProjectVisitor.NESTED_MODULES);
      return locator.facade;
    }

    public String getAdditionalProposalInfo() {
      return "Resolve dependencies from Maven repositories";
    }

  }

}
