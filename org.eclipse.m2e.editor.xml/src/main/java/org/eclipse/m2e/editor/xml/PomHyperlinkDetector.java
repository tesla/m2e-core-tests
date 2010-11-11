/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.FileEditorInputFactory;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.text.JobSafeStructuredDocument;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.actions.OpenPomAction;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.editor.xml.internal.Messages;


/**
 * @author Eugene Kuleshov
 */
class PomHyperlinkDetector implements IHyperlinkDetector {

  private final String[] versioned = new String[] {
      "dependency>", //$NON-NLS-1$
      "parent>", //$NON-NLS-1$
      "plugin>", //$NON-NLS-1$
      "reportPlugin>", //$NON-NLS-1$
      "extension>" //$NON-NLS-1$
  };
  public IHyperlink[] detectHyperlinks(ITextViewer textViewer, final IRegion region, boolean canShowMultipleHyperlinks) {
    if(region == null || textViewer == null) {
      return null;
    }
    
    IDocument document = textViewer.getDocument();
    if(document == null) {
      return null;
    }

    IRegion lineInfo;
    String line;
    try {
      lineInfo = document.getLineInformationOfOffset(region.getOffset());
      line = document.get(lineInfo.getOffset(), lineInfo.getLength());
    } catch(BadLocationException ex) {
      return null;
    }

    if(line.length() == 0) {
      return null;
    }
    List<IHyperlink> hyperlinks = new ArrayList<IHyperlink>();
    final int offset = region.getOffset();
    final String text = document.get();
    Node current = getCurrentNode(document, offset);
    //check if we have a property expression at cursor
    if (current != null && current instanceof Text) {
      Text textNode = (Text)current;
      String value = textNode.getNodeValue();
      if (value != null) {
        IHyperlink link = openPropertyDefinition(value, textNode, textViewer, offset);
        if (link != null) {
          hyperlinks.add(link);
        }
      }
    }
    
    //first check all elements that have id (groupId+artifactId+version) combo
    Fragment fragment = null;
    //TODO rewrite to use Nodes
    for (String el : versioned) {
      fragment = getFragment(text, offset, "<" + el, "</" + el); //$NON-NLS-1$ //$NON-NLS-2$
      if (fragment != null) break;
    }
    
    if (fragment != null) {
      IHyperlink link = openPOMbyID(fragment, textViewer);
      if (link != null) {
        hyperlinks.add(link);
      }
    }
    //check if <module> text is selected.
    //TODO rewrite to use Nodes
    fragment = getFragment(text, offset, "<module>", "</module>"); //$NON-NLS-1$ //$NON-NLS-2$
    if (fragment != null) {
      IHyperlink link = openModule(fragment, textViewer);
      if (link != null) {
        hyperlinks.add(link);
      }
    }
    if (hyperlinks.size() > 0) {
      return hyperlinks.toArray(new IHyperlink[0]);
    }
    return null;
  }

  private IHyperlink openPropertyDefinition(String value, Text node, ITextViewer viewer, int offset) {
    assert node instanceof IndexedRegion;
    IndexedRegion reg = (IndexedRegion)node;
    int index = offset - reg.getStartOffset();
    String before = value.substring(0, Math.min (index + 1, value.length()));
    String after = value.substring(Math.min (index + 1, value.length()));
    int start = before.lastIndexOf("${"); //$NON-NLS-1$
    int end = after.indexOf("}"); //$NON-NLS-1$
    if (start > -1 && end > -1) {
      final int startOffset = reg.getStartOffset() + start;
      final String expr = before.substring(start) + after.substring(0, end + 1);
      final int length = expr.length();
      final String prop = before.substring(start + 2) + after.substring(0, end);
      if (prop.startsWith("project.") || prop.startsWith("pom.")) { //$NON-NLS-1$ //$NON-NLS-2$
        return null; //ignore these, not in properties section.
      }
      final IProject prj = PomContentAssistProcessor.extractProject(viewer);
      //TODO we shall rely on presence of a cached model, not project alone..
      if (prj != null) {
        return new IHyperlink() {
          public IRegion getHyperlinkRegion() {
            return new Region(startOffset, length);
          }

          public String getHyperlinkText() {
            return NLS.bind(Messages.PomHyperlinkDetector_open_property, prop);
          }

          public String getTypeLabel() {
            return "pom-property-expression"; //$NON-NLS-1$
          }

          public void open() {
            //see if we can find the plugin in plugin management of resolved project.
            IMavenProjectFacade mvnproject = MavenPlugin.getDefault().getMavenProjectManager().getProject(prj);
            if(mvnproject != null) {
              MavenProject mavprj = mvnproject.getMavenProject();
              if(mavprj != null) {
                Model mdl = mavprj.getModel();
                if (mdl.getProperties().containsKey(prop)) {
                  InputLocation location = mdl.getLocation( "properties" ).getLocation( prop ); //$NON-NLS-1$
                  if (location != null) {
                    String loc = location.getSource().getLocation();
                    File file = new File(loc);
                    IFileStore fileStore = EFS.getLocalFileSystem().getStore(file.toURI());
                    openXmlEditor(fileStore);
                  }
                }
              }
            }
          }
        };
        
      }
    }
    return null;
  }

