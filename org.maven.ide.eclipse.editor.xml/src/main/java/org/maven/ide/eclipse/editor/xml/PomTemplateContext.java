/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.codehaus.plexus.util.IOUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.editor.xml.search.ArtifactInfo;
import org.maven.ide.eclipse.editor.xml.search.Packaging;
import org.maven.ide.eclipse.editor.xml.search.SearchEngine;
import org.maven.ide.eclipse.index.IndexManager;


/**
 * Context types.
 * 
 * @author Lukas Krecan
 * @author Eugene Kuleshov
 */
public enum PomTemplateContext {
  
  UNKNOWN("unknown"), //
  
  DOCUMENT("#document"), //
  
  PROJECT("project"), //
  
  PARENT("parent"), //
  
  PROPERTIES("properties"), //
  
  DEPENDENCIES("dependencies"), //
  
  EXCLUSIONS("exclusions"), //
  
  PLUGINS("plugins"), //

  PLUGIN("plugin"), //

  EXECUTIONS("executions"), //
  
  PROFILES("profiles"), //
  
  REPOSITORIES("repositories"), //
  
  CONFIGURATION("configuration") {
    private final Map<String, PluginDescriptor> descriptors = new HashMap<String, PluginDescriptor>();

    @Override
    protected void addTemplates(Collection<Template> proposals, Node node, String prefix) {
      String groupId = getGroupId(node);
      if(groupId==null) {
        groupId = "org.apache.maven.plugins";  // TODO support other default groups
      }
      String artifactId = getArtifactId(node);
      String version = getVersion(node);
      if(version==null) {
        Collection<String> versions = getSearchEngine().findVersions(groupId, artifactId, "", Packaging.PLUGIN);
        if(versions.isEmpty()) {
          return;
        }
        version = versions.iterator().next();
      }
      
      PluginDescriptor descriptor = getPluginDescriptor(groupId, artifactId, version);
      if(descriptor!=null) {
        @SuppressWarnings("unchecked")
        List<MojoDescriptor> mojos = descriptor.getMojos();
        HashSet<String> params = new HashSet<String>();
        for(MojoDescriptor mojo : mojos) {
          @SuppressWarnings("unchecked")
          List<Parameter> parameters = (List<Parameter>) mojo.getParameters();
          for(Parameter parameter : parameters) {
            boolean editable = parameter.isEditable();
            if(editable) {
              String name = parameter.getName();
              if(!params.contains(name)) {
                params.add(name);
                
                String text = "<b>required:</b> " + parameter.isRequired() + "<br>" //
                    + "<b>type:</b> " + parameter.getType() + "<br>";
                
                String expression = parameter.getExpression();
                if(expression!=null) {
                  text += "expression: " + expression + "<br>";
                }
                
                String defaultValue = parameter.getDefaultValue();
                if(defaultValue!=null) {
                  text += "default: " + defaultValue + "<br>";
                }
                
                String desc = parameter.getDescription().trim();
                text += desc.startsWith("<p>") ? desc : "<br>" + desc;
                
                proposals.add(new Template(name, text, getContextTypeId(), //
                    "<" + name + ">${cursor}</" + name + ">", false));
              }
            }
          }
        }
      }
    }

    private PluginDescriptor getPluginDescriptor(String groupId, String artifactId, String version) {
      String name = groupId + ":" + artifactId + ":" + version;
      PluginDescriptor descriptor = descriptors.get(name);
      if(descriptor!=null) {
        return descriptor;
      }
      
      MavenPlugin plugin = MavenPlugin.getDefault();
      MavenConsole console = plugin.getConsole();
      try {
        MavenEmbedder embedder = plugin.getMavenEmbedderManager().getWorkspaceEmbedder();
        
        Artifact artifact = embedder.createArtifact(groupId, artifactId, version, null, "maven-plugin");
        
        IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
        List<ArtifactRepository> repositories = indexManager.getArtifactRepositories(null, null);
        
        embedder.resolve(artifact, repositories, embedder.getLocalRepository());
        File file = artifact.getFile();
        if(file == null) {
          String msg = "Can't resolve plugin " + name;
          console.logError(msg);
        } else {
          InputStream is = null;
          ZipFile zf = null;
          try {
            zf = new ZipFile(file);
            ZipEntry entry = zf.getEntry("META-INF/maven/plugin.xml");
            if(entry != null) {
              is = zf.getInputStream(entry);
              PluginDescriptorBuilder builder = new PluginDescriptorBuilder();
              descriptor = builder.build(new InputStreamReader(is));
              descriptors.put(name, descriptor);
              return descriptor;
            }

          } catch(Exception ex) {
            String msg = "Can't read configuration for " + name;
            console.logError(msg);
            MavenLogger.log(msg, ex);

          } finally {
            IOUtil.close(is);
            try {
              zf.close();
            } catch(IOException ex) {
              // ignore
            }
          }
        }

      } catch(AbstractArtifactResolutionException ex) {
        String msg = "Can't resolve plugin " + name;
        console.logError(msg);
        MavenLogger.log(msg, ex);
      } catch(CoreException ex) {
        IStatus status = ex.getStatus();
        console.logError(status.getMessage() + "; " + status.getException().getMessage());
        MavenLogger.log(ex);
      }
      return null;
    }
  },
  
