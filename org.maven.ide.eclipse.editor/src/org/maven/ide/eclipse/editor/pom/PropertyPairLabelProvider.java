/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.maven.ide.components.pom.PropertyPair;
import org.maven.ide.eclipse.editor.MavenEditorImages;

/**
 * Label provider for <code>PropertyPair</code>
 * 
 * @author Eugene Kuleshov
 */
public class PropertyPairLabelProvider extends LabelProvider {

  public String getText(Object element) {
    if(element instanceof PropertyPair) {
      PropertyPair pair = (PropertyPair) element;
      return pair.getKey() + " : " + pair.getValue();
    }
    return super.getText(element);
  }
  
  public Image getImage(Object element) {
    return MavenEditorImages.IMG_PROPERTY;
  }
  
}
