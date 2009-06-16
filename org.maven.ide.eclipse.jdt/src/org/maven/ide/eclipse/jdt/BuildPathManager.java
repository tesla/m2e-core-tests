/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt;

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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.DeltaProcessingState;
import org.eclipse.jdt.internal.core.JavaElementDelta;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.jdt.internal.ClasspathDescriptor;
import org.maven.ide.eclipse.jdt.internal.MavenClasspathContainer;
import org.maven.ide.eclipse.jdt.internal.MavenClasspathContainerSaveHelper;
import org.maven.ide.eclipse.project.DownloadSourceEvent;
import org.maven.ide.eclipse.project.IDownloadSourceListener;
import org.maven.ide.eclipse.project.IMavenProjectChangedListener;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;

/**
 * This class is responsible for mapping Maven classpath to JDT and back.
 * 
 * XXX take project import code into a separate class (ProjectImportManager?)
 */
@SuppressWarnings("restriction")
public class BuildPathManager implements IMavenProjectChangedListener, IDownloadSourceListener, IResourceChangeListener {

  // container settings
  public static final String CONTAINER_ID = "org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER"; //$NON-NLS-1$
  
  // entry attributes
  public static final String GROUP_ID_ATTRIBUTE = "maven.groupId"; //$NON-NLS-1$

  public static final String ARTIFACT_ID_ATTRIBUTE = "maven.artifactId"; //$NON-NLS-1$

  public static final String VERSION_ATTRIBUTE = "maven.version"; //$NON-NLS-1$

  public static final String CLASSIFIER_ATTRIBUTE = "maven.classifier"; //$NON-NLS-1$

  public static final String SCOPE_ATTRIBUTE = "maven.scope"; //$NON-NLS-1$

  // local repository variable
  public static final String M2_REPO = "M2_REPO"; //$NON-NLS-1$
  
  private static final String PROPERTY_SRC_ROOT = ".srcRoot"; //$NON-NLS-1$

  private static final String PROPERTY_SRC_PATH = ".srcPath"; //$NON-NLS-1$

  private static final String PROPERTY_JAVADOC_URL = ".javadoc"; //$NON-NLS-1$

  // public static final String TEST_CLASSES_FOLDERNAME = "test-classes"; //$NON-NLS-1$

  // public static final String CLASSES_FOLDERNAME = "classes"; //$NON-NLS-1$

  public static final int CLASSPATH_TEST = 0;

  public static final int CLASSPATH_RUNTIME = 1;
  
  // test is the widest possible scope, and this is what we need by default
  public static final int CLASSPATH_DEFAULT = CLASSPATH_TEST;
  