  GROUP_ID("groupId") {
    @Override
    public void addTemplates(Collection<Template> proposals, Node node, String prefix) {
      for(String groupId : getSearchEngine().findGroupIds(prefix, getPackaging(node), getContainingArtifact(node))) {
        proposals.add(new Template(groupId, groupId, getContextTypeId(), groupId, false));
      }
    }
  },

  ARTIFACT_ID("artifactId") {
    @Override
    public void addTemplates(Collection<Template> proposals, Node node, String prefix) {
      String groupId = getGroupId(node);
      if(groupId != null) {
        for(String artifactId : getSearchEngine().findArtifactIds(groupId, prefix, getPackaging(node),
            getContainingArtifact(node))) {
          proposals.add(new Template(artifactId, groupId + ":" + artifactId, getContextTypeId(), artifactId, false));
        }
      }
    }
  },

  VERSION("version") {
    @Override
    public void addTemplates(Collection<Template> proposals, Node node, String prefix) {
      String groupId = getGroupId(node);
      String artifactId = getArtifactId(node);
      if(groupId != null && artifactId != null) {
        for(String version : getSearchEngine().findVersions(groupId, artifactId, prefix, getPackaging(node))) {
          proposals.add(new Template(version, groupId + ":" + artifactId + ":" + version, //
              getContextTypeId(), version, false));
        }
      }
    }
  },

  CLASSIFIER("classifier") {
    @Override
    public void addTemplates(Collection<Template> proposals, Node node, String prefix) {
      String groupId = getGroupId(node);
      String artifactId = getArtifactId(node);
      String version = getVersion(node);
      if(groupId != null && artifactId != null && version != null) {
        for(String classifier : getSearchEngine().findClassifiers(groupId, artifactId, version, prefix,
            getPackaging(node))) {
          proposals.add(new Template(classifier, groupId + ":" + artifactId + ":" + version + ":" + classifier,
              getContextTypeId(), classifier, false));
        }
      }
    }
  },

  TYPE("type") {
    @Override
    public void addTemplates(Collection<Template> proposals, Node node, String prefix) {
      String groupId = getGroupId(node);
      String artifactId = getArtifactId(node);
      String version = getVersion(node);
      if(groupId != null && artifactId != null && version != null) {
        for(String type : getSearchEngine().findTypes(groupId, artifactId, version, prefix, getPackaging(node))) {
          proposals.add(new Template(type, groupId + ":" + artifactId + ":" + version + ":" + type, //
              getContextTypeId(), type, false));
        }
      }
    }
  },
  
