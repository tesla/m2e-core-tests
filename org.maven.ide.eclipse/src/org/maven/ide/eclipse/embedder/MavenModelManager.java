/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import org.xml.sax.InputSource;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jem.util.emf.workbench.EMFWorkbenchContextBase;
import org.eclipse.wst.common.internal.emf.resource.EMF2DOMRenderer;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.filter.ArtifactDependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.traversal.BuildingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.FilteringDependencyNodeVisitor;

import org.maven.ide.components.pom.Build;
import org.maven.ide.components.pom.Configuration;
import org.maven.ide.components.pom.Dependencies;
import org.maven.ide.components.pom.Dependency;
import org.maven.ide.components.pom.Exclusion;
import org.maven.ide.components.pom.ExclusionsType;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.Modules;
import org.maven.ide.components.pom.Plugin;
import org.maven.ide.components.pom.Plugins;
import org.maven.ide.components.pom.PomFactory;
import org.maven.ide.components.pom.util.PomResourceFactoryImpl;
import org.maven.ide.components.pom.util.PomResourceImpl;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;


/**
 * Model manager used to read and and modify Maven models
 * 
 * @author Eugene Kuleshov
 * 
 * XXX fix circular dependency
 */
@SuppressWarnings("restriction")
public class MavenModelManager {

  static final PomFactory POM_FACTORY = PomFactory.eINSTANCE;
  
  private final MavenEmbedderManager embedderManager;

  private final MavenProjectManager projectManager;
  
  private final MavenConsole console;

  public MavenModelManager(MavenEmbedderManager embedderManager, MavenProjectManager projectManager, MavenConsole console) {
    this.embedderManager = embedderManager;
    this.projectManager = projectManager;
    this.console = console;
  }

  public PomResourceImpl loadResource(IFile pomFile) throws CoreException {
    String path = pomFile.getFullPath().toOSString();
    URI uri = URI.createPlatformResourceURI(path, true);

    try {
      PomResourceFactoryImpl factory = new PomResourceFactoryImpl();
      PomResourceImpl resource = (PomResourceImpl) factory.createResource(uri);
      
      EMFWorkbenchContextBase contextBase = new EMFWorkbenchContextBase(pomFile.getProject());
      contextBase.getResourceSet().getResources().add(resource);
      
      resource.load(Collections.EMPTY_MAP);
      return resource;

    } catch(IOException ex) {
      String msg = "Can't load model " + pomFile;
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
    }
  }

  public Model loadModel(String pomFile) throws CoreException {
    URI uri = URI.createFileURI(pomFile);

    PomResourceFactoryImpl factory = new PomResourceFactoryImpl();
    PomResourceImpl resource = (PomResourceImpl) factory.createResource(uri);

    // disable SSE support for read-only external documents
    resource.setRenderer(new EMF2DOMRenderer());

    try {
      resource.load(Collections.EMPTY_MAP);
      return (Model) resource.getContents().get(0);
    } catch(IOException ex) {
      String msg = "Can't load model " + pomFile;
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
    }
  }

  public org.apache.maven.model.Model readMavenModel(Reader reader) throws CoreException {
    try {
      MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();
      return embedder.readModel(reader);
    } catch(XmlPullParserException ex) {
      String msg = "Model parsing error; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
    } catch(IOException ex) {
      String msg = "Can't read model; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
    }
  }

  public org.apache.maven.model.Model readMavenModel(File pomFile) throws CoreException {
    try {
      MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();
      return embedder.readModel(pomFile);
    } catch(XmlPullParserException ex) {
      String msg = "Parsing error " + pomFile.getAbsolutePath() + "; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
    } catch(IOException ex) {
      String msg = "Can't read model " + pomFile.getAbsolutePath() + "; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
    }
  }