  private IHyperlink openModule(Fragment fragment, ITextViewer textViewer) {
    final Fragment module = getValue(fragment, "<module>", "</module>"); //$NON-NLS-1$ //$NON-NLS-2$

    ITextFileBuffer buf = FileBuffers.getTextFileBufferManager().getTextFileBuffer(textViewer.getDocument());
    IFileStore folder = buf.getFileStore().getParent();

    String path = module.text;
    //construct IPath for the child pom file, handle relative paths..
    while(folder != null && path.startsWith("../")) { //NOI18N //$NON-NLS-1$
      folder = folder.getParent();
      path = path.substring("../".length());//NOI18N //$NON-NLS-1$
    }
    if(folder == null) {
      return null;
    }
    IFileStore modulePom = folder.getChild(path);
    if(!modulePom.getName().endsWith("xml")) {//NOI18N //$NON-NLS-1$
      modulePom = modulePom.getChild("pom.xml");//NOI18N //$NON-NLS-1$
    }
    final IFileStore fileStore = modulePom;
    if (!fileStore.fetchInfo().exists()) {
      return null;
    }

    IHyperlink pomHyperlink = new IHyperlink() {
      public IRegion getHyperlinkRegion() {
        return new Region(module.offset, module.length);
      }

      public String getHyperlinkText() {
        return NLS.bind(Messages.PomHyperlinkDetector_open_module, module.text);
      }

      public String getTypeLabel() {
        return "pom-module"; //$NON-NLS-1$
      }

      public void open() {
        openXmlEditor(fileStore);
      }
    };

    return pomHyperlink;

  }

  private IHyperlink openPOMbyID(Fragment fragment, final ITextViewer viewer) {
    final Fragment groupId = getValue(fragment, "<groupId>", "</groupId>"); //$NON-NLS-1$ //$NON-NLS-2$
    final Fragment artifactId = getValue(fragment, "<artifactId>", Messages.PomHyperlinkDetector_23); //$NON-NLS-1$
    final Fragment version = getValue(fragment, "<version>", "</version>"); //$NON-NLS-1$ //$NON-NLS-2$
    final IProject prj = PomContentAssistProcessor.extractProject(viewer);
    
    IHyperlink pomHyperlink = new IHyperlink() {
      public IRegion getHyperlinkRegion() {
        //the goal here is to have the groupid/artifactid/version combo underscored by the link.
        //that will prevent underscoring big portions (like plugin config) underscored and
        // will also handle cases like dependencies within plugins.
        int max = groupId != null ? groupId.offset + groupId.length : Integer.MIN_VALUE;
        int min = groupId != null ? groupId.offset : Integer.MAX_VALUE;
        max = Math.max(max, artifactId != null ? artifactId.offset + artifactId.length : Integer.MIN_VALUE);
        min = Math.min(min, artifactId != null ? artifactId.offset : Integer.MAX_VALUE);
        max = Math.max(max, version != null ? version.offset + version.length : Integer.MIN_VALUE);
        min = Math.min(min, version != null ? version.offset : Integer.MAX_VALUE);
        return new Region(min, max - min);
      }

      public String getHyperlinkText() {
        return NLS.bind(Messages.PomHyperlinkDetector_hyperlink_pattern, new Object[] {groupId, artifactId, version});
      }

      public String getTypeLabel() {
        return "pom"; //$NON-NLS-1$
      }

      public void open() {
        new Job(Messages.PomHyperlinkDetector_job_name) {
          protected IStatus run(IProgressMonitor monitor) {
            // TODO resolve groupId if groupId==null
            String gridString = groupId == null ? "org.apache.maven.plugins" : groupId.text; //$NON-NLS-1$      
            String artidString = artifactId == null ? null : artifactId.text;
            String versionString = version == null ? null : version.text;
            if (prj != null && gridString != null && artidString != null && (version == null || version.text.contains("${"))) { //$NON-NLS-1$
              try {
                //TODO how do we decide here if the hyperlink is a dependency or a plugin
                // hyperlink??
                versionString = PomTemplateContext.extractVersion(prj, versionString, gridString, artidString, PomTemplateContext.EXTRACT_STRATEGY_DEPENDENCY);
                
              } catch(CoreException e) {
                versionString = null;
              }
            }
            if (versionString == null) {
              return Status.OK_STATUS;
            }
            OpenPomAction.openEditor(gridString,  
                                     artidString, 
                                     versionString, monitor);
            return Status.OK_STATUS;
          }
        }.schedule();
      }

    };

    return pomHyperlink;
  }

