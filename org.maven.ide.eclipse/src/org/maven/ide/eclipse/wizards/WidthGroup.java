/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wizards;

import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;

/**
 * Group of controls with the same width
 *
 * @author Eugene Kuleshov
 */
public class WidthGroup extends ControlAdapter {

  private final HashSet controls = new HashSet();

  public void controlResized(ControlEvent e) {
    int maxWidth = 0;
    for(Iterator it = this.controls.iterator(); it.hasNext();) {
      Control c = (Control) it.next();
      int width = c.getSize().x;
      if(width > maxWidth) {
        maxWidth = width;
      }
    }
    if(maxWidth > 0) {
      for(Iterator it = this.controls.iterator(); it.hasNext();) {
        Control c = (Control) it.next();
        GridData gd = (GridData) c.getLayoutData();
        gd.widthHint = maxWidth;
        c.getParent().layout();
      }
    }
  }
  
  public void addControl(Control control) {
    controls.add(control);
    control.getParent().layout();
  }
  
}

