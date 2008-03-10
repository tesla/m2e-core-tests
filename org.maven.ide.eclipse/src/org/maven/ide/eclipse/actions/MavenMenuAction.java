/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.actions;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;


/**
 * Maven menu action
 * 
 * @author Eugene Kuleshov
 */
public class MavenMenuAction implements IObjectActionDelegate, IMenuCreator {

  boolean fillMenu;

  IStructuredSelection selection;

  IAction delegateAction;
  
  static ArrayList creators = new ArrayList();
  
  static {
    creators.add(new DefaultMavenMenuCreator());
  }

  public static void addCreator(AbstractMavenMenuCreator creator) {
    creators.add(creator);
  }

  // IObjectActionDelegate

  public void run(IAction action) {
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
      this.fillMenu = true;

      if(delegateAction != action) {
        delegateAction = action;
        delegateAction.setMenuCreator(this);
      }
      
      action.setEnabled(!selection.isEmpty());
      
      for(Iterator it = creators.iterator(); it.hasNext();) {
        AbstractMavenMenuCreator creator = (AbstractMavenMenuCreator) it.next();
        creator.selectionChanged(action, selection);
      }
    }
  }

  // IMenuCreator

  public void dispose() {
  }

  public Menu getMenu(Control parent) {
    return new Menu(parent);
  }

  public Menu getMenu(Menu parent) {
    Menu menu = new Menu(parent);
    new GroupMarker("new").fill(menu, -1);
    new GroupMarker("additions").fill(menu, -1);
    
    /**
     * Add listener to re-populate the menu each time it is shown because MenuManager.update(boolean, boolean) doesn't
     * dispose pull-down ActionContribution items for each popup menu.
     */
    menu.addMenuListener(new MenuAdapter() {
      public void menuShown(MenuEvent e) {
        if(fillMenu) {
          Menu m = (Menu) e.widget;
          MenuItem[] items = m.getItems();
          for(int i = 0; i < items.length; i++ ) {
            items[i].dispose();
          }
          for(Iterator it = creators.iterator(); it.hasNext();) {
            AbstractMavenMenuCreator creator = (AbstractMavenMenuCreator) it.next();
            creator.createMenu(m);
          }
          fillMenu = false;
        }
      }

    });
    return menu;
  }

}