  SYSTEM_PATH("systemPath"),
  
  PHASE("phase") {
    @Override
    public void addTemplates(Collection<Template> proposals, Node node, String prefix) {
      // TODO the following list should be derived from the packaging handler (the actual lifecycle)
      
      // Clean Lifecycle
      add(proposals, "pre-clean", "Executes processes needed prior to the actual project cleaning");
      add(proposals, "clean", "Removes all files generated by the previous build");
      add(proposals, "post-clean", "Executes processes needed to finalize the project cleaning");
      
      // Default Lifecycle
      add(proposals, "validate", "Validate the project is correct and all necessary information is available");
      add(proposals, "generate-sources", "Generate any source code for inclusion in compilation");
      add(proposals, "process-sources", "Process the source code, for example to filter any values");
      add(proposals, "generate-resources", "Generate resources for inclusion in the package");
      add(proposals, "process-resources", "Copy and process the resources into the destination directory, ready for packaging");
      add(proposals, "compile", "Compile the source code of the project");
      add(proposals, "process-classes", "Post-process the generated files from compilation, for example to do bytecode enhancement on Java classes");
      add(proposals, "generate-test-sources", "Generate any test source code for inclusion in compilation");
      add(proposals, "process-test-sources", "Process the test source code, for example to filter any values");
      add(proposals, "generate-test-resources", "Create resources for testing");
      add(proposals, "process-test-resources", "Copy and process the resources into the test destination directory");
      add(proposals, "test-compile", "Compile the test source code into the test destination directory");
      add(proposals, "process-test-classes", "Post-process the generated files from test compilation, for example to do bytecode enhancement on Java classes. For Maven 2.0.5 and above");
      add(proposals, "test", "Run tests using a suitable unit testing framework. These tests should not require the code be packaged or deployed");
      add(proposals, "prepare-package", "Perform any operations necessary to prepare a package before the actual packaging. This often results in an unpacked, processed version of the package. (Maven 2.1 and above)");
      add(proposals, "package", "Take the compiled code and package it in its distributable format, such as a JAR");
      add(proposals, "pre-integration-test", "Perform actions required before integration tests are executed. This may involve things such as setting up the required environment");
      add(proposals, "integration-test", "Process and deploy the package if necessary into an environment where integration tests can be run");
      add(proposals, "post-integration-test", "Perform actions required after integration tests have been executed. This may including cleaning up the environment");
      add(proposals, "verify", "Run any checks to verify the package is valid and meets quality criteria");
      add(proposals, "install", "Install the package into the local repository, for use as a dependency in other projects locally");
      add(proposals, "deploy", "Done in an integration or release environment, copies the final package to the remote repository for sharing with other developers and projects");
      
      // Site Lifecycle
      add(proposals, "pre-site", "Executes processes needed prior to the actual project site generation");
      add(proposals, "site", "Generates the project's site documentation");
      add(proposals, "post-site", "Executes processes needed to finalize the site generation, and to prepare for site deployment");
      add(proposals, "site-deploy", "Deploys the generated site documentation to the specified web server");
    }

    private boolean add(Collection<Template> templates, String name, String description) {
      return templates.add(new Template(name, description, getContextTypeId(), name, false));
    }
  };

  private static final String PREFIX = MvnIndexPlugin.PLUGIN_ID + ".templates.contextType.";

  private final String nodeName;

  private PomTemplateContext(String nodeName) {
    this.nodeName = nodeName;
  }

  /**
   * Return templates depending on the context type.
   */
  public Template[] getTemplates(Node node, String prefix) {
    Collection<Template> templates = new ArrayList<Template>();
    addTemplates(templates, node, prefix);
    
    TemplateStore store = MvnIndexPlugin.getDefault().getTemplateStore();
    if(store != null) {
      templates.addAll(Arrays.asList(store.getTemplates(getContextTypeId())));
    }
    
    return templates.toArray(new Template[templates.size()]);
  }
  
