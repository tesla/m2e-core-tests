/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.progress.UIJob;
import org.maven.ide.components.pom.Build;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.Plugin;
import org.maven.ide.components.pom.PomFactory;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.internal.ExtensionReader;
import org.maven.ide.eclipse.internal.project.CustomizableLifecycleMapping;
import org.maven.ide.eclipse.internal.project.MojoExecutionUtils;
import org.maven.ide.eclipse.project.IMavenProjectChangedListener;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;


public class LifecyclePage extends MavenPomEditorPage implements IMavenProjectChangedListener, IPomFileChangedListener {

  private static final String FORM_NAME = "Lifecycle Mappings";

  private final MavenPomEditor pomEditor;

  private CCombo cmbLifecycleType;
  private TreeViewer executionTreeViewer;
  
  private Map<String, ILifecycleMapping> mappings;
  private String[] lifecycleNames;
  private String[] lifecycleIds;
  
  IMavenProjectFacade projectFacade;
  ILifecycleMapping selectedLifecycleMapping;

  public LifecyclePage(MavenPomEditor pomEditor) {
    super(pomEditor, IMavenConstants.PLUGIN_ID + ".pom.lifecycleMappings", FORM_NAME);
    this.pomEditor = pomEditor;
    
    //Read in the mappings that we can use
    mappings = new HashMap<String, ILifecycleMapping>(ExtensionReader.readLifecycleMappingExtensions());
    lifecycleNames = new String[mappings.size()];
    lifecycleIds = new String[mappings.size()];
    int i = 0;
    for(Map.Entry<String, ILifecycleMapping> mapping : mappings.entrySet()) {
      lifecycleIds[i] = mapping.getKey();
      lifecycleNames[i] = mapping.getValue().getName();
      i++;
    }
  }

  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();
    ScrolledForm form = managedForm.getForm();
    form.setText("Lifecycle Mappings");

    Composite body = form.getBody();
    GridLayout gridLayout = new GridLayout(1, true);
    gridLayout.verticalSpacing = 7;
    body.setLayout(gridLayout);
    toolkit.paintBordersFor(body);
    
    Composite topComposite = toolkit.createComposite(body, SWT.NONE);
    topComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    GridLayout topCompositeLayout = new GridLayout();
    topCompositeLayout.marginWidth = 0;
    topCompositeLayout.marginHeight = 0;
    topComposite.setLayout(topCompositeLayout);
    
    buildTopSection(topComposite, toolkit);
    
    Composite bottomComposite = toolkit.createComposite(body, SWT.NONE);
    bottomComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    GridLayout bottomCompositeLayout = new GridLayout();
    bottomCompositeLayout.marginWidth = 0;
    bottomCompositeLayout.marginHeight = 0;
    bottomComposite.setLayout(bottomCompositeLayout);
    
    buildBottomSection(bottomComposite, toolkit);
    
    toolkit.paintBordersFor(topComposite);
    toolkit.paintBordersFor(bottomComposite);
    
    MavenPlugin.getDefault().getMavenProjectManager().addMavenProjectChangedListener(this);
    
    loadData(false);
    
    super.createFormContent(managedForm);
  }
  
