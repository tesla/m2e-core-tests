/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionDelegate;

import org.maven.ide.eclipse.MavenPlugin;

/**
 * Abstract Maven menu creator
 *
 * @author Eugene Kuleshov
 */
public abstract class AbstractMavenMenuCreator {

  protected IStructuredSelection selection;

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    }
  }

  protected abstract void createMenu(Menu menu);

  protected IAction addMenu(String id, String text, IActionDelegate actionDelegate, Menu menu) {
    return addMenu(id, text, actionDelegate, menu, null);
  }

  protected IAction addMenu(String id, String text, IActionDelegate actionDelegate, Menu menu, int style) {
    ActionProxy action = new ActionProxy(id, text, actionDelegate, style);
    return addMenu(action, menu, null);
  }
  
  protected IAction addMenu(String id, String text, IActionDelegate actionDelegate, Menu menu, String image) {
    ActionProxy action = new ActionProxy(id, text, actionDelegate);
    return addMenu(action, menu, image);
  }

  private IAction addMenu(ActionProxy action, Menu menu, String image) {
    if(image!=null) {
      action.setImageDescriptor(MavenPlugin.getImageDescriptor(image));
    }
    ActionContributionItem item = new ActionContributionItem(action);
    item.fill(menu, -1);
    
    return action;
  }

  class ActionProxy extends Action {
    private IActionDelegate action;
    
    public ActionProxy(String id, String text, IActionDelegate action) {
      super(text);
      this.action = action;
      setId(id);
    }
    
    public ActionProxy(String id, String text, IActionDelegate action, int style) {
      super(text, style);
      this.action = action;
      setId(id);
    }
    
    public void run() {
      action.selectionChanged(this, selection);
      action.run(this);
    }
  }
  
}