  protected void addTemplates(Collection<Template> templates, Node currentNode, String prefix) {
  }

  protected String getNodeName() {
    return nodeName;
  }
  
  public String getContextTypeId() {
    return PREFIX + nodeName;
  }

  public static PomTemplateContext fromId(String contextTypeId) {
    for(PomTemplateContext context : values()) {
      if(context.getContextTypeId().equals(contextTypeId)) {
        return context;
      }
    }
    return UNKNOWN;
  }

  public static PomTemplateContext fromNodeName(String idSuffix) {
    for(PomTemplateContext context : values()) {
      if(context.getNodeName().equals(idSuffix)) {
        return context;
      }
    }
    return UNKNOWN;
  }

  private static SearchEngine getSearchEngine() {
    return MvnIndexPlugin.getDefault().getSearchEngine();
  }
  
  
  ///
  
  /**
   * Returns containing artifactInfo for exclusions. Otherwise returns null.
   */
  protected ArtifactInfo getContainingArtifact(Node currentNode) {
    if(isExclusion(currentNode)) {
      Node node = currentNode.getParentNode().getParentNode();
      return getArtifactInfo(node);
    }
    return null;
  }

  /**
   * Returns artifact info from siblings of given node.
   */
  private ArtifactInfo getArtifactInfo(Node node) {
    return new ArtifactInfo(getGroupId(node), getArtifactId(node), getVersion(node), //
        getSiblingTextValue(node, "classifier"), getSiblingTextValue(node, "type"));
  }

  /**
   * Returns required packaging.
   */
  protected Packaging getPackaging(Node currentNode) {
    if(isPlugin(currentNode)) {
      return Packaging.PLUGIN;
    } else if(isParent(currentNode)) {
      return Packaging.POM;
    }
    return Packaging.ALL;
  }

  /**
   * Returns true if user is editing plugin dependency.
   */
  private boolean isPlugin(Node currentNode) {
    return "plugin".equals(currentNode.getParentNode().getNodeName());
  }
  
  /**
   * Returns true if user is editing plugin dependency exclusion.
   */
  private boolean isExclusion(Node currentNode) {
    return "exclusion".equals(currentNode.getParentNode().getNodeName());
  }
  
  /**
   * Returns true if user is editing parent dependency.
   */
  private boolean isParent(Node currentNode) {
    return "parent".equals(currentNode.getParentNode().getNodeName());
  }
  
  protected String getGroupId(Node currentNode) {
    return getSiblingTextValue(currentNode, "groupId");
  }

  protected static String getArtifactId(Node currentNode) {
    return getSiblingTextValue(currentNode, "artifactId");
  }

  protected static String getVersion(Node currentNode) {
    return getSiblingTextValue(currentNode, "version");
  }

  private static String getSiblingTextValue(Node sibling, String name) {
    Node node = getSiblingWithName(sibling, name);
    return getNodeTextValue(node);
  }

  /**
   * Returns sibling with given name.
   */
  private static Node getSiblingWithName(Node node, String name) {
    NodeList nodeList = node.getParentNode().getChildNodes();
    for(int i = 0; i < nodeList.getLength(); i++ ) {
      if(name.equals(nodeList.item(i).getNodeName())) {
        return nodeList.item(i);
      }
    }
    return null;
  }

  /**
   * Returns text value of the node.
   */
  private static String getNodeTextValue(Node node) {
    if(node != null && hasOneNode(node.getChildNodes())) {
      return ((Text) node.getChildNodes().item(0)).getData().trim();
    }
    return null;
  }

  /**
   * Returns true if there is only one node in the nodeList.
   */
  private static boolean hasOneNode(NodeList nodeList) {
    return nodeList != null && nodeList.getLength() == 1;
  }

}