  public org.apache.maven.model.Model readMavenModel(IFile pomFile) throws CoreException {
    String name = pomFile.getProject().getName() + "/" + pomFile.getProjectRelativePath();
    try {
      MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();
      return embedder.readModel(pomFile.getLocation().toFile());
    } catch(XmlPullParserException ex) {
      String msg = "Parsing error " + name + "; " + ex.getMessage();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
    } catch(IOException ex) {
      String msg = "Can't read model " + name + "; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
    }
  }

  public void createMavenModel(IFile pomFile, org.apache.maven.model.Model model) throws CoreException {
    String pomFileName = pomFile.getLocation().toString();
    if(pomFile.exists()) {
      String msg = "POM " + pomFileName + " already exists";
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, null));
    }

    try {
      StringWriter sw = new StringWriter();

      MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();
      embedder.writeModel(sw, model, true);

      String pom = sw.toString();

      // XXX MNGECLIPSE-495
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(false);
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      
      Document document = documentBuilder.parse(new InputSource(new StringReader(pom)));
      Element documentElement = document.getDocumentElement();

      NamedNodeMap attributes = document.getAttributes();

      if(attributes == null || attributes.getNamedItem("xmlns") == null) {
        Attr attr = document.createAttribute("xmlns");
        attr.setTextContent("http://maven.apache.org/POM/4.0.0");
        documentElement.setAttributeNode(attr);
      }

      if(attributes == null || attributes.getNamedItem("xmlns:xsi") == null) {
        Attr attr = document.createAttribute("xmlns:xsi");
        attr.setTextContent("http://www.w3.org/2001/XMLSchema-instance");
        documentElement.setAttributeNode(attr);
      }

      if(attributes == null || attributes.getNamedItem("xsi:schemaLocation") == null) {
        Attr attr = document.createAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation");
        attr.setTextContent("http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd");
        documentElement.setAttributeNode(attr);
      }
      
      TransformerFactory transfac = TransformerFactory.newInstance();
      Transformer trans = transfac.newTransformer();
      trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      
      sw = new StringWriter();
      trans.transform(new DOMSource(document), new StreamResult(sw));
      pom = sw.toString();
      
      pomFile.create(new ByteArrayInputStream(pom.getBytes("UTF-8")), true, new NullProgressMonitor());

    } catch(RuntimeException ex) {
      String msg = "Can't create model " + pomFileName + "; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
    } catch(Exception ex) {
      String msg = "Can't create model " + pomFileName + "; " + ex.toString();
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
    }
  }

  /**
   * @param force
   * @param monitor
   * @param scope one of 
   *   {@link Artifact#SCOPE_COMPILE}, 
   *   {@link Artifact#SCOPE_TEST}, 
   *   {@link Artifact#SCOPE_SYSTEM}, 
   *   {@link Artifact#SCOPE_PROVIDED}, 
   *   {@link Artifact#SCOPE_RUNTIME}
   *   
   * @return dependency node
   */
  public synchronized DependencyNode readDependencies(IFile file, String scope, IProgressMonitor monitor) throws CoreException {
    try {
      monitor.setTaskName("Reading project");
      MavenProject mavenProject = readMavenProject(file, monitor);

      MavenEmbedder embedder = projectManager.createWorkspaceEmbedder();
      try {
        monitor.setTaskName("Building dependency tree");
        PlexusContainer plexus = embedder.getPlexusContainer();

        ArtifactFactory artifactFactory = (ArtifactFactory) plexus.lookup(ArtifactFactory.class);
        ArtifactMetadataSource artifactMetadataSource = //
        (ArtifactMetadataSource) plexus.lookup(ArtifactMetadataSource.class);

        ArtifactCollector artifactCollector = (ArtifactCollector) plexus.lookup(ArtifactCollector.class);

        ArtifactRepository localRepository = embedder.getLocalRepository();

        DependencyTreeBuilder builder = (DependencyTreeBuilder) plexus.lookup(DependencyTreeBuilder.ROLE);
        DependencyNode node = builder.buildDependencyTree(mavenProject, localRepository, artifactFactory,
            artifactMetadataSource, null, artifactCollector);
        
        BuildingDependencyNodeVisitor visitor = new BuildingDependencyNodeVisitor(); 
        node.accept(new FilteringDependencyNodeVisitor(visitor,
            new ArtifactDependencyNodeFilter(new ScopeArtifactFilter(scope))));
        return visitor.getDependencyTree();
      } finally {
        embedder.stop();
      }

    } catch(MavenEmbedderException ex) {
      String msg = "Can't create Maven embedder";
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));

    } catch(ComponentLookupException ex) {
      String msg = "Component lookup error";
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));

    } catch(DependencyTreeBuilderException ex) {
      String msg = "Project read error";
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
    }
  }

  public MavenProject readMavenProject(IFile file, IProgressMonitor monitor) throws CoreException {
    MavenPlugin plugin = MavenPlugin.getDefault();
    MavenProjectManager projectManager = plugin.getMavenProjectManager();
    IMavenProjectFacade projectFacade = projectManager.create(file, true, monitor);
    MavenProject mavenProject = projectFacade.getMavenProject(monitor);
    return mavenProject;
  }

