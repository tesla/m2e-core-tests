/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.maven.ide.eclipse.MavenPlugin;

/**
 * @author Eugene Kuleshov
 */
public class MavenEditorImages {

  // images
  
  public static final Image IMG_CLEAR = createImage("clear.gif");
  
  public static final Image IMG_CLEAR_DISABLED = createImage("clear_disabled.gif");
  
  public static final Image IMG_PROJECT = createImage("project_obj.gif"); 

  // object images
  
  public static final Image IMG_JAR = createImage("jar_obj.gif"); 

  public static final Image IMG_REPOSITORY = createImage("repository_obj.gif"); 
  
  public static final Image IMG_PLUGIN = createImage("plugin_obj.gif"); 

  public static final Image IMG_EXECUTION = createImage("execution_obj.gif");
  
  public static final Image IMG_GOAL = createImage("goal_obj.gif"); 

  public static final Image IMG_FILTER = createImage("filter_obj.gif"); 

  public static final Image IMG_RESOURCE = createImage("resource_obj.gif"); 

  public static final Image IMG_INCLUDE = createImage("include_obj.gif"); 
  
  public static final Image IMG_EXCLUDE = createImage("exclude_obj.gif"); 
  
  public static final Image IMG_PERSON = createImage("person_obj.gif");
  
  public static final Image IMG_ROLE = createImage("role_obj.gif");
  
  public static final Image IMG_PROPERTY = createImage("property_obj.gif");

  public static final Image IMG_REPORT = createImage("report_obj.gif");

  public static final Image IMG_PROFILE = createImage("profile_obj.gif");
  
  // image descriptors
  
  public static final ImageDescriptor REFRESH = create("refresh.gif"); 
  
  public static final ImageDescriptor COLLAPSE_ALL = create("collapseall.gif");

  public static final ImageDescriptor EXPAND_ALL = create("expandall.gif");

  public static final ImageDescriptor SHOW_GROUP = create("group.gif");

  // XXX fix icon
  public static final ImageDescriptor SELECT_ARTIFACT = create("group.gif");

  // XXX fix icon
  public static final ImageDescriptor ADD_ARTIFACT = create("group.gif");

  public static final ImageDescriptor SORT = create("sort.gif");

  public static final ImageDescriptor FILTER = create("filter.gif");

  public static final ImageDescriptor EFFECTIVE_POM = create("effective_pom.gif");
  
  public static final ImageDescriptor PARENT_POM = create("parent_pom.gif");


  private static ImageDescriptor create(String key) {
    try {
      ImageDescriptor imageDescriptor = createDescriptor(key);
      ImageRegistry imageRegistry = getImageRegistry();
      if(imageRegistry!=null) {
        imageRegistry.put(key, imageDescriptor);
      }
      return imageDescriptor;
    } catch (Exception ex) {
      MavenPlugin.log(key, ex);
      return null;
    }
  }

  private static Image createImage(String key) {
    create(key);
    ImageRegistry imageRegistry = getImageRegistry();
    return imageRegistry==null ? null : imageRegistry.get(key);
  }

  private static ImageRegistry getImageRegistry() {
    MavenEditorPlugin plugin = MavenEditorPlugin.getDefault();
    return plugin==null ? null : plugin.getImageRegistry();
  }

  private static ImageDescriptor createDescriptor(String image) {
    return MavenEditorPlugin.imageDescriptorFromPlugin(MavenEditorPlugin.PLUGIN_ID, "icons/" + image);
  }
  
}
