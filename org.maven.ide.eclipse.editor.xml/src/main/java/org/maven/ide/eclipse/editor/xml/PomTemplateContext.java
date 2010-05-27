/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.util.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.editor.xml.search.ArtifactInfo;
import org.maven.ide.eclipse.editor.xml.search.Packaging;
import org.maven.ide.eclipse.editor.xml.search.SearchEngine;


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
  
  MODULES("modules"), //

  PROPERTIES("properties"), //
  
  DEPENDENCIES("dependencies"), //
  
  EXCLUSIONS("exclusions"), //
  
  PLUGINS("plugins"), //

  PLUGIN("plugin"), //

  EXECUTIONS("executions"), //
  
  PROFILES("profiles"), //
  
  REPOSITORIES("repositories"), //

  CONFIGURATION("configuration") {

    @Override
    protected void addTemplates(IProject project, Collection<Template> proposals, Node node, String prefix) throws CoreException {
      if("execution".equals(node.getParentNode().getNodeName())
          || "reportSet".equals(node.getParentNode().getNodeName())) {
        node = node.getParentNode().getParentNode();
      }
      System.out.println("prefix=" + prefix);
      String groupId = getGroupId(node);
      if(groupId==null) {
        groupId = "org.apache.maven.plugins";  // TODO support other default groups
      }
      String artifactId = getArtifactId(node);
      String version = getVersion(node);
      if(version==null) {
        Collection<String> versions = getSearchEngine(project).findVersions(groupId, artifactId, "", Packaging.PLUGIN);
        if(versions.isEmpty()) {
          return;
        }
        version = versions.iterator().next();
      }
      
      PluginDescriptor descriptor = PomTemplateContextUtil.INSTANCE.getPluginDescriptor(groupId, artifactId, version);
      if(descriptor!=null) {
        List<MojoDescriptor> mojos = descriptor.getMojos();
        HashSet<String> params = new HashSet<String>();
        for(MojoDescriptor mojo : mojos) {
          List<Parameter> parameters = (List<Parameter>) mojo.getParameters();
          for(Parameter parameter : parameters) {
            boolean editable = parameter.isEditable();
            if(editable) {
              String name = parameter.getName();
              if(!params.contains(name) && name.startsWith(prefix)) {
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
  },
  
  GROUP_ID("groupId") {
    @Override
    public void addTemplates(IProject project, Collection<Template> proposals, Node node, String prefix) throws CoreException {
      String contextTypeId = getContextTypeId();
      for(String groupId : getSearchEngine(project).findGroupIds(prefix, getPackaging(node), getContainingArtifact(node))) {
        add(proposals, contextTypeId, groupId);
      }
    }
  },

  ARTIFACT_ID("artifactId") {
    @Override
    public void addTemplates(IProject project, Collection<Template> proposals, Node node, String prefix) throws CoreException {
      String groupId = getGroupId(node);
      if(groupId != null) {
        String contextTypeId = getContextTypeId();
        for(String artifactId : getSearchEngine(project).findArtifactIds(groupId, prefix, getPackaging(node),
            getContainingArtifact(node))) {
          add(proposals, contextTypeId, artifactId, groupId + ":" + artifactId);
        }
      }
    }
  },

  VERSION("version") {
    @Override
    public void addTemplates(IProject project, Collection<Template> proposals, Node node, String prefix) throws CoreException {
      String groupId = getGroupId(node);
      String artifactId = getArtifactId(node);
      if(groupId != null && artifactId != null) {
        String contextTypeId = getContextTypeId();
        for(String version : getSearchEngine(project).findVersions(groupId, artifactId, prefix, getPackaging(node))) {
          add(proposals, contextTypeId, version, groupId + ":" + artifactId + ":" + version);
        }
      }
    }
  },

  CLASSIFIER("classifier") {
    @Override
    public void addTemplates(IProject project, Collection<Template> proposals, Node node, String prefix) throws CoreException {
      String groupId = getGroupId(node);
      String artifactId = getArtifactId(node);
      String version = getVersion(node);
      if(groupId != null && artifactId != null && version != null) {
        String contextTypeId = getContextTypeId();
        for(String classifier : getSearchEngine(project).findClassifiers(groupId, artifactId, version, prefix,
            getPackaging(node))) {
          add(proposals, contextTypeId, classifier, groupId + ":" + artifactId + ":" + version + ":" + classifier);
        }
      }
    }
  },

  TYPE("type") {
    @Override
    public void addTemplates(IProject project, Collection<Template> proposals, Node node, String prefix) throws CoreException {
      String groupId = getGroupId(node);
      String artifactId = getArtifactId(node);
      String version = getVersion(node);
      String contextTypeId = getContextTypeId();
      if(groupId != null && artifactId != null && version != null) {
        for(String type : getSearchEngine(project).findTypes(groupId, artifactId, version, prefix, getPackaging(node))) {
          add(proposals, contextTypeId, type, groupId + ":" + artifactId + ":" + version + ":" + type);
        }
      }
    }
  },
  
  PACKAGING("packaging") {
    @Override
    public void addTemplates(IProject project, Collection<Template> proposals, Node node, String prefix) {
      String contextTypeId = getContextTypeId();
      // TODO only show "pom" packaging in root section
      add(proposals, contextTypeId, "pom");
      add(proposals, contextTypeId, "jar");
      add(proposals, contextTypeId, "war");
      add(proposals, contextTypeId, "ear");
      add(proposals, contextTypeId, "ejb");
      add(proposals, contextTypeId, "eclipse-plugin");
      add(proposals, contextTypeId, "eclipse-feature");
      add(proposals, contextTypeId, "eclipse-update-site");
      add(proposals, contextTypeId, "maven-plugin");
      add(proposals, contextTypeId, "maven-archetype");
    }
  },
  
  SCOPE("scope") {
    @Override
    public void addTemplates(IProject project, Collection<Template> proposals, Node node, String prefix) {
      String contextTypeId = getContextTypeId();
      add(proposals, contextTypeId, "compile");
      add(proposals, contextTypeId, "test");
      add(proposals, contextTypeId, "provided");
      add(proposals, contextTypeId, "runtime");
      add(proposals, contextTypeId, "system");
      // TODO only show "import" scope in <dependencyManagement>
      add(proposals, contextTypeId, "import");
    }    
  },
  
  SYSTEM_PATH("systemPath"),
  
  PHASE("phase") {
    @Override
    public void addTemplates(IProject project, Collection<Template> proposals, Node node, String prefix) {
      String contextTypeId = getContextTypeId();
      // TODO the following list should be derived from the packaging handler (the actual lifecycle)
      
      // Clean Lifecycle
      add(proposals, contextTypeId, "pre-clean", "Executes processes needed prior to the actual project cleaning");
      add(proposals, contextTypeId, "clean", "Removes all files generated by the previous build");
      add(proposals, contextTypeId, "post-clean", "Executes processes needed to finalize the project cleaning");
      
      // Default Lifecycle
      add(proposals, contextTypeId, "validate", "Validate the project is correct and all necessary information is available");
      add(proposals, contextTypeId, "generate-sources", "Generate any source code for inclusion in compilation");
      add(proposals, contextTypeId, "process-sources", "Process the source code, for example to filter any values");
      add(proposals, contextTypeId, "generate-resources", "Generate resources for inclusion in the package");
      add(proposals, contextTypeId, "process-resources", "Copy and process the resources into the destination directory, ready for packaging");
      add(proposals, contextTypeId, "compile", "Compile the source code of the project");
      add(proposals, contextTypeId, "process-classes", "Post-process the generated files from compilation, for example to do bytecode enhancement on Java classes");
      add(proposals, contextTypeId, "generate-test-sources", "Generate any test source code for inclusion in compilation");
      add(proposals, contextTypeId, "process-test-sources", "Process the test source code, for example to filter any values");
      add(proposals, contextTypeId, "generate-test-resources", "Create resources for testing");
      add(proposals, contextTypeId, "process-test-resources", "Copy and process the resources into the test destination directory");
      add(proposals, contextTypeId, "test-compile", "Compile the test source code into the test destination directory");
      add(proposals, contextTypeId, "process-test-classes", "Post-process the generated files from test compilation, for example to do bytecode enhancement on Java classes. For Maven 2.0.5 and above");
      add(proposals, contextTypeId, "test", "Run tests using a suitable unit testing framework. These tests should not require the code be packaged or deployed");
      add(proposals, contextTypeId, "prepare-package", "Perform any operations necessary to prepare a package before the actual packaging. This often results in an unpacked, processed version of the package. (Maven 2.1 and above)");
      add(proposals, contextTypeId, "package", "Take the compiled code and package it in its distributable format, such as a JAR");
      add(proposals, contextTypeId, "pre-integration-test", "Perform actions required before integration tests are executed. This may involve things such as setting up the required environment");
      add(proposals, contextTypeId, "integration-test", "Process and deploy the package if necessary into an environment where integration tests can be run");
      add(proposals, contextTypeId, "post-integration-test", "Perform actions required after integration tests have been executed. This may including cleaning up the environment");
      add(proposals, contextTypeId, "verify", "Run any checks to verify the package is valid and meets quality criteria");
      add(proposals, contextTypeId, "install", "Install the package into the local repository, for use as a dependency in other projects locally");
      add(proposals, contextTypeId, "deploy", "Done in an integration or release environment, copies the final package to the remote repository for sharing with other developers and projects");
      
      // Site Lifecycle
      add(proposals, contextTypeId, "pre-site", "Executes processes needed prior to the actual project site generation");
      add(proposals, contextTypeId, "site", "Generates the project's site documentation");
      add(proposals, contextTypeId, "post-site", "Executes processes needed to finalize the site generation, and to prepare for site deployment");
      add(proposals, contextTypeId, "site-deploy", "Deploys the generated site documentation to the specified web server");
    }
  },

  GOAL("goal") {
    @Override
    public void addTemplates(IProject project, Collection<Template> proposals, Node node, String prefix)  throws CoreException {
      if(!"goals".equals(node.getParentNode().getNodeName())) {
        return;
      }
      node = node.getParentNode();
      if(!"execution".equals(node.getParentNode().getNodeName())) {
        return;
      }
      node = node.getParentNode();
      if(!"executions".equals(node.getParentNode().getNodeName())) {
        return;
      }
      node = node.getParentNode();

      String groupId = getGroupId(node);
      if(groupId==null) {
        groupId = "org.apache.maven.plugins";
      }
      String artifactId = getArtifactId(node);
      String version = getVersion(node);
      if(version==null) {
        Collection<String> versions = getSearchEngine(project).findVersions(groupId, artifactId, "", Packaging.PLUGIN);
        if(versions.isEmpty()) {
          return;
        }
        version = versions.iterator().next();
      }
      
      PluginDescriptor descriptor = PomTemplateContextUtil.INSTANCE.getPluginDescriptor(groupId, artifactId, version);
      if (descriptor != null) {
        List<MojoDescriptor> mojos = descriptor.getMojos();
        if (mojos != null) {
          String contextTypeId = getContextTypeId();
          for (MojoDescriptor mojo : mojos) {
            add(proposals, contextTypeId, mojo.getGoal(), mojo.getDescription());
          }
        }
      }
    }
  },

  MODULE("module") {
    @Override
    public void addTemplates(IProject project, Collection<Template> proposals, Node node, String prefix)
        throws CoreException {
      if(project == null) {
        //shall not happen just double check.
        return;
      }
      File currentPom = new File(project.getLocationURI());
      File directory = currentPom;
      String path = prefix;
      boolean endingSlash = path.endsWith("/");
      String[] elems = StringUtils.split(path, "/");
      String lastElement = null;
      for(int i = 0; i < elems.length; i++ ) {
        if("..".equals(elems[i])) {
          directory = directory != null ? directory.getParentFile() : null;
        } else if(i < elems.length - (endingSlash ? 0 : 1)) {
          directory = directory != null ? new File(directory, elems[i]) : null;
        } else {
          lastElement = elems[i];
        }
      }
      path = lastElement != null ? path.substring(0, path.length() - lastElement.length()) : path;
      if (directory != null && directory.exists() && directory.isDirectory()) {
        File[] offerings = directory.listFiles(new FileFilter() {
          public boolean accept(File pathname) {
            if (pathname.isDirectory()) {
              File pom = new File(pathname, "pom.xml");
              //TODO shall also handle polyglot maven :)
              return pom.exists() && pom.isFile();
            }
            return false;
          }
        });
        for (File candidate : offerings) {
          if(lastElement == null || candidate.getName().startsWith(lastElement)) {
            add(proposals, getContextTypeId(), path + candidate.getName(), "Maven project at " + candidate);
          }
        }
      }
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
  public Template[] getTemplates(IProject project, Node node, String prefix) {
    Collection<Template> templates = new ArrayList<Template>();
    try {
      addTemplates(project, templates, node, prefix);
    } catch (CoreException e) {
      MavenLogger.log(e);
    }
    
    TemplateStore store = MvnIndexPlugin.getDefault().getTemplateStore();
    if(store != null) {
      templates.addAll(Arrays.asList(store.getTemplates(getContextTypeId())));
    }
    
    return templates.toArray(new Template[templates.size()]);
  }
  
  protected void addTemplates(IProject project, Collection<Template> templates, Node currentNode, String prefix) throws CoreException {
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

  private static SearchEngine getSearchEngine(IProject project) throws CoreException {
    return MvnIndexPlugin.getDefault().getSearchEngine(project);
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

  private static void add(Collection<Template> proposals, String contextTypeId, String name) {
    add(proposals, contextTypeId, name, name);
  }    
  
  private static void add(Collection<Template> proposals, String contextTypeId, String name, String description) {
    proposals.add(new Template(name, description, contextTypeId, name, false));
  }    
  
}