//  public ProjectDocument readProjectDocument(IFile pomFile) throws CoreException {
//    String name = pomFile.getProject().getName() + "/" + pomFile.getProjectRelativePath();
//    try {
//      return ProjectDocument.Factory.parse(pomFile.getLocation().toFile(), getXmlOptions());
//    } catch(XmlException ex) {
//      String msg = "Unable to parse " + name;
//      console.logError(msg + "; " + ex.toString());
//      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
//    } catch(IOException ex) {
//      String msg = "Unable to read " + name;
//      console.logError(msg + "; " + ex.toString());
//      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
//    }
//  }
//
//  public ProjectDocument readProjectDocument(File pom) throws CoreException {
//    try {
//      return ProjectDocument.Factory.parse(pom, getXmlOptions());
//    } catch(XmlException ex) {
//      String msg = "Unable to parse " + pom.getAbsolutePath();
//      console.logError(msg + "; " + ex.toString());
//      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
//    } catch(IOException ex) {
//      String msg = "Unable to read " + pom.getAbsolutePath();
//      console.logError(msg + "; " + ex.toString());
//      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
//    }
//  }

  public void updateProject(IFile pomFile, ProjectUpdater updater) {
    File pom = pomFile.getLocation().toFile();
    try {
      PomResourceImpl resource = loadResource(pomFile);
      updater.update(resource.getModel());

//      XmlCursor cursor = projectDocument.newCursor();
//      if (cursor.toFirstChild()) {
//        cursor.setAttributeText(new QName("http://www.w3.org/2001/XMLSchema-instance","schemaLocation"), location);
//      }      

      resource.save(Collections.EMPTY_MAP);

      // pomFile.setContents(new ByteArrayInputStream(bos.toByteArray()), true, true, null);
      // pomFile.refreshLocal(IResource.DEPTH_ONE, null); // TODO ???

    } catch(Exception ex) {
      String msg = "Unable to update " + pom;
      console.logError(msg + "; " + ex.getMessage());
      MavenLogger.log(msg, ex);
    }
  }

  public void addDependency(IFile pomFile, org.apache.maven.model.Dependency dependency) {
    updateProject(pomFile, new DependencyAdder(dependency));
  }

  public void addModule(IFile pomFile, final String moduleName) {
    updateProject(pomFile, new ModuleAdder(moduleName));
  }

