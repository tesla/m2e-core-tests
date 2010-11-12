/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt.internal.actions;

import java.util.ArrayList;
import java.util.Collections;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;

import org.eclipse.m2e.core.MavenImages;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ArtifactRef;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.ui.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.jdt.internal.Messages;


public class MavenDependencyResolver implements IQuickAssistProcessor {

  public boolean hasAssists(IInvocationContext context) {
    return true;
  }

  public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations) {
    List<IJavaCompletionProposal> proposals = new ArrayList<IJavaCompletionProposal>();
    for(int i = 0; i < locations.length; i++ ) {
      IProblemLocation location = locations[i];
      String[] arguments = location.getProblemArguments();
      String name;
      if(arguments != null && arguments.length > 0) {
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
    return proposals.toArray(new IJavaCompletionProposal[proposals.size()]);

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
      super(NLS.bind(Messages.MavenDependencyResolver_proposal_search, query), null, relevance, MavenImages.IMG_MAVEN_JAR);
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
        MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.MavenDependencyResolver_error_title, //
            Messages.MavenDependencyResolver_error_message);
        return;
      }

      IFile projectPom = javaProject.getProject().getFile(new Path(IMavenConstants.POM_FILE_NAME));
      if(projectPom == null || !projectPom.isAccessible()) {
        MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.MavenDependencyResolver_error_title,
            Messages.MavenDependencyResolver_error_message2);
        return;
      }

      boolean hasMavenNature = false;
      try {
        hasMavenNature = javaProject.getProject().hasNature(IMavenConstants.NATURE_ID);
      } catch(CoreException ex1) {
        MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.MavenDependencyResolver_error_title, //
            Messages.MavenDependencyResolver_error_message3);
        return;
      }

      MavenPlugin plugin = MavenPlugin.getDefault();
      MavenProjectManager projectManager = plugin.getMavenProjectManager();
      MavenModelManager modelManager = plugin.getMavenModelManager();

      IFile pomFile = projectPom;
      Set<ArtifactKey> artifacts = Collections.emptySet();

      if(hasMavenNature) {
        IMavenProjectFacade projectFacade = projectManager.create(projectPom, false, new NullProgressMonitor());
        if(projectFacade == null) {
          MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.MavenDependencyResolver_error_title, //
              Messages.MavenDependencyResolver_error_message4);
          return;
        }

        try {
          pomFile = projectFacade.getPom();
          artifacts = ArtifactRef.toArtifactKey(projectFacade.getMavenProjectArtifacts());
        } catch(Exception ex) {
          String msg = "Unable to locate Maven project";
          MavenLogger.log(msg, ex);
          MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.MavenDependencyResolver_error_title,
              NLS.bind(Messages.MavenDependencyResolver_error_message5, cu.getPath().toString()));
          return;
        }
      } else {
        try {
          pomFile = findModulePom(resource, pomFile, modelManager);
          IMavenProjectFacade facade = projectManager.create(pomFile, true, new NullProgressMonitor());
          if(facade != null) {
            artifacts = ArtifactRef.toArtifactKey(facade.getMavenProjectArtifacts());
          }
        } catch(CoreException ex) {
          String msg = "Unable to locate Maven project";
          MavenLogger.log(msg, ex);
          MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.MavenDependencyResolver_error_title,
              NLS.bind(Messages.MavenDependencyResolver_error_message6, cu.getPath().toString()));
          return;
        }
      }

      IWorkbench workbench = plugin.getWorkbench();
      Shell shell = workbench.getDisplay().getActiveShell();
      
      MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(shell, //
          Messages.MavenDependencyResolver_searchDialog_title, IIndex.SEARCH_CLASS_NAME, artifacts, true);
      dialog.setQuery(query);

      if(dialog.open() == Window.OK) {
        IndexedArtifactFile iaf = dialog.getSelectedIndexedArtifactFile();

        Dependency dependency = iaf.getDependency();
        dependency.setScope(dialog.getSelectedScope());
        
        modelManager.addDependency(pomFile, dependency);

        // add import for selected class
        if(addImport) {
          IndexedArtifact ia = dialog.getSelectedIndexedArtifact();
          String packageName = ia.getPackageName();
          String className = ia.getClassname();

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
        @SuppressWarnings("unchecked")
        List<ImportDeclaration> imports = ast.imports();
        if(!imports.isEmpty()) {
          ImportDeclaration importDeclaration = imports.get(imports.size() - 1);
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
            document.getLineDelimiter(0) + "import " + packageName + "." + className + ";"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        edit.apply(document, TextEdit.CREATE_UNDO);
        return true;
      } catch(Exception ex) {
        MavenLogger.log("Unable to update imports", ex);
        MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.MavenDependencyResolver_error_title,
            NLS.bind(Messages.MavenDependencyResolver_error_message7, className));
        return false;
      }
    }

    private IFile findModulePom(IResource resource, IFile pomFile, MavenModelManager modelManager) throws CoreException {
      Model model = modelManager.readMavenModel(pomFile);

      for(String module : model.getModules()) {
        IFile modulePom = pomFile.getParent().getFile(new Path(module).append(IMavenConstants.POM_FILE_NAME));
        if(modulePom.exists() && modulePom.isAccessible()) {
          IPath modulePath = modulePom.getLocation();
          if(modulePath.matchingFirstSegments(resource.getLocation()) == modulePath.segmentCount() - 1) {
            IFile nestedModulePom = findModulePom(resource, modulePom, modelManager);
            return nestedModulePom == null ? modulePom : nestedModulePom;
          }
        }
      }

      for(Profile profile : model.getProfiles()) {
        for(String module : profile.getModules()) {
          IFile modulePom = pomFile.getParent().getFile(new Path(module).append(IMavenConstants.POM_FILE_NAME));
          IPath modulePath = modulePom.getLocation();
          if(modulePath.matchingFirstSegments(resource.getLocation()) == modulePath.segmentCount()) {
            IFile nestedModulePom = findModulePom(resource, modulePom, modelManager);
            return nestedModulePom == null ? modulePom : nestedModulePom;
          }
        }
      }

      return null;
    }

    public String getAdditionalProposalInfo() {
      return Messages.MavenDependencyResolver_additional_info;
    }

  }

}