  static final ArtifactFilter SCOPE_FILTER_RUNTIME = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME); 
  static final ArtifactFilter SCOPE_FILTER_TEST = new ScopeArtifactFilter(Artifact.SCOPE_TEST);

  final MavenConsole console;

  final MavenProjectManager projectManager;

  final IProjectConfigurationManager configurationManager;

  final IndexManager indexManager;
  
  final IMavenConfiguration mavenConfiguration;
  
  final BundleContext bundleContext;
  
  final IMaven maven;
  
  final File stateLocationDir;

  private String jdtVersion;

  public BuildPathManager(MavenConsole console,
      MavenProjectManager projectManager, IndexManager indexManager, MavenModelManager modelManager,
      MavenRuntimeManager runtimeManager, BundleContext bundleContext, File stateLocationDir) {
    this.console = console;
    this.projectManager = projectManager;
    this.indexManager = indexManager;
    this.mavenConfiguration = MavenPlugin.lookup(IMavenConfiguration.class);
    this.bundleContext = bundleContext;
    this.stateLocationDir = stateLocationDir;
    this.maven = MavenPlugin.lookup(IMaven.class);
    this.configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
  }

  public static boolean isMaven2ClasspathContainer(IPath containerPath) {
    return containerPath != null && containerPath.segmentCount() > 0
        && CONTAINER_ID.equals(containerPath.segment(0));
  }
  
  public static IClasspathEntry getDefaultContainerEntry() {
    return JavaCore.newContainerEntry(new Path(CONTAINER_ID));
  }

  public static IClasspathEntry getMavenContainerEntry(IJavaProject javaProject) {
    if(javaProject != null) {
      try {
        for(IClasspathEntry entry : javaProject.getRawClasspath()) {
          if(isMaven2ClasspathContainer(entry.getPath())) {
            return entry;
          }
        }
      } catch(JavaModelException ex) {
        return null;
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
        for(IElementChangedListener listener : state.elementChangedListeners) {
          if(listener instanceof PackageExplorerContentProvider) {
            JavaElementDelta delta = new JavaElementDelta(javaProject);
            delta.changed(IJavaElementDelta.F_CLASSPATH_CHANGED);
            listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));
          }
        }
      }
    }
  }
  
  // XXX should inject version instead of looking it up 
  private synchronized String getJDTVersion() {
    if(jdtVersion==null) {
      Bundle[] bundles = bundleContext.getBundles();
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
    Set<IProject> projects = new HashSet<IProject>();
    monitor.setTaskName("Setting classpath containers");
    for (int i = 0; i < events.length; i++) {
      MavenProjectChangedEvent event = events[i];
      IFile pom = event.getSource();
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
        IPath path = containerEntry != null ? containerEntry.getPath() : new Path(CONTAINER_ID);
        IClasspathEntry[] classpath = getClasspath(project, monitor);
        IClasspathContainer container = new MavenClasspathContainer(path, classpath);
        JavaCore.setClasspathContainer(container.getPath(), new IJavaProject[] {javaProject},
            new IClasspathContainer[] {container}, monitor);
        forcePackageExplorerRefresh(javaProject);
        saveContainerState(project, container);
      } catch(CoreException ex) {
        MavenLogger.log(ex);
      }
    }
  }

  private void saveContainerState(IProject project, IClasspathContainer container) {
    File containerStateFile = getContainerStateFile(project);
    FileOutputStream is = null;
    try {
      is = new FileOutputStream(containerStateFile);
      new MavenClasspathContainerSaveHelper().writeContainer(container, is);
    } catch(IOException ex) {
      MavenLogger.log("Can't save classpath container state for " + project.getName(), ex);
    } finally {
      if(is != null) {
        try {
          is.close();
        } catch(IOException ex) {
          MavenLogger.log("Can't close output stream for " + containerStateFile.getAbsolutePath(), ex);
        }
      }
    }
  }

  public IClasspathContainer getSavedContainer(IProject project) throws CoreException {
    File containerStateFile = getContainerStateFile(project);
    if(!containerStateFile.exists()) {
      return null;
    }
    
    FileInputStream is = null;
    try {
      is = new FileInputStream(containerStateFile);
      return new MavenClasspathContainerSaveHelper().readContainer(is);
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, MavenJdtPlugin.PLUGIN_ID, -1, //
          "Can't read classpath container state for " + project.getName(), ex));
    } catch(ClassNotFoundException ex) {
      throw new CoreException(new Status(IStatus.ERROR, MavenJdtPlugin.PLUGIN_ID, -1, //
          "Can't read classpath container state for " + project.getName(), ex));
    } finally {
      if(is != null) {
        try {
          is.close();
        } catch(IOException ex) {
          MavenLogger.log("Can't close output stream for " + containerStateFile.getAbsolutePath(), ex);
        }
      }
    }
  }
  
  private IClasspathEntry[] getClasspath(IMavenProjectFacade projectFacade, final int kind, final Properties sourceAttachment, boolean uniquePaths, final IProgressMonitor monitor) throws CoreException {

    IJavaProject javaProject = JavaCore.create(projectFacade.getProject());

    final IClasspathDescriptor classpath = new ClasspathDescriptor(javaProject);

    projectFacade.accept(new IMavenProjectVisitor() {
      public boolean visit(IMavenProjectFacade mavenProject) throws CoreException {
        addClasspathEntries(classpath, mavenProject, kind, sourceAttachment, monitor);
        return true; // continue traversal
      }
    }, IMavenProjectVisitor.NESTED_MODULES);

    for (IJavaProjectConfigurator configurator : getJavaProjectConfigurators(projectFacade, monitor)) {
      configurator.configureClasspath(projectFacade, classpath, monitor);
    }

    IClasspathEntry[] entries = classpath.getEntries();

    if (uniquePaths) {
      Map<IPath, IClasspathEntry> paths = new LinkedHashMap<IPath, IClasspathEntry>();
      for (IClasspathEntry entry : entries) {
        if (!paths.containsKey(entry.getPath())) {
          paths.put(entry.getPath(), entry);
        }
      }
      return paths.values().toArray(new IClasspathEntry[paths.size()]);
    }

    return entries;
  }

  private List<IJavaProjectConfigurator> getJavaProjectConfigurators(IMavenProjectFacade projectFacade,
      final IProgressMonitor monitor) throws CoreException {

    ArrayList<IJavaProjectConfigurator> configurators = new ArrayList<IJavaProjectConfigurator>();
    
    ILifecycleMapping lifecycleMapping = configurationManager.getLifecycleMapping(projectFacade, monitor);
    
    for (AbstractProjectConfigurator configurator : lifecycleMapping.getProjectConfigurators(projectFacade, monitor)) {
      if (configurator instanceof IJavaProjectConfigurator) {
        configurators.add((IJavaProjectConfigurator) configurator);
      }
    }

    return configurators;
  }
  
  void addClasspathEntries(IClasspathDescriptor classpath, IMavenProjectFacade facade, int kind, Properties sourceAttachment, IProgressMonitor monitor) throws CoreException {
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
    
    MavenProject mavenProject = facade.getMavenProject(monitor);
    Set<Artifact> artifacts = mavenProject.getArtifacts();
    for(Artifact a : artifacts) {
      if (!scopeFilter.include(a) || !a.getArtifactHandler().isAddedToClasspath()) {
        continue;
      }

      // project
      IMavenProjectFacade dependency = projectManager.getMavenProject(a.getGroupId(), a.getArtifactId(), a.getVersion());
      if (dependency != null && dependency.getProject().equals(facade.getProject())) {
        continue;
      }

      if (dependency != null && dependency.getFullPath(a.getFile()) != null) {
        classpath.addProjectEntry(a, dependency);
        continue;
      }

      File artifactFile = a.getFile();
      if(artifactFile != null) {
        String key = new Path(artifactFile.getAbsolutePath()).toPortableString();

        IPath srcPath = null; 
        IPath srcRoot = null;
        if (sourceAttachment != null && sourceAttachment.containsKey(key + PROPERTY_SRC_PATH)) {
          srcPath = Path.fromPortableString((String) sourceAttachment.get(key + PROPERTY_SRC_PATH));
          if (sourceAttachment.containsKey(key + PROPERTY_SRC_ROOT)) {
            srcRoot = Path.fromPortableString((String) sourceAttachment.get(key + PROPERTY_SRC_ROOT));
          }
        }
        if (srcPath == null) {
          srcPath = projectManager.getSourcePath(a);
        }

        // configure javadocs if available
        String javaDocUrl = null;
        if (sourceAttachment != null && sourceAttachment.containsKey(key + PROPERTY_JAVADOC_URL)) {
          javaDocUrl = (String) sourceAttachment.get(key + PROPERTY_JAVADOC_URL);
        }
        if (javaDocUrl == null) {
          javaDocUrl = projectManager.getJavaDocUrl(a);
        }

        boolean downloadSources = srcPath==null && mavenConfiguration.isDownloadSources();
        boolean downloadJavaDoc = javaDocUrl==null && mavenConfiguration.isDownloadJavaDoc();
        downloadSources(facade.getProject(), a, downloadSources, downloadJavaDoc);

        classpath.addLibraryEntry(a, srcPath, srcRoot, javaDocUrl);

      }
    }
  }

  private void downloadSources(IProject project, Artifact artifact, boolean downloadSources, boolean downloadJavaDoc) {
    if(downloadSources || downloadJavaDoc) {
      try {
        IndexedArtifactFile af = indexManager.getIndexedArtifactFile(IndexManager.LOCAL_INDEX, //
            indexManager.getDocumentKey(new ArtifactKey(artifact)));
        if(af != null) {
          // download if sources and javadoc artifact is available from remote repositories
          boolean shouldDownloadSources = downloadSources && af.sourcesExists != IndexManager.NOT_AVAILABLE;
          boolean shouldDownloadJavaDoc = downloadJavaDoc && af.javadocExists != IndexManager.NOT_AVAILABLE;
          if(shouldDownloadSources || shouldDownloadJavaDoc) {
            projectManager.downloadSources(project, null, artifact.getGroupId(), artifact.getArtifactId(), //
                artifact.getVersion(), artifact.getClassifier(), shouldDownloadSources, shouldDownloadJavaDoc);
          }
        }
      } catch(Exception ex) {
        MavenLogger.log(ex.getMessage(), ex);
      }
    }
  }
  
  public IClasspathEntry[] getClasspath(IProject project, int scope, IProgressMonitor monitor) throws CoreException {
    return getClasspath(project, scope, true, monitor);
  }

  public IClasspathEntry[] getClasspath(IProject project, int scope, boolean uniquePaths, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = projectManager.create(project, monitor);
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
      return getClasspath(facade, scope, props, uniquePaths, monitor);
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, MavenJdtPlugin.PLUGIN_ID, -1, //
          "Can't save classpath container changes", e));
    }
  }

  public IClasspathEntry[] getClasspath(IProject project, IProgressMonitor monitor) throws CoreException {
    return getClasspath(project, CLASSPATH_DEFAULT, monitor);
  }

  /**
   * Downloads artifact sources using background job.
   * 
   * If path is null, downloads sources for all classpath entries of the project,
   * otherwise downloads sources for the first classpath entry with the
   * given path.
   */
  public void downloadSources(IProject project, IPath path) throws CoreException {
    download(project, path, true, false);
  }

  /**
   * Downloads artifact JavaDocs using background job.
   * 
   * If path is null, downloads sources for all classpath entries of the project,
   * otherwise downloads sources for the first classpath entry with the
   * given path.
   */
  public void downloadJavaDoc(IProject project, IPath path) throws CoreException {
    download(project, path, false, true);
  }

  private void download(IProject project, IPath path, boolean downloadSources, boolean downloadJavaDoc)
      throws CoreException {
    for(ArtifactKey artifact : findArtifacts(project, path)) {
      projectManager.downloadSources(project, path, artifact.getGroupId(), artifact.getArtifactId(), //
          artifact.getVersion(), artifact.getClassifier(), downloadSources, downloadJavaDoc);
    }
  }
  
  private Set<ArtifactKey> findArtifacts(IProject project, IPath path) throws CoreException {
    ArrayList<IClasspathEntry> entries = findClasspathEntries(project, path);

    Set<ArtifactKey> artifacts = new LinkedHashSet<ArtifactKey>();

    for(IClasspathEntry entry : entries) {
      ArtifactKey artifact = findArtifactByArtifactKey(entry);

      if(artifact == null) {
        artifact = findArtifactInIndex(project, entry);
        if(artifact == null) {
          // console.logError("Can't find artifact for " + entry.getPath());
        } else {
          // console.logMessage("Found indexed artifact " + artifact + " for " + entry.getPath());
          artifacts.add(artifact);
        }
      } else {
        // console.logMessage("Found artifact " + artifact + " for " + entry.getPath());
        artifacts.add(artifact);
      }
    }

    return artifacts;
  }

  public ArtifactKey findArtifact(IProject project, IPath path) throws CoreException {
    if (path != null) {
      Set<ArtifactKey> artifacts = findArtifacts(project, path);
      // it is not possible to have more than one classpath entry with the same path
      if (artifacts.size() > 0) {
        return artifacts.iterator().next();
      }
    }
    return null;
  }
  
  private ArtifactKey findArtifactByArtifactKey(IClasspathEntry entry) {
    IClasspathAttribute[] attributes = entry.getExtraAttributes();
    String groupId = null;
    String artifactId = null;
    String version = null;
    String classifier = null;
    for(int j = 0; j < attributes.length; j++ ) {
      if(GROUP_ID_ATTRIBUTE.equals(attributes[j].getName())) {
        groupId = attributes[j].getValue();
      } else if(ARTIFACT_ID_ATTRIBUTE.equals(attributes[j].getName())) {
        artifactId = attributes[j].getValue();
      } else if(VERSION_ATTRIBUTE.equals(attributes[j].getName())) {
        version = attributes[j].getValue();
      } else if(CLASSIFIER_ATTRIBUTE.equals(attributes[j].getName())) {
        classifier = attributes[j].getValue();
      }
    }

    if(groupId != null && artifactId != null && version != null) {
      return new ArtifactKey(groupId, artifactId, version, classifier);
    }
    return null;
  }

  private ArtifactKey findArtifactInIndex(IProject project, IClasspathEntry entry) throws CoreException {
    IFile jarFile = project.getWorkspace().getRoot().getFile(entry.getPath());
    File file = jarFile==null || jarFile.getLocation()==null ? entry.getPath().toFile() : jarFile.getLocation().toFile();

    IndexedArtifactFile iaf = indexManager.identify(file);
    if(iaf!=null) {
      return new ArtifactKey(iaf.group, iaf.artifact, iaf.version, iaf.classifier);
    }
      
    return null;
  }

  // TODO should it be just one entry?
  private ArrayList<IClasspathEntry> findClasspathEntries(IProject project, IPath path) throws JavaModelException {
    ArrayList<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();

    IJavaProject javaProject = JavaCore.create(project);
    addEntries(entries, javaProject.getRawClasspath(), path);

    IClasspathContainer container = getMaven2ClasspathContainer(javaProject);
    if(container!=null) {
      addEntries(entries, container.getClasspathEntries(), path);
    }
    return entries;
  }

  private void addEntries(Collection<IClasspathEntry> collection, IClasspathEntry[] entries, IPath path) {
    for(IClasspathEntry entry : entries) {
      if(entry.getEntryKind()==IClasspathEntry.CPE_LIBRARY && (path==null || path.equals(entry.getPath()))) {
        collection.add(entry);
      }
    }
  }

  public void sourcesDownloaded(DownloadSourceEvent event, IProgressMonitor monitor) throws CoreException {
    IProject project = event.getSource();
    IJavaProject javaProject = JavaCore.create(project);

    IMavenProjectFacade mavenProject = projectManager.create(project, monitor);
    if (mavenProject != null) {
      // Maven project, refresh classpath container
      updateClasspath(project, monitor);
    }
    
    IPath path = event.getPath();
    if(path != null) {
      // non-Maven jar, modify specific classpath entry
      IClasspathEntry[] cp = javaProject.getRawClasspath();
      for(int i = 0; i < cp.length; i++ ) {
        IClasspathEntry entry = cp[i];
        if (IClasspathEntry.CPE_LIBRARY == entry.getEntryKind() && path.equals(entry.getPath())) {
          List<IClasspathAttribute> attributes = new ArrayList<IClasspathAttribute>(Arrays.asList(entry.getExtraAttributes()));
          IPath srcPath = event.getSourcePath();

          if(srcPath == null) {
            // configure javadocs if available
            String javaDocUrl = event.getJavadocUrl();
            if(javaDocUrl != null) {
              attributes.add(JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
                  javaDocUrl));
            }
          }

          cp[i] = JavaCore.newLibraryEntry(entry.getPath(), srcPath, null, entry.getAccessRules(), //
              attributes.toArray(new IClasspathAttribute[attributes.size()]), // 
              entry.isExported());
        }
      }

      javaProject.setRawClasspath(cp, monitor);
    }
  }

  /**
   * Extracts and persists custom source/javadoc attachment info
   */
  public void updateClasspathContainer(IJavaProject project, IClasspathContainer containerSuggestion, IProgressMonitor monitor) throws CoreException {
    IFile pom = project.getProject().getFile(IMavenConstants.POM_FILE_NAME);
    IMavenProjectFacade facade = projectManager.create(pom, false, null);
    if (facade == null) {
      return;
    }

    // collect all source/javadoc attachement
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

    // eliminate all "standard" source/javadoc attachement we get from local repo
    entries = getClasspath(facade, CLASSPATH_DEFAULT, null, true, monitor);
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

    // persist custom source/javadoc attachement info
    File file = getSourceAttachmentPropertiesFile(project.getProject());
    try {
      OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
      try {
        props.store(os, null);
      } finally {
        os.close();
      }
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, MavenJdtPlugin.PLUGIN_ID, -1, "Can't save classpath container changes", e));
    }

    // update classpath container. suboptimal as this will re-calculate classpath
    updateClasspath(project.getProject(), monitor);
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
    return new File(stateLocationDir, project.getName() + ".sources");
  }

  /** public for unit tests only */
  public File getContainerStateFile(IProject project) {
    return new File(stateLocationDir, project.getName() + ".container");
  }
  
  public void resourceChanged(IResourceChangeEvent event) {
    int type = event.getType();
    if(IResourceChangeEvent.PRE_DELETE == type) {
      // remove custom source and javadoc configuration
      File attachmentProperties = getSourceAttachmentPropertiesFile((IProject) event.getResource());
      if(attachmentProperties.exists() && !attachmentProperties.delete()) {
        MavenLogger.log("Can't delete " + attachmentProperties.getAbsolutePath(), null);
      }
      
      // remove classpath container state
      File containerState = getContainerStateFile((IProject) event.getResource());
      if(containerState.exists() && !containerState.delete()) {
        MavenLogger.log("Can't delete " + containerState.getAbsolutePath(), null);
      }
    }
  }

  public boolean setupVariables() {
    boolean changed = false;
    try {
      File localRepositoryDir = new File(maven.getLocalRepository().getBasedir());
      IPath oldPath = JavaCore.getClasspathVariable(M2_REPO);
      IPath newPath = new Path(localRepositoryDir.getAbsolutePath());
      JavaCore.setClasspathVariable(M2_REPO, //
          newPath, //
          new NullProgressMonitor());
      changed = !newPath.equals(oldPath);
    } catch(CoreException ex) {
      MavenLogger.log(ex);
      changed = false;
    }
    return changed;
  }
  
  public boolean variablesAreInUse() {
    try {
      IJavaModel model= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
      IJavaProject[] projects= model.getJavaProjects();
      for (int i= 0; i < projects.length; i++) {
        IClasspathEntry[] entries= projects[i].getRawClasspath();
        for (int k= 0; k < entries.length; k++) {
          IClasspathEntry curr= entries[k];
          if (curr.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
            String var= curr.getPath().segment(0);
            if (M2_REPO.equals(var)) {
              return true;
            }
          }
        }
      }
    } catch (JavaModelException e) {
      return true;
    }
    return false;
  }
  
}
