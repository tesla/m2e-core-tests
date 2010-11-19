package org.eclipse.m2e.editor.xml;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
/**
 * insertion proposal for ${ expressions
 * @author mkleint
 *
 */
public class InsertExpressionProposal implements ICompletionProposal, ICompletionProposalExtension5 {

  private IMavenProjectFacade project;
  private String key;
  private Region region;
  private ISourceViewer sourceViewer;
  private int len = 0;

  public InsertExpressionProposal(ISourceViewer sourceViewer, Region region, String key, IMavenProjectFacade mvnproject) {
    this.sourceViewer = sourceViewer;
    this.region = region;
    this.key = key;
    this.project = mvnproject;
  }

  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    String value = PomTemplateContext.simpleInterpolate(project.getProject(), "${" + key + "}"); //$NON-NLS-1$ //$NON-NLS-2$
    MavenProject mavprj = project.getMavenProject();
    String loc = null;
    if (mavprj != null) {
      Model mdl = mavprj.getModel();
      if (mdl.getProperties().containsKey(key)) {
        InputLocation location = mdl.getLocation("properties").getLocation(key); //$NON-NLS-1$
        if (location != null) {
          loc = location.getSource().getModelId();
        }
      }
    }
    StringBuffer buff = new StringBuffer();
    buff.append("<html>");
    if (value != null) {
      buff.append(NLS.bind("The expression evaluates to <b>{0}</b> in the current effective pom.", value));
    }
    if (loc != null) {
      buff.append(NLS.bind("<br>It is based on property defined in <b>{0}</b>", loc));
    }
    buff.append("</html>");
    return buff.toString();
  }

  public void apply(IDocument document) {
    int offset = region.getOffset();
    String replace = "${" + key + "}";
    try {
      document.replace(offset, region.getLength(), replace);
      len = replace.length();
    } catch(BadLocationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public Point getSelection(IDocument document) {
    return new Point(region.getOffset() + len, 0);
  }

  public String getAdditionalProposalInfo() {
    //not used anymore
    return null;
  }

  public String getDisplayString() {
    return "${" + key + "}";
  }

  public Image getImage() {
    // TODO  what kind of icon to use?
    return null;
  }

  public IContextInformation getContextInformation() {
    return null;
  }

}
