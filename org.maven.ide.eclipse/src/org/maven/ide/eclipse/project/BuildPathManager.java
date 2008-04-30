/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.DeltaProcessingState;
import org.eclipse.jdt.internal.core.JavaElementDelta;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;

import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.digest.Sha1Digester;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;

import org.maven.ide.eclipse.MavenConsole;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.container.MavenClasspathContainer;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.internal.project.ClasspathConfiguratorFactoryFactory;
import org.maven.ide.eclipse.project.configurator.AbstractClasspathConfigurator;
import org.maven.ide.eclipse.project.configurator.AbstractClasspathConfiguratorFactory;

/**
 * This class is responsible for mapping Maven classpath to JDT and back.
 * 
 * XXX take project import code into a separate class (ProjectImportManager?)
 */
public class BuildPathManager implements IMavenProjectChangedListener, IDownloadSourceListener, IResourceChangeListener {

  private static final String PROPERTY_SRC_ROOT = ".srcRoot";

  private static final String PROPERTY_SRC_PATH = ".srcPath";

  private static final String PROPERTY_JAVADOC_URL = ".javadoc"; 

  public static final String TEST_CLASSES_FOLDERNAME = "test-classes";

  public static final String CLASSES_FOLDERNAME = "classes";

  public static final String TEST_TYPE = "test";

  public static final int CLASSPATH_TEST = 0;

  public static final int CLASSPATH_RUNTIME = 1;
  
  // test is the widest possible scope, and this is what we need by default
  public static final int CLASSPATH_DEFAULT = CLASSPATH_TEST;
  