private void buildTopSection(Composite parent, FormToolkit toolkit) {
    
    Section topSection = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
    topSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    topSection.setText("Lifecycle Mapping");
  
    Composite topComposite = toolkit.createComposite(topSection, SWT.NONE);
    toolkit.adapt(topComposite);
    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginBottom = 5;
    gridLayout.marginHeight = 2;
    gridLayout.marginWidth = 1;
    topComposite.setLayout(gridLayout);
    topSection.setClient(topComposite);
  
    cmbLifecycleType = new CCombo(topComposite, SWT.FLAT | SWT.READ_ONLY);
    cmbLifecycleType.setItems(lifecycleNames);
    toolkit.adapt(cmbLifecycleType, true, true);
    
    GridData cmbLayout = new GridData(SWT.FILL, SWT.TOP, true, false);
    cmbLayout.widthHint = 250;
    cmbLifecycleType.setLayoutData(cmbLayout);
    
    cmbLifecycleType.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    cmbLifecycleType.setData("name", "lifecycle");
    toolkit.paintBordersFor(cmbLifecycleType);
    
    cmbLifecycleType.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent arg0) {
        handleLifecycleSelect(cmbLifecycleType.getSelectionIndex());
      }
      public void widgetSelected(SelectionEvent arg0) {
        handleLifecycleSelect(cmbLifecycleType.getSelectionIndex());
      }
    });
    
    toolkit.paintBordersFor(topComposite);
  }

  private void buildBottomSection(Composite parent, FormToolkit toolkit) {
    Section bottomSection = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
    bottomSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    bottomSection.setText("Mojo Executions");
  
    Composite bottomComposite = toolkit.createComposite(bottomSection, SWT.NONE);
    toolkit.adapt(bottomComposite);
    bottomComposite.setLayout(new FillLayout());
    bottomSection.setClient(bottomComposite);

    executionTreeViewer = new TreeViewer(bottomComposite);
    executionTreeViewer.setContentProvider(new LifecyclePageContentProvider());
    executionTreeViewer.setLabelProvider(new LifeCyclePageLabelProvider());
    executionTreeViewer.setInput(null);
    executionTreeViewer.setAutoExpandLevel(2);
    initializeContextMenu();
    toolkit.paintBordersFor(bottomComposite);
   
  }

  private void initializeContextMenu() {
    GoalMenuManager menuManager = new GoalMenuManager("#PopupMenu"); //$NON-NLS-1$
    menuManager.setRemoveAllWhenShown(true);
    
    menuManager.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        final GoalMenuManager gm = (GoalMenuManager)manager;
        if(selectedLifecycleMapping != null && selectedLifecycleMapping instanceof CustomizableLifecycleMapping) {
          if(gm.getGoal() != null && gm.getGoal().enabled) {
            gm.add(new Action() {
              @Override
              public void run() {
                removeGoalFromLifecycleMapping(gm.getGoal().name, gm.getGoal().parent.buildKind);
              }
              
              @Override
              public String getText() {
                return "Disable goal";
              }
            });
          } else {
            if(gm.getGoal() != null && !gm.getGoal().enabled) {
              gm.add(new Action() {
                @Override
                public void run() {
                  addGoalToLifecycleMapping(gm.getGoal().name, gm.getGoal().parent.buildKind);
                }
                
                @Override
                public String getText() {
                  return "Enable goal";
                }     
              });
            }
          }
        }
      }
    });
    TreeViewer viewer = executionTreeViewer;
    viewer.addSelectionChangedListener(menuManager);
    Menu menu = menuManager.createContextMenu(viewer.getTree());
    viewer.getTree().setMenu(menu);
  }

  

  void loadData(final boolean force) {
    try {
      IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
      projectFacade = MavenPlugin.getDefault().getMavenProjectManager().create(pomEditor.getPomFile(), true, new NullProgressMonitor());
      selectedLifecycleMapping = configurationManager.getLifecycleMapping(projectFacade, new NullProgressMonitor());
      int i = Arrays.asList(lifecycleNames).indexOf(selectedLifecycleMapping.getName());
      cmbLifecycleType.select(i);
      executionTreeViewer.setInput(new TreeInput(projectFacade, selectedLifecycleMapping));
    } catch(CoreException e) {
      MavenLogger.log(e);
    }
  }
  
  /**
   * Adds the given goal to the list of custom goals to enable for build mode
   * buildKind.
   * 
   * @param goal
   * @param buildKind
   */
  private void addGoalToLifecycleMapping(String goal, int buildKind) {
    Plugin p = getOrCreateLifecycleMappingPlugin();
    Element configNode = (Element)p.getConfiguration().getConfigurationNode();
    Element mojoExecutions = (Element)p.getConfiguration().getNode("mojoExecutions");
    if(null == mojoExecutions) {
      mojoExecutions = configNode.getOwnerDocument().createElement("mojoExecutions");
      configNode.appendChild(mojoExecutions);
    }
    
    Element existing = findChildElementWithContent("mojoExecution", goal, mojoExecutions);
    
    if(null == existing) {
      existing = configNode.getOwnerDocument().createElement("mojoExecution");
      existing.setAttribute("runOnClean", "false");
      existing.setAttribute("runOnIncremental", "false");
      existing.appendChild(configNode.getOwnerDocument().createTextNode(goal));
      mojoExecutions.appendChild(existing);
    }
    
    if(buildKind == IncrementalProjectBuilder.FULL_BUILD) {
      existing.setAttribute("runOnClean", "true");
    } else {
      existing.setAttribute("runOnIncremental", "true");
    }
    selectedLifecycleMapping = new CustomizableLifecycleMapping(configNode);
    executionTreeViewer.setInput(new TreeInput(projectFacade, selectedLifecycleMapping));
  }
  
  /**
   * Removes the given goal from execution for build mode
   * @param goal
   * @param buildKind
   */
  private void removeGoalFromLifecycleMapping(String goal, int buildKind) {
    Plugin p = getOrCreateLifecycleMappingPlugin();
    Element configNode = (Element)p.getConfiguration().getConfigurationNode();
    Element mojoExecutions = (Element)p.getConfiguration().getNode("mojoExecutions");
    if(null == mojoExecutions) {
      mojoExecutions = configNode.getOwnerDocument().createElement("mojoExecutions");
      configNode.appendChild(mojoExecutions);
    }
    
    Element existing = findChildElementWithContent("mojoExecution", goal, mojoExecutions);
    
    if(null == existing) {
      return; //Nothing to do.
    }
    
    if(buildKind == IncrementalProjectBuilder.FULL_BUILD) {
      if("false".equals(existing.getAttribute("runOnIncremental"))) {
        existing.getParentNode().removeChild(existing);
      } else {
        existing.setAttribute("runOnClean", "false");
      }
    } else {
      if("false".equals(existing.getAttribute("runOnClean"))) {
        existing.getParentNode().removeChild(existing);
      } else {
        existing.setAttribute("runOnIncremental", "false");
      }
    }
    selectedLifecycleMapping = new CustomizableLifecycleMapping(configNode);
    executionTreeViewer.setInput(new TreeInput(projectFacade, selectedLifecycleMapping));
  }
  
  /**
   * Handle changing the XML when the user selects a mapping type.
   * @param i
   */
  private void handleLifecycleSelect(int i) {
    if(i > -1) {
      try {
        String id = lifecycleIds[i];
        Plugin lifecycleMappingPlugin = getOrCreateLifecycleMappingPlugin();
        
        if(CustomizableLifecycleMapping.EXTENSION_ID.equals(id)) {
          createCustomConfiguration(lifecycleMappingPlugin);
        } else {
          Node configNode = lifecycleMappingPlugin.getConfiguration().getConfigurationNode();
          while(configNode.getFirstChild() != null) {
            configNode.removeChild(configNode.getFirstChild());
          }
          lifecycleMappingPlugin.getConfiguration().setStringValue("mappingId", id);
        }
        ILifecycleMapping mapping = mappings.get(id);
        selectedLifecycleMapping = mapping;
        executionTreeViewer.setInput(new TreeInput(projectFacade, selectedLifecycleMapping));
      } catch(DOMException e) {
        MavenLogger.log("Dom error", e);
      } catch(CoreException e) {
        MavenLogger.log(e);
      }
    }
  }

  private Element findChildElementWithContent(String tagName, String content, Element mojoExecutions) {
    Element ret = null;
    Node n = mojoExecutions.getFirstChild();
    while(n != null) {
      if(n instanceof Element && n.getNodeName().equals(tagName)) {
        String value = getNodeContents(n);
        if(content.equals(value)) {
          ret = (Element)n;
          break;
        }
      }
      n = n.getNextSibling();
    }
    return ret;
  }
  
  private String getNodeContents(Node n) {
    if(n instanceof Text) {
      return ((Text)n).getNodeValue();
    } else if(n instanceof Element) {
      StringBuilder value = new StringBuilder();
      Node child = ((Element)n).getFirstChild();
      while(child != null) {
        value.append(getNodeContents(child));
        child = child.getNextSibling();
      }
      return value.toString();
    }
    return "";
  }
  
  

  private void createCustomConfiguration(Plugin lifecycleMappingPlugin) throws CoreException {
    CustomLifecycleParamsDialog dialog = new CustomLifecycleParamsDialog(pomEditor.getSite().getShell(), lifecycleIds, lifecycleNames);
    dialog.setBlockOnOpen(true);
    dialog.open();
    String templateId = dialog.getSelectedTemplate();
    
    Element configNode = (Element)lifecycleMappingPlugin.getConfiguration().getConfigurationNode();
    while(configNode.hasChildNodes()) {
      configNode.removeChild(configNode.getFirstChild());
    }
    lifecycleMappingPlugin.getConfiguration().setStringValue("mappingId", "customizable");
    Element configuratorsElement = configNode.getOwnerDocument().createElement("configurators");
    configNode.appendChild(configuratorsElement);
    
    if(templateId != null) {
      ILifecycleMapping mapping = mappings.get(templateId);
      for(AbstractProjectConfigurator configer : mapping.getProjectConfigurators(projectFacade, new NullProgressMonitor())) {
        if(!configer.isGeneric()) {
          Element configuratorElement = configNode.getOwnerDocument().createElement("configurator");
          configuratorElement.setAttribute("id", configer.getId());
          configuratorsElement.appendChild(configuratorElement);
        }
      }
    }
    
    Element mojoExecutionsElement = configNode.getOwnerDocument().createElement("mojoExecutions");
    configNode.appendChild(mojoExecutionsElement);
    if(templateId != null) {
      ILifecycleMapping mapping = mappings.get(templateId);
      List<String> cleanExecutions = mapping.getPotentialMojoExecutionsForBuildKind(projectFacade, IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
      List<String> incrExecutions = mapping.getPotentialMojoExecutionsForBuildKind(projectFacade, IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
      Set<String> uniqExecutions = new LinkedHashSet<String>();
      uniqExecutions.addAll(cleanExecutions);
      uniqExecutions.addAll(incrExecutions);
      for(String execution : uniqExecutions) {
        Element mojoExecutionElement = configNode.getOwnerDocument().createElement("mojoExecution");
        if(cleanExecutions.contains(execution)) {
          mojoExecutionElement.setAttribute("runOnClean", "true");
        } else {
          mojoExecutionElement.setAttribute("runOnClean", "false");
        }
        if(incrExecutions.contains(execution)) {
          mojoExecutionElement.setAttribute("runOnIncremental", "true");
        } else {
          mojoExecutionElement.setAttribute("runOnIncremental", "false");
        }
        Text t = configNode.getOwnerDocument().createTextNode(execution);
        mojoExecutionElement.appendChild(t);
        mojoExecutionsElement.appendChild(mojoExecutionElement);
      }
    }
    mappings.put(CustomizableLifecycleMapping.EXTENSION_ID, new CustomizableLifecycleMapping(configNode));
  }

  private Plugin getOrCreateLifecycleMappingPlugin() {
    Model m = getModel();
    Build build = m.getBuild();
    if(null == build) {
      build = PomFactory.eINSTANCE.createBuild();
      m.setBuild(build);
    }
    
    EList<Plugin> plugins = build.getPlugins();
    Plugin lifecycleMappingPlugin = null;
    for(Plugin plugin : plugins) {
      if("org.maven.ide.eclipse".equals(plugin.getGroupId()) && "lifecycle-mapping".equals(plugin.getArtifactId())) {
        lifecycleMappingPlugin = plugin;
        break;
      }
    }
    
    if(null == lifecycleMappingPlugin) {
      lifecycleMappingPlugin = PomFactory.eINSTANCE.createPlugin();
      lifecycleMappingPlugin.setGroupId("org.maven.ide.eclipse");
      lifecycleMappingPlugin.setArtifactId("lifecycle-mapping");
      lifecycleMappingPlugin.setVersion("0.9.9-SNAPSHOT");
      plugins.add(lifecycleMappingPlugin);
    }
    
    if(null == lifecycleMappingPlugin.getConfiguration()) {
      lifecycleMappingPlugin.setConfiguration(PomFactory.eINSTANCE.createConfiguration());
    }
    return lifecycleMappingPlugin;
  }

  @Override
  public void dispose() {
    MavenPlugin.getDefault().getMavenProjectManager().removeMavenProjectChangedListener(this);

    super.dispose();
  }

  public void loadData() {
    loadData(true);
  }
  
  @Override
  public void updateView(Notification notification) {
    // TODO Auto-generated method stub
    
  } 

  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    if(getManagedForm() == null || getManagedForm().getForm() == null)
      return;

    for(int i = 0; i < events.length; i++ ) {
      if(events[i].getSource().equals(((MavenPomEditor) getEditor()).getPomFile())) {
        // file has been changed. need to update graph  
        new UIJob("Reloading") {
          public IStatus runInUIThread(IProgressMonitor monitor) {
            loadData();
            FormUtils.setMessage(getManagedForm().getForm(), null, IMessageProvider.WARNING);
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    }
  }

  public void fileChanged() {
    if(getManagedForm() == null || getManagedForm().getForm() == null)
      return;

    new UIJob("Reloading") {
      public IStatus runInUIThread(IProgressMonitor monitor) {
        FormUtils.setMessage(getManagedForm().getForm(), "Updating lifecycle mappings...", IMessageProvider.WARNING);
        return Status.OK_STATUS;
      }
    }.schedule();
  }
  
  private static class LifecyclePageContentProvider implements ITreeContentProvider {
    private static final Object[] EMPTY_ARY = new Object[0];
    private static final Object[] ROOT_NODES = new Object[] { new BuildKindTreeNode(IncrementalProjectBuilder.FULL_BUILD, "Full Build"), new BuildKindTreeNode(IncrementalProjectBuilder.INCREMENTAL_BUILD, "Incremental Build") };
    private ILifecycleMapping lifecycleMapping;
    private IMavenProjectFacade projectFacade;
    
    public Object[] getChildren(Object parent) {
      try {
        if(parent instanceof BuildKindTreeNode) {
          BuildKindTreeNode tn = (BuildKindTreeNode)parent;
          if(lifecycleMapping != null && projectFacade != null) {
            List<String> enabled =  lifecycleMapping.getPotentialMojoExecutionsForBuildKind(projectFacade, tn.buildKind, new NullProgressMonitor());
            MavenExecutionPlan executionPlan = projectFacade.getExecutionPlan(new NullProgressMonitor());
            ArrayList<MojoExecutionTreeNode> ret = new ArrayList<MojoExecutionTreeNode>(executionPlan.getExecutions().size());
            for(MojoExecution execution : executionPlan.getExecutions()) {
              String display = MojoExecutionUtils.getExecutionKey(execution);
              boolean e = false;
              if(enabled.contains(display)) {
                e = true;
              }
              ret.add(new MojoExecutionTreeNode(display, tn, e));
            }
            return ret.toArray();
          }
        }
      } catch(CoreException e) {
        MavenLogger.log(e);
      }
      return EMPTY_ARY;
    }
    
    public void dispose() {
      
    }
    public Object[] getElements(Object parent) {
      return ROOT_NODES;
    }
    
    public Object getParent(Object obj) {
      if(obj instanceof MojoExecutionTreeNode) {
        return ((MojoExecutionTreeNode)obj).parent;
      }
      
      return null;
    }
      
    public boolean hasChildren(Object obj) {
      return (obj instanceof BuildKindTreeNode);
    }
    
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      if(newInput != null) {
        lifecycleMapping = ((TreeInput)newInput).lifecycleMapping;
        projectFacade = ((TreeInput)newInput).projectFacade;
      } else {
        lifecycleMapping = null;
        projectFacade = null;
      }
      //viewer.refresh();
      viewer.setSelection(null);
      //((TreeViewer)viewer).expandAll();
      
    }
  }
  
  private static class TreeInput {
    final IMavenProjectFacade projectFacade;
    final ILifecycleMapping lifecycleMapping;
    public TreeInput(IMavenProjectFacade projectFacade, ILifecycleMapping lifecycleMapping) {
      super();
      this.projectFacade = projectFacade;
      this.lifecycleMapping = lifecycleMapping;
    }
    
  }
  private static class BuildKindTreeNode {
    final int buildKind;
    final String name;
    public BuildKindTreeNode(int buildKind, String name) {
      super();
      this.buildKind = buildKind;
      this.name = name;
    }
    
  }
  
  private static class MojoExecutionTreeNode {
    final String name;
    final BuildKindTreeNode parent;
    final boolean enabled;
    public MojoExecutionTreeNode(String name, BuildKindTreeNode parent, boolean enabled) {
      super();
      this.name = name;
      this.parent = parent;
      this.enabled = enabled;
    }
  }
  
  private static class LifeCyclePageLabelProvider extends LabelProvider implements IColorProvider {
    @Override
    public String getText(Object element) {
      if(element instanceof BuildKindTreeNode) {
        return ((BuildKindTreeNode)element).name;
      } else if(element instanceof MojoExecutionTreeNode) {
        return ((MojoExecutionTreeNode)element).name;
      }
      return null;
    }
    
    public Color getBackground(Object arg0) {
     return null;
    }
    
    public Color getForeground(Object element) {
      if(element instanceof MojoExecutionTreeNode) {
        MojoExecutionTreeNode tn = (MojoExecutionTreeNode)element;
        return tn.enabled ? null : Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
      }
      return null;
    }
  }
  
  private static class GoalMenuManager extends MenuManager implements ISelectionChangedListener {
    private MojoExecutionTreeNode goal;
    
    public void selectionChanged(SelectionChangedEvent event) {
      ISelection selection = event.getSelection();
      if(selection instanceof ITreeSelection) {
        ITreeSelection ts = (ITreeSelection)selection;
        Object o = ts.getFirstElement();
        if(o instanceof MojoExecutionTreeNode) {
          goal = (MojoExecutionTreeNode)o;
        } else {
          goal = null;
        }
      } else {
        goal = null;
      }
    }
    
    public GoalMenuManager() {
      super();
    }

    public GoalMenuManager(String text, ImageDescriptor image, String id) {
      super(text, id);
    }

    public GoalMenuManager(String text, String id) {
      super(text, id);
    }

    public GoalMenuManager(String text) {
      super(text);
    }

    public MojoExecutionTreeNode getGoal() {
      return goal;
    }
  }
}
