package org.eclipse.m2e.editor.xml;

import java.util.Collections;

import org.w3c.dom.Node;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.ui.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.editor.xml.InsertArtifactProposal.Configuration;
import org.eclipse.m2e.editor.xml.internal.Messages;

public class InsertArtifactProposal implements ICompletionProposal, ICompletionProposalExtension4, ICompletionProposalExtension5 {

  private ISourceViewer sourceViewer;
  private Region region;
  private int generatedLength = 0;
  private int generatedOffset;
  private Configuration config;

  public InsertArtifactProposal(ISourceViewer sourceViewer, Region region, Configuration config) {
    this.sourceViewer = sourceViewer;
    this.region = region;
    generatedOffset = region.getOffset();
    this.config = config;
    assert config.getType() != null;
  }

  public void apply(IDocument document) {
    MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(sourceViewer.getTextWidget().getShell(),
        config.getType().getWindowTitle(), config.getType().getIIndexType(),
        Collections.<ArtifactKey> emptySet(), false);
    if (config.getInitiaSearchString() != null) {
      dialog.setQuery(config.getInitiaSearchString());
    }
    if(dialog.open() == Window.OK) {
      if (config.getType() == SearchType.PARENT) {
        IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
        if(af != null) {
          int offset = region.getOffset();
          try {
            int line = document.getLineOfOffset(offset);
            int initialSpace = offset - document.getLineOffset(line);
            
            StringBuffer buffer = new StringBuffer();
            buffer.append("<parent>").append(document.getLegalLineDelimiters()[0]); //do we care? or just append \n always? //$NON-NLS-1$
            // now append the correct number of spaces or tabs (how to find out what the preference is)
            String spaces = document.get(document.getLineOffset(line), initialSpace);
            if (spaces.trim().length() != 0) {
              //hmm got, non whitespace chars on the line.. purge
              spaces = "\t"; //$NON-NLS-1$
            }
            String ind = spaces.endsWith("\t") ? "\t" : "  "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            
            buffer.append(spaces).append(ind);
            buffer.append("<groupId>").append(af.group).append("</groupId>").append(document.getLegalLineDelimiters()[0]); //$NON-NLS-1$ //$NON-NLS-2$
            buffer.append(spaces).append(ind);
            buffer.append("<artifactId>").append(af.artifact).append("</artifactId>").append(document.getLegalLineDelimiters()[0]); //$NON-NLS-1$ //$NON-NLS-2$
            buffer.append(spaces).append(ind);
            buffer.append("<version>").append(af.version).append("</version>").append(document.getLegalLineDelimiters()[0]); //$NON-NLS-1$ //$NON-NLS-2$
            //check if parent is on workspace, if so, add a relative path right away..
            String relativePath = PomContentAssistProcessor.findRelativePath(sourceViewer, af.group, af.artifact, af.version);
            if (relativePath != null) {
              buffer.append(spaces).append(ind);
              buffer.append("<relativePath>").append(relativePath).append("</relativePath>").append(document.getLegalLineDelimiters()[0]); //$NON-NLS-1$ //$NON-NLS-2$
            }
            buffer.append(spaces);
            buffer.append("</parent>").append(document.getLegalLineDelimiters()[0]); //$NON-NLS-1$
            generatedLength = buffer.toString().length();
            document.replace(offset, region.getLength(), buffer.toString());
          } catch(BadLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }
      if (config.getType() == SearchType.PLUGIN) {
        
      }
    }
  }

  public Point getSelection(IDocument document) {
    return new Point(generatedOffset, generatedLength);
  }

  public String getAdditionalProposalInfo() {
    return null; //not to be used anymore
  }

  public String getDisplayString() {
    return config.getType().getDisplayName(); 
  }

  public Image getImage() {
    return config.getType().getImage();
  }

  public IContextInformation getContextInformation() {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean isAutoInsertable() {
    return false;
  }

  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    return config.getType().getAdditionalInfo();
  }
  
  /**
   * supported search types
   * @author mkleint
   *
   */
  public static enum SearchType {
    
    PARENT(IIndex.SEARCH_PARENTS, Messages.InsertArtifactProposal_searchDialog_title, Messages.InsertArtifactProposal_display_name, MvnImages.IMG_OPEN_POM, Messages.InsertArtifactProposal_additionals), 
    PLUGIN(IIndex.SEARCH_PLUGIN, "Select Plugin", "Insert plugin", MvnImages.IMG_OPEN_POM, "Opens a search dialog where you can select a Maven plugin to add to this project");
    
    private final String type;
    private final String windowTitle;
    private final String displayName;
    private final Image image;
    private final String additionalInfo;
    private SearchType(String type, String windowTitle, String dn, Image img, String addInfo) {
      this.type = type;
      this.windowTitle = windowTitle;
      this.displayName = dn;
      this.image = img;
      this.additionalInfo = addInfo;
    }
    
    String getIIndexType() {
      return type;
    }

    public String getWindowTitle() {
      return windowTitle;
    }

    public String getDisplayName() {
      return displayName;
    }

    public Image getImage() {
      return image;
    }

    public String getAdditionalInfo() {
      return additionalInfo;
    }
    
  }
  
  public static class Configuration {
    private final SearchType type;
    private String initiaSearchString;
    private Node node;
    
    public Configuration(SearchType type) {
      this.type = type;
    }
    
    public void setInitiaSearchString(String initiaSearchString) {
      this.initiaSearchString = initiaSearchString;
    }
    public String getInitiaSearchString() {
      return initiaSearchString;
    }
    public SearchType getType() {
      return type;
    }

    public void setCurrentNode(Node node) {
      this.node = node;
    }

    public Node getCurrentNode() {
      return node;
    }
  }

}