  static final ArtifactFilter SCOPE_FILTER_RUNTIME = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME); 
  static final ArtifactFilter SCOPE_FILTER_TEST = new ScopeArtifactFilter(Artifact.SCOPE_TEST);
  
  final MavenEmbedderManager embedderManager;

  final MavenConsole console;

  final MavenProjectManager projectManager;

  final IndexManager indexManager;
  
  final MavenRuntimeManager runtimeManager;
  
  private String jdtVersion;

  public BuildPathManager(MavenEmbedderManager embedderManager, MavenConsole console,
      MavenProjectManager projectManager, IndexManager indexManager, MavenModelManager modelManager,
      MavenRuntimeManager runtimeManager) {
    this.embedderManager = embedderManager;
    this.console = console;
    this.projectManager = projectManager;
    this.indexManager = indexManager;
    this.runtimeManager = runtimeManager;
  }

  public static IClasspathEntry getDefaultContainerEntry() {
    return JavaCore.newContainerEntry(new Path(MavenPlugin.CONTAINER_ID));
  }

  public static IClasspathEntry getMavenContainerEntry(IJavaProject javaProject) {
    if(javaProject == null) {
      return null;
    }
    
    IClasspathEntry[] classpath;
    try {
      classpath = javaProject.getRawClasspath();
    } catch(JavaModelException ex) {
      return null;
    }
    for(int i = 0; i < classpath.length; i++ ) {
      IClasspathEntry entry = classpath[i];
      if(isMaven2ClasspathContainer(entry.getPath())) {
        return entry;
      }
    }
    return null;
  }

  /**
     XXX In Eclipse 3.3, changes to resolved classpath are not announced by JDT Core
     and PackageExplorer does not properly refresh when we update Maven
     classpath container.
     As a temporary workaround, send F_CLASSPATH_CHANGED notifications
     to all PackageExplorerContentProvider instances listening to
     java ElementChangedEvent. 
     Note that even with this hack, build clean is sometimes necessary to
     reconcile PackageExplorer with actual classpath
     See https://bugs.eclipse.org/bugs/show_bug.cgi?id=154071
   */
  private void forcePackageExplorerRefresh(IJavaProject javaProject) {
    if(getJDTVersion().startsWith("3.3")) {
      DeltaProcessingState state = JavaModelManager.getJavaModelManager().deltaState;
      synchronized(state) {
        IElementChangedListener[] listeners = state.elementChangedListeners;
        for(int i = 0; i < listeners.length; i++ ) {
          if(listeners[i] instanceof PackageExplorerContentProvider) {
            JavaElementDelta delta = new JavaElementDelta(javaProject);
            delta.changed(IJavaElementDelta.F_CLASSPATH_CHANGED);
            listeners[i].elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));
          }
        }
      }
    }
  }
  
  // XXX should inject version instead of looking it up 
  private synchronized String getJDTVersion() {
    if(jdtVersion==null) {
      Bundle[] bundles = MavenPlugin.getDefault().getBundleContext().getBundles();
      for(int i = 0; i < bundles.length; i++ ) {
        if(JavaCore.PLUGIN_ID.equals(bundles[i].getSymbolicName())) {
          jdtVersion = (String) bundles[i].getHeaders().get(Constants.BUNDLE_VERSION);
          break;
        }
      }
    }
    return jdtVersion;
  }

  public static IClasspathContainer getMaven2ClasspathContainer(IJavaProject project) throws JavaModelException {
    IClasspathEntry[] entries = project.getRawClasspath();
    for(int i = 0; i < entries.length; i++ ) {
      IClasspathEntry entry = entries[i];
      if(entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && isMaven2ClasspathContainer(entry.getPath())) {
        return JavaCore.getClasspathContainer(entry.getPath(), project);
      }
    }
    return null;
  }

  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    Set projects = new HashSet();
    monitor.setTaskName("Setting classpath containers");
    for (int i = 0; i < events.length; i++) {
      MavenProjectChangedEvent event = events[i];
      IFile pom = (IFile) event.getSource();
      IProject project = pom.getProject();
      if (project.isAccessible() && projects.add(project)) {
        updateClasspath(project, monitor);
      }
    }
  }

  public void updateClasspath(IProject project, IProgressMonitor monitor) {
    IJavaProject javaProject = JavaCore.create(project);
    if(javaProject != null) {
      try {
        IClasspathEntry containerEntry = getMavenContainerEntry(javaProject);
        IPath path = containerEntry != null ? containerEntry.getPath() : new Path(MavenPlugin.CONTAINER_ID);
        IClasspathEntry[] classpath = getClasspath(project, monitor);
        IClasspathContainer container = new MavenClasspathContainer(path, classpath);
        JavaCore.setClasspathContainer(container.getPath(), new IJavaProject[] {javaProject},
            new IClasspathContainer[] {container}, monitor);
        forcePackageExplorerRefresh(javaProject);
      } catch(CoreException ex) {
        // TODO Auto-generated catch block
        MavenPlugin.log(ex);
      }
    }
  }

  private IClasspathEntry[] getClasspath(MavenProjectFacade projectFacade, final int kind, final Properties sourceAttachment) throws CoreException {
    IProject project = projectFacade.getProject();

    // maps entry path to entry to avoid dups caused by different entry attributes
    final Map entries = new LinkedHashMap();

    final List configurators = new ArrayList();
    Set factories = ClasspathConfiguratorFactoryFactory.getFactories();
    for (Iterator it = factories.iterator(); it.hasNext(); ) {
      AbstractClasspathConfiguratorFactory factory = (AbstractClasspathConfiguratorFactory) it.next();
      AbstractClasspathConfigurator configurator = factory.createConfigurator(projectFacade);
      if (configurator != null) {
        configurators.add(configurator);
      }
    }

    projectFacade.accept(new IMavenProjectVisitor() {
      public boolean visit(MavenProjectFacade mavenProject) {
        addClasspathEntries(entries, mavenProject, kind, sourceAttachment, configurators);
        return true; // continue traversal
      }
      
      public void visit(MavenProjectFacade mavenProject, Artifact artifact) {
      }
    }, IMavenProjectVisitor.NESTED_MODULES);

    for (Iterator ci = configurators.iterator(); ci.hasNext(); ) {
      AbstractClasspathConfigurator cpc = (AbstractClasspathConfigurator) ci.next();
      cpc.configureClasspath(entries);
    }

    return (IClasspathEntry[]) entries.values().toArray(new IClasspathEntry[entries.size()]);
  }

  void addClasspathEntries(Map entries, MavenProjectFacade mavenProject, int kind, Properties sourceAttachment, List configurators) {
    ArtifactFilter scopeFilter;

    if(CLASSPATH_RUNTIME == kind) {
      // ECLIPSE-33: runtime+provided scope
      // ECLIPSE-85: adding system scope
      scopeFilter = new ArtifactFilter() {
        public boolean include(Artifact artifact) {
          return SCOPE_FILTER_RUNTIME.include(artifact) 
                  || Artifact.SCOPE_PROVIDED.equals( artifact.getScope() )
                  || Artifact.SCOPE_SYSTEM.equals( artifact.getScope() );
        }
      };
    } else {
      // ECLIPSE-33: test scope (already includes provided)
      scopeFilter = SCOPE_FILTER_TEST;
    }

    for(Iterator it = mavenProject.getMavenProject().getArtifacts().iterator(); it.hasNext();) {
      Artifact a = (Artifact) it.next();

      if (!scopeFilter.include(a) || !a.getArtifactHandler().isAddedToClasspath()) {
        continue;
      }

      ArrayList attributes = new ArrayList();

      // project
      MavenProjectFacade dependency = projectManager.getMavenProject(a);
      if (dependency != null && dependency.getProject().equals(mavenProject.getProject())) {
        continue;
      }

      if (dependency != null && dependency.getFullPath(a.getFile()) != null) {
        entries.put(dependency.getFullPath(), JavaCore.newProjectEntry(dependency.getFullPath(), false));
        continue;
      }

      File artifactFile = a.getFile();
      if(artifactFile != null) {
        String artifactLocation = artifactFile.getAbsolutePath();
        IPath entryPath = new Path(artifactLocation);

        attributes.add(JavaCore.newClasspathAttribute(MavenPlugin.GROUP_ID_ATTRIBUTE, a.getGroupId()));
        attributes.add(JavaCore.newClasspathAttribute(MavenPlugin.ARTIFACT_ID_ATTRIBUTE, a.getArtifactId()));
        attributes.add(JavaCore.newClasspathAttribute(MavenPlugin.VERSION_ATTRIBUTE, a.getVersion()));
        if (a.getClassifier() != null) {
          attributes.add(JavaCore.newClasspathAttribute(MavenPlugin.CLASSIFIER_ATTRIBUTE, a.getClassifier()));
        }

        String key = entryPath.toPortableString();

        IPath srcPath = null, srcRoot = null;
        if (sourceAttachment != null && sourceAttachment.containsKey(key + PROPERTY_SRC_PATH)) {
          srcPath = Path.fromPortableString((String) sourceAttachment.get(key + PROPERTY_SRC_PATH));
          if (sourceAttachment.containsKey(key + PROPERTY_SRC_ROOT)) {
            srcRoot = Path.fromPortableString((String) sourceAttachment.get(key + PROPERTY_SRC_ROOT));
          }
        }
        if (srcPath == null) {
          srcPath = projectManager.getSourcePath(mavenProject.getProject(), null, a, runtimeManager.isDownloadSources());
        }

        // configure javadocs if available
        String javaDocUrl = null;
        if (sourceAttachment != null && sourceAttachment.containsKey(key + PROPERTY_JAVADOC_URL)) {
          javaDocUrl = (String) sourceAttachment.get(key + PROPERTY_JAVADOC_URL);
        }
        if (javaDocUrl == null) {
          javaDocUrl = projectManager.getJavaDocUrl(a);
        }
        if(javaDocUrl != null) {
          attributes.add(JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
              javaDocUrl));
        }

        for (Iterator ci = configurators.iterator(); ci.hasNext(); ) {
          AbstractClasspathConfigurator cpc = (AbstractClasspathConfigurator) ci.next();
          Set set = cpc.getAttributes(a, kind);
          if (set != null) {
            attributes.addAll(set);
          }
        }

        entries.put(entryPath, JavaCore.newLibraryEntry(entryPath, //
            srcPath, srcRoot, new IAccessRule[0], //
            (IClasspathAttribute[]) attributes.toArray(new IClasspathAttribute[attributes.size()]), // 
            false /*not exported*/));
      }
    }

  }

  public IClasspathEntry[] getClasspath(IProject project, int scope, IProgressMonitor monitor) throws CoreException {
    MavenProjectFacade facade = projectManager.create(project, monitor);
    if (facade == null) {
      return new IClasspathEntry[0];
    }
    try {
      Properties props = new Properties();
      File file = getSourceAttachmentPropertiesFile(project);
      if (file.canRead()) {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        try {
          props.load(is);
        } finally {
          is.close();
        }
      }
      return getClasspath(facade, scope, props);
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, "Can't save classpath container changes", e));
    }
  }

  public IClasspathEntry[] getClasspath(IProject project, IProgressMonitor monitor) throws CoreException {
    return getClasspath(project, CLASSPATH_DEFAULT, monitor);
  }

  /**
   * Downloads sources for the given project using background job.
   * 
   * If path is null, downloads sources for all classpath entries of the project,
   * otherwise downloads sources for the first classpath entry with the
   * given path.
   */
  public void downloadSources(IProject project, IPath path) throws CoreException {
    Set artifacts = findArtifacts(project, path);
    for (Iterator it = artifacts.iterator(); it.hasNext(); ) {
      Artifact artifact = (Artifact) it.next();
      projectManager.downloadSources(project, path, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getClassifier());
    }
  }

  public Artifact findArtifact(IProject project, IPath path) throws CoreException {
    if (path != null) {
      Set artifacts = findArtifacts(project, path);
      // it is not possible to have more than one classpath entry with the same path
      if (artifacts.size() > 0) {
        return (Artifact) artifacts.iterator().next();
      }
    }
    return null;
  }

  private Set findArtifacts(IProject project, IPath path) throws CoreException {
    ArrayList entries = findClasspathEntries(project, path);

    Set artifacts = new LinkedHashSet();

    for(Iterator it = entries.iterator(); it.hasNext();) {
      IClasspathEntry entry = (IClasspathEntry) it.next();

      Artifact artifact = findArtifactByArtifactKey(entry);

      if(artifact == null) {
        artifact = findArtifactInIndex(project, entry);
        if(artifact == null) {
          console.logError("Can't find artifact for " + entry.getPath());
        } else {
          console.logMessage("Found indexed artifact " + artifact + " for " + entry.getPath());
          artifacts.add(artifact);
        }
      } else {
        console.logMessage("Found artifact " + artifact + " for " + entry.getPath());
        artifacts.add(artifact);
      }
    }

    return artifacts;
  }

  private Artifact findArtifactByArtifactKey(IClasspathEntry entry) {
    IClasspathAttribute[] attributes = entry.getExtraAttributes();
    String groupId = null;
    String artifactId = null;
    String version = null;
    String classifier = null;
    for(int j = 0; j < attributes.length; j++ ) {
      if(MavenPlugin.GROUP_ID_ATTRIBUTE.equals(attributes[j].getName())) {
        groupId = attributes[j].getValue();
      } else if(MavenPlugin.ARTIFACT_ID_ATTRIBUTE.equals(attributes[j].getName())) {
        artifactId = attributes[j].getValue();
      } else if(MavenPlugin.VERSION_ATTRIBUTE.equals(attributes[j].getName())) {
        version = attributes[j].getValue();
      } else if(MavenPlugin.CLASSIFIER_ATTRIBUTE.equals(attributes[j].getName())) {
        classifier = attributes[j].getValue();
      }
    }

    if(groupId != null && artifactId != null && version != null) {
      if (classifier != null) {
        return embedderManager.getWorkspaceEmbedder().createArtifactWithClassifier(groupId, artifactId, version, "jar", classifier);
      } 
      return embedderManager.getWorkspaceEmbedder().createArtifact(groupId, artifactId, version, null, "jar");
    }
    return null;
  }

  private Artifact findArtifactInIndex(IProject project, IClasspathEntry entry) throws CoreException {
    // calculate md5
    try {
      IFile jarFile = project.getWorkspace().getRoot().getFile(entry.getPath());
      File file = jarFile==null || jarFile.getLocation()==null ? entry.getPath().toFile() : jarFile.getLocation().toFile();

      Sha1Digester digester = new Sha1Digester();
      String sha1 = digester.calc(file);
      console.logMessage("Artifact digest " + sha1 + " for " + entry.getPath());

      Map result = indexManager.search(sha1, IndexManager.SEARCH_SHA1);
      if(result.size()==1) {
        IndexedArtifact ia = (IndexedArtifact) result.values().iterator().next();
        IndexedArtifactFile iaf = (IndexedArtifactFile) ia.files.iterator().next();
        return embedderManager.getWorkspaceEmbedder().createArtifact(iaf.group, iaf.artifact, iaf.version, null, "jar");
      }
      
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, 0, "Search error", ex));
    } catch(DigesterException ex) {
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, 0, "MD5 calculation error", ex));
    }
    
    return null;
  }

  // TODO should it be just one entry?
  private ArrayList findClasspathEntries(IProject project, IPath path) throws JavaModelException {
    ArrayList entries = new ArrayList();

    IJavaProject javaProject = JavaCore.create(project);
    addEntries(entries, javaProject.getRawClasspath(), path);

    IClasspathContainer container = getMaven2ClasspathContainer(javaProject);
    if(container!=null) {
      addEntries(entries, container.getClasspathEntries(), path);
    }
    return entries;
  }

  private void addEntries(Collection collection, IClasspathEntry[] entries, IPath path) {
    for(int i = 0; i < entries.length; i++ ) {
      IClasspathEntry entry = entries[i];
      if(entry.getEntryKind()==IClasspathEntry.CPE_LIBRARY && (path==null || path.equals(entry.getPath()))) {
        collection.add(entry);
      }
    }
  }

  public void sourcesDownloaded(DownloadSourceEvent event, IProgressMonitor monitor) throws CoreException {
    IProject project = (IProject) event.getSource();
    IJavaProject javaProject = JavaCore.create(project);

    MavenProjectFacade mavenProject = projectManager.create(project, monitor);

    if (mavenProject != null) {
      // maven project, refresh classpath container
      updateClasspath(project, monitor);
    } else {
      // non-maven project, modify specific classpath entry
      IPath path = event.getPath();

      IClasspathEntry[] cp = javaProject.getRawClasspath();
      for (int i = 0; i < cp.length; i++) {
        if (IClasspathEntry.CPE_LIBRARY == cp[i].getEntryKind() && path.equals(cp[i].getPath())) {

          List attributes = new ArrayList(Arrays.asList(cp[i].getExtraAttributes()));
          IPath srcPath = event.getSourcePath();

          if(srcPath == null) {
            // configure javadocs if available
            String javaDocUrl = event.getJavadocUrl();
            if(javaDocUrl != null) {
              attributes.add(JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
                  javaDocUrl));
            }
          }

          cp[i] = JavaCore.newLibraryEntry(cp[i].getPath(), //
              srcPath, null, cp[i].getAccessRules(), //
              (IClasspathAttribute[]) attributes.toArray(new IClasspathAttribute[attributes.size()]), // 
              cp[i].isExported());
        }
      }

      javaProject.setRawClasspath(cp, monitor);
    }

  }
  
  public void updateClasspathContainer(IJavaProject project, IClasspathContainer containerSuggestion) throws CoreException {
    IFile pom = project.getProject().getFile(MavenPlugin.POM_FILE_NAME);
    MavenProjectFacade facade = projectManager.create(pom, false, null);
    if (facade == null) {
      return;
    }

    Properties props = new Properties();
    IClasspathEntry[] entries = containerSuggestion.getClasspathEntries();
    for (int i = 0; i < entries.length; i++) {
      IClasspathEntry entry = entries[i];
      if (IClasspathEntry.CPE_LIBRARY == entry.getEntryKind()) {
        String path = entry.getPath().toPortableString();
        if (entry.getSourceAttachmentPath() != null) {
          props.put(path + PROPERTY_SRC_PATH, entry.getSourceAttachmentPath().toPortableString());
        }
        if (entry.getSourceAttachmentRootPath() != null) {
          props.put(path + PROPERTY_SRC_ROOT, entry.getSourceAttachmentRootPath().toPortableString());
        }
        String javadocUrl = getJavadocLocation(entry);
        if (javadocUrl != null) {
          props.put(path + PROPERTY_JAVADOC_URL, javadocUrl);
        }
      }
    }

    entries = getClasspath(facade, CLASSPATH_DEFAULT, null);
    for (int i = 0; i < entries.length; i++) {
      IClasspathEntry entry = entries[i];
      if (IClasspathEntry.CPE_LIBRARY == entry.getEntryKind()) {
        String path = entry.getPath().toPortableString();
        String value = (String) props.get(path + PROPERTY_SRC_PATH);
        if (value != null && entry.getSourceAttachmentPath() != null && value.equals(entry.getSourceAttachmentPath().toPortableString())) {
          props.remove(path + PROPERTY_SRC_PATH);
        }
        value = (String) props.get(path + PROPERTY_SRC_ROOT);
        if (value != null && entry.getSourceAttachmentRootPath() != null && value.equals(entry.getSourceAttachmentRootPath().toPortableString())) {
          props.remove(path + PROPERTY_SRC_ROOT);
        }
      }
    }

    File file = getSourceAttachmentPropertiesFile(project.getProject());

    try {
      OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
      try {
        props.store(os, null);
      } finally {
        os.close();
      }
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, "Can't save classpath container changes", e));
    }

    updateClasspath(project.getProject(), new NullProgressMonitor());
  }

  /** public for unit tests only */
  public String getJavadocLocation(IClasspathEntry entry) {
    IClasspathAttribute[] attributes = entry.getExtraAttributes();
    for (int j = 0; j < attributes.length; j++) {
      IClasspathAttribute attribute = attributes[j];
      if (IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME.equals(attribute.getName())) {
        return attribute.getValue();
      }
    }
    return null;
  }

  /** public for unit tests only */
  public File getSourceAttachmentPropertiesFile(IProject project) {
    File stateLocationDir = MavenPlugin.getDefault().getStateLocation().toFile();
    return new File(stateLocationDir, project.getName() + ".sources");
  }

  public void resourceChanged(IResourceChangeEvent event) {
    int type = event.getType();
    if(IResourceChangeEvent.PRE_DELETE == type) {
      getSourceAttachmentPropertiesFile((IProject) event.getResource()).delete();
    }
  }

  public static boolean isMaven2ClasspathContainer(IPath containerPath) {
    return containerPath != null && containerPath.segmentCount() > 0
        && MavenPlugin.CONTAINER_ID.equals(containerPath.segment(0));
  }

}
