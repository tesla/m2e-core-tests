package org.maven.ide.eclipse.editor.xml;

import org.eclipse.wst.xml.core.internal.modelhandler.ModelHandlerForXML;

@SuppressWarnings("restriction")
public class PomModelHandler extends ModelHandlerForXML {

  private static final String ASSOCIATED_CONTENT_TYPE_ID = "org.maven.ide.eclipse.pomFile";
  
  public PomModelHandler() {
    super();
    setAssociatedContentTypeId(ASSOCIATED_CONTENT_TYPE_ID);
  }
  
}