  /**
   * fragment offset returned contains the xml elements 
   * while the text only includes the element text value
   */
  private Fragment getValue(Fragment section, String startTag, String endTag) {
    int start = section.text.indexOf(startTag);
    if(start == -1) {
      return null;
    }
    int end = section.text.indexOf(endTag);
    if(end == -1) {
      return null;
    }

    return new Fragment(section.text.substring(start + startTag.length(), end).trim(), section.offset + start, end + endTag.length() - start);
  }

  /**
   * returns the text, offset and length of the xml element. text includes the xml tags. 
   */
  private Fragment getFragment(String text, int offset, String startTag, String endTag) {
    int start = text.substring(0, offset).lastIndexOf(startTag);
    if(start == -1) {
      return null;
    }

    int end = text.indexOf(endTag, start);
    if(end == -1 || end <= offset) {
      return null;
    }
    end = end + endTag.length();
    return new Fragment(text.substring(start, end), start, end - start);
  }
  
  private static class Fragment {
    final int length;
    final int offset;
    final String text;
    
    Fragment(String text, int start, int len) {
      this.text = text;
      this.offset = start;
      
      this.length = len;
      
    }

    @Override
    public String toString() {
      return text;
    }
  }
  
  
  /**
   * copied from org.eclipse.wst.xml.ui.internal.hyperlink.XMLHyperlinkDetector
   * Returns the node the cursor is currently on in the document. null if no
   * node is selected
   * 
   * returned value is also an instance of IndexedRegion
   * 
   * @param offset
   * @return Node either element, doctype, text, or null
   */
  private Node getCurrentNode(IDocument document, int offset) {
    // get the current node at the offset (returns either: element,
    // doctype, text)
    IndexedRegion inode = null;
    IStructuredModel sModel = null;
    try {
      sModel = StructuredModelManager.getModelManager().getExistingModelForRead(document);
      if (sModel != null) {
        inode = sModel.getIndexedRegion(offset);
        if (inode == null) {
          inode = sModel.getIndexedRegion(offset - 1);
        }
      }
    }
    finally {
      if (sModel != null) {
        sModel.releaseFromRead();
      }
    }

    if (inode instanceof Node) {
      return (Node) inode;
    }
    return null;
  }

  private void openXmlEditor(final IFileStore fileStore) {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if(window != null) {
      IWorkbenchPage page = window.getActivePage();
      if(page != null) {
        try {
          IEditorPart part = IDE.openEditorOnFileStore(page, fileStore);
          if(part instanceof FormEditor) {
            FormEditor ed = (FormEditor) part;
            ed.setActivePage(null); //null means source, always or just in the case of MavenPomEditor?
          }
        } catch(PartInitException e) {
          MessageDialog.openInformation(Display.getDefault().getActiveShell(), //
              Messages.PomHyperlinkDetector_error_title, NLS.bind(Messages.PomHyperlinkDetector_error_message, fileStore, e.toString()));

        }
      }
    }
  }  

}
