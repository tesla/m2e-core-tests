package org.eclipse.m2e.editor.xml;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.editor.xml.internal.Messages;

public class PomTextHover implements ITextHover {

  public PomTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
  }
  
  public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
    if (hoverRegion instanceof ExpressionRegion) {
      ExpressionRegion region = (ExpressionRegion) hoverRegion;
      //TODO MNGECLIPSE-2540
      final IProject prj = PomContentAssistProcessor.extractProject(textViewer);
      if (prj != null) {
        IMavenProjectFacade mvnproject = MavenPlugin.getDefault().getMavenProjectManager().getProject(prj);
        if (mvnproject != null) {
          MavenProject mavprj = mvnproject.getMavenProject();
          if (mavprj != null) {
            String value = PomTemplateContext.simpleInterpolate(prj, "${" + region.getProperty() + "}"); //$NON-NLS-1$ //$NON-NLS-2$
            String loc = null;
            Model mdl = mavprj.getModel();
            if (mdl.getProperties().containsKey(region.getProperty())) {
              InputLocation location = mdl.getLocation("properties").getLocation(region.getProperty()); //$NON-NLS-1$
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
    }
    return null;
  }

  public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
    IDocument document = textViewer.getDocument();
    if(document == null) {
      return null;
    }

    Node current = PomHyperlinkDetector.getCurrentNode(document, offset);
    //check if we have a property expression at cursor
    if (current != null && current instanceof Text) {
      Text textNode = (Text)current;
      String value = textNode.getNodeValue();
      if (value != null) {
        assert textNode instanceof IndexedRegion;
        IndexedRegion reg = (IndexedRegion)textNode;
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
          return new ExpressionRegion(startOffset, length, prop);
        }
      }
    }
    return null;
  }
  
  private class ExpressionRegion implements IRegion {

    private String property;
    private int length;
    private int offset;

    public ExpressionRegion(int startOffset, int length, String prop) {
      this.offset = startOffset;
      this.length = length;
      this.property = prop;
    }

    public int getLength() {
      return length;
    }

    public int getOffset() {
      return offset;
    }
    
    public String getProperty() {
      return property;
    }
    
  }


}
