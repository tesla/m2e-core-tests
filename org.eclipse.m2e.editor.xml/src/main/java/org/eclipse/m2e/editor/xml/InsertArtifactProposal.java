package org.eclipse.m2e.editor.xml;

import java.util.Collections;

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

public class InsertArtifactProposal implements ICompletionProposal, ICompletionProposalExtension4, ICompletionProposalExtension5 {

  private ISourceViewer sourceViewer;
  private Region region;
  private int generatedLength = 0;
  private String group;

  public InsertArtifactProposal(ISourceViewer sourceViewer, Region region, String groupString) {
    this.sourceViewer = sourceViewer;
    this.region = region;
    this.group = groupString;
  }

  public void apply(IDocument document) {
    MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(sourceViewer.getTextWidget().getShell(), //
        "Select Parent", IIndex.SEARCH_PARENTS,
        Collections.<ArtifactKey> emptySet(), false);
    if (group != null) {
      dialog.setQuery(group);
    }
    if(dialog.open() == Window.OK) {
      IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
      if(af != null) {
        int offset = region.getOffset();
        try {
          int line = document.getLineOfOffset(offset);
          int initialSpace = offset - document.getLineOffset(line);
          
          StringBuffer buffer = new StringBuffer();
          buffer.append("<parent>").append(document.getLegalLineDelimiters()[0]); //do we care? or just append \n always?
          
          // now append the correct number of spaces or tabs (how to find out what the preference is)
          String spaces = document.get(document.getLineOffset(line), initialSpace);
          if (spaces.trim().length() != 0) {
            //hmm got, non whitespace chars on the line.. purge
            spaces = "\t";
          }
          String ind = spaces.endsWith("\t") ? "\t" : "  ";
          
          buffer.append(spaces).append(ind);
          buffer.append("<groupId>").append(af.group).append("</groupId>").append(document.getLegalLineDelimiters()[0]);
          buffer.append(spaces).append(ind);
          buffer.append("<artifactId>").append(af.artifact).append("</artifactId>").append(document.getLegalLineDelimiters()[0]);
          buffer.append(spaces).append(ind);
          buffer.append("<version>").append(af.version).append("</version>").append(document.getLegalLineDelimiters()[0]);
          buffer.append(spaces);
          buffer.append("</parent>").append(document.getLegalLineDelimiters()[0]);
          generatedLength = buffer.toString().length();
          document.replace(offset, region.getLength(), buffer.toString());
        } catch(BadLocationException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  public Point getSelection(IDocument document) {
    return new Point(region.getOffset(), generatedLength);
  }

  public String getAdditionalProposalInfo() {
    return null; //not to be used anymore
  }

  public String getDisplayString() {
    return "Insert reference to parent POM";
  }

  public Image getImage() {
    return MvnImages.IMG_OPEN_POM;
  }

  public IContextInformation getContextInformation() {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean isAutoInsertable() {
    return false;
  }

  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    return "Opens a search dialog where you can select the parent pom for this project.";
  }

}
