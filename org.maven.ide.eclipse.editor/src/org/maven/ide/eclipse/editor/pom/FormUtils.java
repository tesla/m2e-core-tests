/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.maven.ide.eclipse.MavenPlugin;

/**
 * @author Eugene Kuleshov
 */
public abstract class FormUtils {

  public static String nvl(String s) {
    return s == null ? "" : s;
  }
  
  public static boolean isEmpty(String s) {
    return s == null || s.length()==0;
  }
  
  public static boolean isEmpty(Text t) {
    return t == null || isEmpty(t.getText());
  }
  
  public static void setText(Text control, String text) {
    if(!control.getText().equals(text)) {
      control.setText(nvl(text));
    }
  }
  
  public static void setText(CCombo control, String text) {
    if(!control.getText().equals(text)) {
      control.setText(nvl(text));
    }
  }
  
  public static void setButton(Button control, boolean selection) {
    if(control.getSelection()!=selection) {
      control.setSelection(selection);
    }
  }
  
  public static void openHyperlink(String url) {
    if(url!=null && url.trim().length()>0) {
      url = url.trim();
      try {
        IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
        IWebBrowser browser = browserSupport.createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR
            | IWorkbenchBrowserSupport.LOCATION_BAR, url, url, url);
        browser.openURL(new URL(url));
      } catch(PartInitException ex) {
        MavenPlugin.log(ex);
      } catch(MalformedURLException ex) {
        MavenPlugin.log("Malformed url " + url, ex);
      }
    }
  }

  public static void setEnabled(Composite composite, boolean enabled) {
    composite.setEnabled(enabled);
    for(Control control : composite.getChildren()) {
      if(control instanceof Combo) {
        control.setEnabled(enabled);
      
      } else if(control instanceof CCombo) {
        control.setEnabled(enabled);
      
      } else if(control instanceof Hyperlink) {
        control.setEnabled(enabled);
      
      } else if(control instanceof Composite) {
        setEnabled((Composite) control, enabled);
      
      } else {
        control.setEnabled(enabled);
      
      }
    }
  }

  public static void setReadonly(Composite composite, boolean readonly) {
    if(composite!=null) {
      for(Control control : composite.getChildren()) {
        if(control instanceof Text) {
          ((Text) control).setEditable(!readonly);
        
        } else if(control instanceof Combo) {
          ((Combo) control).setEnabled(!readonly);
        
        } else if(control instanceof CCombo) {
          ((CCombo) control).setEnabled(!readonly);
        
        } else if(control instanceof Button) {
          ((Button) control).setEnabled(!readonly);
        
        } else if(control instanceof Composite) {
          setReadonly((Composite) control, readonly);
        
        }
      }
    }
  }
  
}
