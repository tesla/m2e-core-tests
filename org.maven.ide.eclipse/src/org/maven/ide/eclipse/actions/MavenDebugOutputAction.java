/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.maven.ide.eclipse.MavenImages;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.internal.preferences.MavenPreferenceConstants;


/**
 * @author Eugene Kuleshov
 */
public class MavenDebugOutputAction extends Action {

  private IPropertyChangeListener listener = new IPropertyChangeListener() {
    public void propertyChange(PropertyChangeEvent event) {
      if(MavenPreferenceConstants.P_DEBUG_OUTPUT.equals(event.getProperty())) {
        setChecked(isDebug());
      }
    }
  };

  public MavenDebugOutputAction() {
    setToolTipText("Debug Output");
    setImageDescriptor(MavenImages.DEBUG);
    
    getPreferenceStore().addPropertyChangeListener(listener);
    setChecked(isDebug());
  }

  public void run() {
    getPreferenceStore().setValue(MavenPreferenceConstants.P_DEBUG_OUTPUT, isChecked());
  }
  
  public void dispose() {
    getPreferenceStore().removePropertyChangeListener(listener);
  }

  IPreferenceStore getPreferenceStore() {
    return MavenPlugin.getDefault().getPreferenceStore();
  }

  boolean isDebug() {
    return getPreferenceStore().getBoolean(MavenPreferenceConstants.P_DEBUG_OUTPUT);
  }
  
}

