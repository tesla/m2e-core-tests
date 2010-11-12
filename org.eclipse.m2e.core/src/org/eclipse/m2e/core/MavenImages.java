/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;


/**
 * @author Eugene Kuleshov
 */
public class MavenImages {

  // object images

  public static final Image IMG_CLEAR = createImage("clear.gif"); //$NON-NLS-1$
  
  public static final Image IMG_CLEAR_DISABLED = createImage("clear_disabled.gif"); //$NON-NLS-1$

  public static final Image IMG_JAR = createImage("jar_obj.gif"); //$NON-NLS-1$
  
  public static final Image IMG_VERSION = createImage("jar_version.gif"); //$NON-NLS-1$
  
  public static final Image IMG_VERSION_SRC = createImage("jar_src_version.gif"); //$NON-NLS-1$
  
  public static final Image IMG_JAVA = createImage("java_obj.gif"); //$NON-NLS-1$
  
  public static final Image IMG_JAVA_SRC = createImage("java_src_obj.gif"); //$NON-NLS-1$
  
  // public static final Image IMG_M2 = createImage("m2.gif");
  
  public static final Image IMG_LAUNCH_MAIN = createImage("main_tab.gif"); //$NON-NLS-1$
  
  public static final Image IMG_INDEX = createImage("maven_index.gif"); //$NON-NLS-1$
  
  public static final Image IMG_INDEXES = createImage("maven_indexes.gif"); //$NON-NLS-1$
  
  public static final Image IMG_MAVEN_JAR = createImage("mjar.gif"); //$NON-NLS-1$
  
  // public static final Image IMG_JAR = createImage("mlabel.gif");
  
  public static final Image IMG_NEW_POM = createImage("new_m2_pom.gif"); //$NON-NLS-1$
  
  public static final Image IMG_NEW_PROJECT = createImage("new_m2_project.gif"); //$NON-NLS-1$
  
  public static final Image IMG_OPEN_POM = createImage("open_pom.gif"); //$NON-NLS-1$
  
  // public static final Image IMG_POM = createImage("pom_obj.gif");
  
  public static final Image IMG_UPD_DEPENDENCIES = createImage("update_dependencies.gif"); //$NON-NLS-1$
  
  public static final Image IMG_UPD_SOURCES = createImage("update_source_folders.gif"); //$NON-NLS-1$
  
  public static final Image IMG_WEB = createImage("web.gif"); //$NON-NLS-1$
  
  // wizard images
  
  public static final ImageDescriptor WIZ_IMPORT_WIZ = create("import_project.png"); //$NON-NLS-1$

  public static final ImageDescriptor WIZ_NEW_PROJECT = create("new_m2_project_wizard.gif"); //$NON-NLS-1$
  
  // descriptors
  
  public static final ImageDescriptor M2 = create("m2.gif"); //$NON-NLS-1$
  
  public static final ImageDescriptor DEBUG = create("debug.gif"); //$NON-NLS-1$
  
  public static final ImageDescriptor ADD_INDEX = create("add_index.gif"); //$NON-NLS-1$

  public static final ImageDescriptor CLOSE = create("close.gif"); //$NON-NLS-1$
  
  public static final ImageDescriptor COPY = create("copy.gif"); //$NON-NLS-1$

  public static final ImageDescriptor COLLAPSE_ALL = create("collapseall.gif"); //$NON-NLS-1$
  
  public static final ImageDescriptor EXPAND_ALL = create("expandall.gif"); //$NON-NLS-1$

  public static final ImageDescriptor REFRESH = create("refresh.gif"); //$NON-NLS-1$
  
  public static final ImageDescriptor UPD_INDEX = create("update_index.gif"); //$NON-NLS-1$
  
  public static final ImageDescriptor REBUILD_INDEX = create("rebuild_index.gif"); //$NON-NLS-1$

  public static final ImageDescriptor POM = create("pom_obj.gif"); //$NON-NLS-1$
  
  public static final ImageDescriptor IMPORT_PROJECT = create("import_m2_project.gif"); //$NON-NLS-1$
  
  public static final ImageDescriptor SHOW_CONSOLE_ERR = create("stderr.gif"); //$NON-NLS-1$
  
  public static final ImageDescriptor SHOW_CONSOLE_OUT = create("stdout.gif"); //$NON-NLS-1$
  
  private static ImageDescriptor create(String key) {
    try {
      ImageRegistry imageRegistry = getImageRegistry();
      if(imageRegistry != null) {
        ImageDescriptor imageDescriptor = imageRegistry.getDescriptor(key);
        if(imageDescriptor==null) {
          imageDescriptor = createDescriptor(key);
          imageRegistry.put(key, imageDescriptor);
        }
        return imageDescriptor;
      }
    } catch(Exception ex) {
      MavenLogger.log(key, ex);
    }
    return null;
  }

  private static Image createImage(String key) {
    create(key);
    ImageRegistry imageRegistry = getImageRegistry();
    return imageRegistry == null ? null : imageRegistry.get(key);
  }

  private static ImageRegistry getImageRegistry() {
    MavenPlugin plugin = MavenPlugin.getDefault();
    return plugin == null ? null : plugin.getImageRegistry();
  }

  private static ImageDescriptor createDescriptor(String image) {
    return AbstractUIPlugin.imageDescriptorFromPlugin(IMavenConstants.PLUGIN_ID, "icons/" + image); //$NON-NLS-1$
  }

}