//  /**
//   * Project updater for adding Maven namespace declaration
//   */
//  public static class NamespaceAdder extends ProjectUpdater {
//
//    public void update(Model model) {
//      DocumentRoot documentRoot = PomFactory.eINSTANCE.createDocumentRoot();
//      EMap<String, String> prefixMap = documentRoot.getXMLNSPrefixMap();
//      EMap<String, String> schemaLocation = documentRoot.getXSISchemaLocation();
//
//      // xmlns="http://maven.apache.org/POM/4.0.0" 
//      // xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
//      // xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd"      
//      
////      XmlCursor cursor = project.newCursor();
////      cursor.toNextToken();
////      if(!cursor.toFirstAttribute()) {
////        cursor.toNextToken();
////      }
////
////      String uri = ProjectDocument.type.getDocumentElementName().getNamespaceURI();
////      cursor.insertNamespace("", uri);
////      cursor.insertNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
////      cursor.insertAttributeWithValue( //
////          new QName("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation", "xsi"), uri
////              + " http://maven.apache.org/maven-v4_0_0.xsd");
//    }
//
//  }

  /**
   * Project updater for adding dependencies
   */
  public static class DependencyAdder extends ProjectUpdater {

    private final org.apache.maven.model.Dependency dependency;

    public DependencyAdder(org.apache.maven.model.Dependency dependency) {
      this.dependency = dependency;
    }

    public void update(org.maven.ide.components.pom.Model model) {
      Dependencies dependencies = model.getDependencies();
      if(dependencies==null) {
        dependencies = POM_FACTORY.createDependencies();
        model.setDependencies(dependencies);
      }
      
      Dependency dependency = POM_FACTORY.createDependency();
      
      dependency.setGroupId(this.dependency.getGroupId());
      dependency.setArtifactId(this.dependency.getArtifactId());
      
      if(this.dependency.getVersion()!=null) {
        dependency.setVersion(this.dependency.getVersion());
      }
      
      if(this.dependency.getClassifier() != null) {
        dependency.setClassifier(this.dependency.getClassifier());
      }
      
      if(this.dependency.getType() != null //
          && !"jar".equals(this.dependency.getType()) //
          && !"null".equals(this.dependency.getType())) { // guard against MNGECLIPSE-622
        dependency.setType(this.dependency.getType());
      }
      
      if(this.dependency.getScope() != null && !"compile".equals(this.dependency.getScope())) {
        dependency.setScope(this.dependency.getScope());
      }
      
      if(this.dependency.getSystemPath() != null) {
        dependency.setSystemPath(this.dependency.getSystemPath());
      }
      
      if(this.dependency.isOptional()) {
        dependency.setOptional("true");
      }

      if(!this.dependency.getExclusions().isEmpty()) {
        ExclusionsType exclusions = POM_FACTORY.createExclusionsType();
        EList<Exclusion> exclusionList = exclusions.getExclusion();

        @SuppressWarnings("unchecked")
        Iterator<org.apache.maven.model.Exclusion> it = this.dependency.getExclusions().iterator();
        while(it.hasNext()) {
          org.apache.maven.model.Exclusion e = it.next();
          Exclusion exclusion = POM_FACTORY.createExclusion();
          exclusion.setGroupId(e.getGroupId());
          exclusion.setArtifactId(e.getArtifactId());
          exclusionList.add(exclusion);
        }
        
        dependency.setExclusions(exclusions);
      }
      
      // search for dependency with same GAC and remove if found
      Iterator<Dependency> it = dependencies.getDependency().iterator();
      boolean mergeScope = false;
      String oldScope = Artifact.SCOPE_COMPILE;
      while (it.hasNext()) {
        Dependency dep = it.next();
        if (dep.getGroupId().equals(dependency.getGroupId()) && 
            dep.getArtifactId().equals(dependency.getArtifactId()) &&
            compareNulls(dep.getClassifier(), dependency.getClassifier())) {
          oldScope = dep.getScope();
          it.remove();
          mergeScope = true;
        }
      }
      
      if (mergeScope) {
        // merge scopes
        if (oldScope == null) {
          oldScope = Artifact.SCOPE_COMPILE;
        }
        
        String newScope = this.dependency.getScope();
        if (newScope == null) {
          newScope = Artifact.SCOPE_COMPILE;
        }
        
        if (!oldScope.equals(newScope)) {
          boolean systemScope = false;
          boolean providedScope = false;
          boolean compileScope = false;
          boolean runtimeScope = false;
          boolean testScope = false;
  
          // test old scope
          if ( Artifact.SCOPE_COMPILE.equals( oldScope ) ) {
            systemScope = true;
            providedScope = true;
            compileScope = true;
            runtimeScope = false;
            testScope = false;
          } else if ( Artifact.SCOPE_RUNTIME.equals( oldScope ) ) {
            systemScope = false;
            providedScope = false;
            compileScope = true;
            runtimeScope = true;
            testScope = false;
          } else if ( Artifact.SCOPE_TEST.equals( oldScope ) ) {
            systemScope = true;
            providedScope = true;
            compileScope = true;
            runtimeScope = true;
            testScope = true;
          }

          // merge with new one
          if ( Artifact.SCOPE_COMPILE.equals( newScope ) ) {
            systemScope = systemScope || true;
            providedScope = providedScope || true;
            compileScope = compileScope || true;
            runtimeScope = runtimeScope || false;
            testScope = testScope || false;
          } else if ( Artifact.SCOPE_RUNTIME.equals( newScope ) ) {
            systemScope = systemScope || false;
            providedScope = providedScope || false;
            compileScope = compileScope || true;
            runtimeScope = runtimeScope || true;
            testScope = testScope || false;
          } else if ( Artifact.SCOPE_TEST.equals( newScope ) ) {
            systemScope = systemScope || true;
            providedScope = providedScope || true;
            compileScope = compileScope || true;
            runtimeScope = runtimeScope || true;
            testScope = testScope || true;
          }
          
          if (testScope) {
            newScope = Artifact.SCOPE_TEST;
          } else if (runtimeScope) {
            newScope = Artifact.SCOPE_RUNTIME;
          } else if (compileScope) {
            newScope = Artifact.SCOPE_COMPILE;
          } else {
            // unchanged
          }

          dependency.setScope(newScope);
        }
      }
      
      dependencies.getDependency().add(dependency);
    }

    @SuppressWarnings("null")
    private boolean compareNulls(String s1, String s2) {
      if (s1 == null && s2 == null) {
        return true;
      }
      if ((s1 == null && s2 != null) || (s2 == null && s1 != null)) {
        return false;
      }
      return s1.equals(s2);   
    }
  }
  

  /**
   * Project updater for adding modules
   */
  public static class ModuleAdder extends ProjectUpdater {

    private final String moduleName;

    public ModuleAdder(String moduleName) {
      this.moduleName = moduleName;
    }

    public void update(Model model) {
      Modules modules = model.getModules();
      if(modules==null) {
        modules = POM_FACTORY.createModules();
        model.setModules(modules);
      }

      modules.getModule().add(moduleName);
    }
  }

  /**
   * Project updater for adding plugins
   */
  public static class PluginAdder extends ProjectUpdater {

    private final String groupId;
    private final String artifactId;
    private final String version;

    public PluginAdder(String groupId, String artifactId, String version) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
    }

    public void update(Model model) {
      Build build = model.getBuild();
      if(build==null) {
        build = POM_FACTORY.createBuild();
        model.setBuild(build);
      }

      Plugins plugins = build.getPlugins();
      if(plugins==null) {
        plugins = POM_FACTORY.createPlugins();
        build.setPlugins(plugins);
      }

      Plugin plugin = POM_FACTORY.createPlugin();
      
      if(!"org.apache.maven.plugins".equals(this.groupId)) {
        plugin.setGroupId(this.groupId);
      }
      
      plugin.setArtifactId(this.artifactId);

      if(this.version != null) {
        plugin.setVersion(this.version);
      }

      Configuration configuration = POM_FACTORY.createConfiguration();
      plugin.setConfiguration(configuration);
      
      plugins.getPlugin().add(plugin);
    }
  }

}
