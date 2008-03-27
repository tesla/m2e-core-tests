/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlOptions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.pom.x400.Build;
import org.apache.maven.pom.x400.ProjectDocument;
import org.apache.maven.pom.x400.Build.Plugins;
import org.apache.maven.pom.x400.Model.Dependencies;
import org.apache.maven.pom.x400.Model.Modules;

import org.maven.ide.eclipse.MavenConsole;
import org.maven.ide.eclipse.MavenPlugin;


/**
 * Model manager used to read and and modify Maven models
 * 
 * @author Eugene Kuleshov
 */
public class MavenModelManager {

  private final MavenEmbedderManager embedderManager;

  private final MavenConsole console;

  private XmlOptions xmlOptions;

  public MavenModelManager(MavenEmbedderManager embedderManager, MavenConsole console) {
    this.embedderManager = embedderManager;
    this.console = console;
  }

  public Model readMavenModel(Reader reader) throws CoreException {
    try {
      MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();
      return embedder.readModel(reader);
    } catch(XmlPullParserException ex) {
      String msg = "Model parsing error; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
    } catch(IOException ex) {
      String msg = "Can't read model; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
    }
  }

  public Model readMavenModel(File pomFile) throws CoreException {
    try {
      MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();
      return embedder.readModel(pomFile);
    } catch(XmlPullParserException ex) {
      String msg = "Parsing error " + pomFile.getAbsolutePath() + "; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
    } catch(IOException ex) {
      String msg = "Can't read model " + pomFile.getAbsolutePath() + "; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
    }
  }

  public Model readMavenModel(IFile pomFile) throws CoreException {
    String name = pomFile.getProject().getName() + "/" + pomFile.getProjectRelativePath();
    try {
      MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();
      return embedder.readModel(pomFile.getLocation().toFile());
    } catch(XmlPullParserException ex) {
      String msg = "Parsing error " + name + "; " + ex.getMessage();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
    } catch(IOException ex) {
      String msg = "Can't read model " + name + "; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
    }
  }

  public void createMavenModel(IFile pomFile, Model model) throws CoreException {
    String pomFileName = pomFile.getLocation().toString();
    if(pomFile.exists()) {
      String msg = "POM " + pomFileName + " already exists";
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, null));
    }

    try {
      StringWriter w = new StringWriter();

      MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();
      embedder.writeModel(w, model, true);

      String pom = w.toString();
      
      ProjectDocument projectDocument = ProjectDocument.Factory.parse(pom, getXmlOptions());

      new NamespaceAdder().update(projectDocument);
      
      XmlOptions options = new XmlOptions();
      options.setSaveNamespacesFirst();
      
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      projectDocument.save(bos, options);
      
      pomFile.create(new ByteArrayInputStream(bos.toByteArray()), true, null);

    } catch(Exception ex) {
      String msg = "Can't create model " + pomFileName + "; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
    }
  }

