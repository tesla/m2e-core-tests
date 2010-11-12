package org.eclipse.m2e.editor.xml;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Node;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.editor.xml.PomHyperlinkDetector.ExpressionRegion;
import org.eclipse.m2e.editor.xml.internal.Messages;

public class PomTextHover implements ITextHover {

  public PomTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
  }
  
  public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
    if (hoverRegion instanceof ExpressionRegion) {
      ExpressionRegion region = (ExpressionRegion) hoverRegion;
      IMavenProjectFacade mvnproject = MavenPlugin.getDefault().getMavenProjectManager().getProject(region.project);
      if (mvnproject != null) {
        MavenProject mavprj = mvnproject.getMavenProject();
        if (mavprj != null) {
          String value = PomTemplateContext.simpleInterpolate(region.project, "${" + region.property + "}"); //$NON-NLS-1$ //$NON-NLS-2$
          String loc = null;
          Model mdl = mavprj.getModel();
          if (mdl.getProperties().containsKey(region.property)) {
            InputLocation location = mdl.getLocation("properties").getLocation(region.property); //$NON-NLS-1$
            if (location != null) {
              loc = location.getSource().getModelId();
            }
          }
          String ret = NLS.bind(Messages.PomTextHover_eval1, 
              value, loc != null ? NLS.bind(Messages.PomTextHover_eval2, loc) : ""); //$NON-NLS-2$
          return ret;
        }
      }
    }
    return null;
  }

  public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
    IDocument document = textViewer.getDocument();
    if(document == null) {
      return null;
    }

    Node current = PomHyperlinkDetector.getCurrentNode(document, offset);
    ExpressionRegion region = PomHyperlinkDetector.findExpressionRegion(current, textViewer, offset);
    if (region != null) {
      return region;
    }
    return null;
  }
  


}