//  public MavenExecutionResult readMavenProject(File pomFile, IProgressMonitor monitor, boolean offline, boolean debug,
//      ResolverConfiguration resolverConfiguration) {
//    MavenEmbedder embedder = embedderManager.createEmbedder(EmbedderFactory.createWorkspaceCustomizer());
//    return readMavenProject(pomFile, monitor, offline, debug, resolverConfiguration, embedder);
//  }
//
//  public MavenExecutionResult readMavenProject(File pomFile, IProgressMonitor monitor, //
//      boolean offline, boolean debug, ResolverConfiguration resolverConfiguration, MavenEmbedder embedder) {
//    try {
//      // monitor.subTask("Reading " + pomFile.getFullPath());
//      // File file = pomFile.getLocation().toFile();
//
//      MavenExecutionRequest request = EmbedderFactory.createMavenExecutionRequest(embedder, offline, debug);
//      request.setPomFile(pomFile.getAbsolutePath());
//      request.setBaseDirectory(pomFile.getParentFile());
//      request.setTransferListener(new TransferListenerAdapter(monitor, console, indexManager));
//      request.setProfiles(resolverConfiguration.getActiveProfileList());
//      request.addActiveProfiles(resolverConfiguration.getActiveProfileList());
//
//      return embedder.readProjectWithDependencies(request);
//
//      // XXX need to manage markers somehow see MNGECLIPSE-***
//      // Util.deleteMarkers(pomFile);
//
////      if(!result.hasExceptions()) {
////        return result.getMavenProject();
////      }
////      
////      return result.getMavenProject();
//
////    } catch(Exception ex) {
////      Util.deleteMarkers(this.file);
////      Util.addMarker(this.file, "Unable to read project; " + ex.toString(), 1, IMarker.SEVERITY_ERROR);
////      
////      String msg = "Unable to read " + file.getLocation() + "; " + ex.toString();
////      console.logError(msg);
////      MavenPlugin.log(msg, ex);
//
//    } finally {
//      monitor.done();
//    }
//  }

  public void updateProject(IFile pomFile, ProjectUpdater updater) {
    File pom = pomFile.getLocation().toFile();
    try {
      XmlOptions options = getXmlOptions();

      ProjectDocument projectDocument = ProjectDocument.Factory.parse(pom, options);
      updater.update(projectDocument);

//      XmlCursor cursor = projectDocument.newCursor();
//      if (cursor.toFirstChild()) {
//        cursor.setAttributeText(new QName("http://www.w3.org/2001/XMLSchema-instance","schemaLocation"), location);
//      }      

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      projectDocument.save(bos, options);

      pomFile.setContents(new ByteArrayInputStream(bos.toByteArray()), true, true, null);
      pomFile.refreshLocal(IResource.DEPTH_ONE, null); // TODO ???

    } catch(Exception ex) {
      String msg = "Unable to update " + pom;
      console.logError(msg + "; " + ex.getMessage());
      MavenPlugin.log(msg, ex);
    }
  }

  private XmlOptions getXmlOptions() {
    if(xmlOptions==null) {
      xmlOptions = new XmlOptions();
  
      Map ns = Collections.singletonMap("", ProjectDocument.type.getDocumentElementName().getNamespaceURI());
      xmlOptions.setLoadSubstituteNamespaces(ns);
      xmlOptions.setSaveImplicitNamespaces(ns);
    }
    return xmlOptions;
  }

  public void addDependency(IFile pomFile, final Dependency dependency) {
    updateProject(pomFile, new DependencyAdder(dependency));
  }

  public void addModule(IFile pomFile, final String moduleName) {
    updateProject(pomFile, new ModuleAdder(moduleName));
  }

  
  /**
   * Project updater for adding Maven namespace declaration
   */
  public static class NamespaceAdder implements ProjectUpdater {

    public void update(ProjectDocument project) {
      XmlCursor cursor = project.newCursor();
      cursor.toNextToken();
      if(!cursor.toFirstAttribute()) {
        cursor.toNextToken();
      }

      String uri = ProjectDocument.type.getDocumentElementName().getNamespaceURI();
      cursor.insertNamespace("", uri);
      cursor.insertNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
      cursor.insertAttributeWithValue( //
          new QName("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation",  "xsi"), 
          uri + " http://maven.apache.org/maven-v4_0_0.xsd");
    }
    
  }
  
  /**
   * Project updater for adding dependencies
   */
  public static class DependencyAdder implements ProjectUpdater {
  
    private final Dependency dependency;

    public DependencyAdder(Dependency dependency) {
      this.dependency = dependency;
    }

    public void update(ProjectDocument project) {
      Dependencies dependencies = project.getProject().getDependencies();
  
      if(dependencies == null) {
        dependencies = project.getProject().addNewDependencies();
  
        XmlCursor cursor = dependencies.newCursor();
        cursor.insertChars("  ");
        cursor.toEndToken();
        cursor.insertChars("\n  ");
        cursor.toNextToken();
        cursor.insertChars("\n");
        cursor.dispose();
      }
  
      String uri = ProjectDocument.type.getDocumentElementName().getNamespaceURI();
      
      XmlCursor cursor = dependencies.newCursor();
      cursor.toEndToken();
  
      cursor.insertChars("  ");
      cursor.beginElement("dependency", uri);
      cursor.insertChars("\n");
  
      cursor.insertChars("      ");
      cursor.insertElementWithText("groupId", uri, this.dependency.getGroupId());
      cursor.insertChars("\n");
  
      cursor.insertChars("      ");
      cursor.insertElementWithText("artifactId", uri, this.dependency.getArtifactId());
      cursor.insertChars("\n");
  
      if(this.dependency.getClassifier() != null) {
        cursor.insertChars("      ");
        cursor.insertElementWithText("classifier", uri, this.dependency.getClassifier());
        cursor.insertChars("\n    ");
      }
  
      if(this.dependency.getVersion() != null) {
        cursor.insertChars("      ");
        cursor.insertElementWithText("version", uri, this.dependency.getVersion());
        cursor.insertChars("\n    ");
      }
  
      if(this.dependency.getType() != null && !"jar".equals(dependency.getType())) {
        cursor.insertChars("      ");
        cursor.insertElementWithText("type", uri, this.dependency.getType());
        cursor.insertChars("\n    ");
      }
  
      if(this.dependency.getScope() != null) {
        cursor.insertChars("      ");
        cursor.insertElementWithText("scope", uri, this.dependency.getScope());
        cursor.insertChars("\n    ");
      }
  
      if(this.dependency.getSystemPath() != null) {
        cursor.insertChars("      ");
        cursor.insertElementWithText("systemPath", uri, this.dependency.getSystemPath());
        cursor.insertChars("\n    ");
      }
  
      if(this.dependency.isOptional()) {
        cursor.insertChars("      ");
        cursor.insertElementWithText("optional", uri, "true");
        cursor.insertChars("\n    ");
      }
  
      if(this.dependency.getExclusions() != null) {
        // TODO support exclusions
  
      }

      cursor.toNextToken();
      cursor.insertChars("\n  ");
      
      cursor.dispose();
    }
  }

  /**
   * Project updater for adding modules
   */
  public static class ModuleAdder implements ProjectUpdater {
  
    private final String moduleName;

    public ModuleAdder(String moduleName) {
      this.moduleName = moduleName;
    }

    public void update(ProjectDocument project) {
      Modules modules = project.getProject().getModules();
  
      if(modules == null) {
        modules = project.getProject().addNewModules();
  
        XmlCursor cursor = modules.newCursor();
        cursor.insertChars("  ");
        cursor.toEndToken();
        cursor.insertChars("\n  ");
        cursor.toNextToken();
        cursor.insertChars("\n");
        cursor.dispose();
      }
  
      String uri = ProjectDocument.type.getDocumentElementName().getNamespaceURI();
      
      XmlCursor cursor = modules.newCursor();
      cursor.toEndToken();
      cursor.insertChars("  ");
      cursor.insertElementWithText("module", uri, this.moduleName);
      cursor.insertChars("\n  ");
      cursor.dispose();
    }
  }

  /**
   * Project updater for adding plugins
   */
  public static class PluginAdder implements ProjectUpdater {
  
    private final String groupId;
    private final String artifactId;
    private final String version;

    public PluginAdder(String groupId, String artifactId, String version) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
    }

    public void update(ProjectDocument project) {
      Build build = project.getProject().getBuild();
  
      if(build == null) {
        build = project.getProject().addNewBuild();
  
        XmlCursor cursor = build.newCursor();
        cursor.insertChars("  ");
        cursor.toEndToken();
        cursor.insertChars("\n  ");
        cursor.toNextToken();
        cursor.insertChars("\n");
        cursor.dispose();
      }
  
      Plugins plugins = build.getPlugins();
      if(plugins == null) {
        plugins = build.addNewPlugins();
        
        XmlCursor cursor = plugins.newCursor();
        cursor.insertChars("  ");
        cursor.toEndToken();
        cursor.insertChars("\n    ");
        cursor.toNextToken();
        cursor.insertChars("\n  ");
        cursor.dispose();
      }

      String uri = ProjectDocument.type.getDocumentElementName().getNamespaceURI();
      
      XmlCursor cursor = plugins.newCursor();
      cursor.toEndToken();
  
      cursor.insertChars("  ");
      cursor.beginElement("plugin", uri);
  
      if(!"org.apache.maven.plugins".equals(this.groupId)) {
        cursor.insertChars("\n        ");
        cursor.insertElementWithText("groupId", uri, this.groupId);
      }
  
      cursor.insertChars("\n        ");
      cursor.insertElementWithText("artifactId", uri, this.artifactId);
      
      if(this.version!=null) {
        cursor.insertChars("\n        ");
        cursor.insertElementWithText("version", uri, this.version);
      }
      
      cursor.insertChars("\n      ");
      cursor.toNextToken();
      cursor.insertChars("\n    ");
      cursor.dispose();
    }
  }

}
