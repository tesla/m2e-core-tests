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

package org.eclipse.m2e.editor.lifecycle.internal;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.internal.project.MojoExecutionUtils;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.editor.lifecycle.ILifecycleMappingEditorContribution;
import org.eclipse.m2e.editor.lifecycle.MojoExecutionData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

public class ExecutionsTableContentProvider implements IStructuredContentProvider, ITableLabelProvider, IColorProvider, ICellModifier {
  private ILifecycleMappingEditorContribution contributor;
  private IMavenProjectFacade mavenProject;
  private Viewer viewer;
  private Image checkedImage;
  private Image uncheckedImage;
  
  public ExecutionsTableContentProvider() {
  }
  
  public void dispose() {
    checkedImage.dispose();
    uncheckedImage.dispose();
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if(null == this.viewer) {
      checkedImage = makeShot(viewer.getControl(), true);
      uncheckedImage = makeShot(viewer.getControl(), false);
    }
    contributor = (ILifecycleMappingEditorContribution)newInput;
    this.viewer = viewer;
  }

  public Image getColumnImage(Object object, int column) {
    if(column == 1) {
      return ((MojoExecutionData)object).isRunOnIncrementalBuild() ? checkedImage : uncheckedImage;
    }
    return null;
  }
  
  public String getColumnText(Object object, int column) {
    return column == 0 ? ((MojoExecutionData)object).getDisplayName() : null;
  }
  
  public Color getBackground(Object object) {
    return null;
  }
  
  public Color getForeground(Object object) {
    boolean enabled = ((MojoExecutionData)object).isEnabled();
    
    return enabled ? Display.getCurrent().getSystemColor(SWT.COLOR_BLACK) : Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
  }
  

  public void addListener(ILabelProviderListener arg0) { }

  public boolean isLabelProperty(Object object, String property) {
    return LifecyclePage.EXEC_TABLE_COLUMN_PROPERTIES[0].equals(property);
  }

  public void removeListener(ILabelProviderListener arg0) { }
  
  public Object[] getElements(Object parent) {
    try {
      Map<String, MojoExecutionData> enabled = new LinkedHashMap<String, MojoExecutionData>();
      for(MojoExecutionData med : contributor.getMojoExecutions()) {
        enabled.put(med.getId(), med);
      }
      
      List<MojoExecutionData> executions = new LinkedList<MojoExecutionData>();
      MavenExecutionPlan plan = mavenProject.getExecutionPlan(new NullProgressMonitor());
      for(MojoExecution execution : plan.getExecutions()) {
        String id = MojoExecutionUtils.getExecutionKey(execution);
        if(!enabled.containsKey(id)) {
          executions.add(new MojoExecutionData(id, id, false, false));
        } else {
          executions.add(enabled.get(id));
        }
      }
      return executions.toArray();
    } catch(CoreException e) {
      MavenLogger.log(e);
      return new Object[]{};
    }
  }
  
  public boolean canModify(Object object, String property) {
    try {
      if(LifecyclePage.EXEC_TABLE_COLUMN_PROPERTIES[1].equals(property)) {
        return contributor.canSetIncremental((MojoExecutionData)object);
      } else {
        return false;
      }
    } catch(CoreException e) {
      MavenLogger.log(e);
      return false;
    }
  }
  public Object getValue(Object object, String property) {
    if(LifecyclePage.EXEC_TABLE_COLUMN_PROPERTIES[1].equals(property)) {
      return ((MojoExecutionData)object).isRunOnIncrementalBuild();
    }
    return null;
  }
  public void modify(Object object, String property, Object newValue) {
    try {
      TableItem item = (TableItem)object;
      contributor.setIncremental((MojoExecutionData)item.getData(), ((Boolean)newValue).booleanValue());
      viewer.refresh();
    } catch(CoreException e) {
      MavenLogger.log(e);
    }
  }
  
  private Image makeShot(Control control, boolean type)
  {
    // Hopefully no platform uses exactly this color
    // because we'll make it transparent in the image.
    Color greenScreen = new Color(control.getDisplay(),
      222, 223, 224);

    Shell shell = new Shell(control.getShell(),
      SWT.NO_TRIM);

    // otherwise we have a default gray color
    shell.setBackground(greenScreen);

    Button button = new Button(shell, SWT.CHECK);
    button.setBackground(greenScreen);
    button.setSelection(type);

    // otherwise an image is located in a corner
    button.setLocation(1, 1);
    Point bsize = button.computeSize(SWT.DEFAULT,
      SWT.DEFAULT);

    // otherwise an image is stretched by width
    bsize.x = Math.max(bsize.x - 1, bsize.y - 1);
    bsize.y = Math.max(bsize.x - 1, bsize.y - 1);
    button.setSize(bsize);
    shell.setSize(bsize);

    shell.open();
    GC gc = new GC(shell);
    Image image = new Image(control.getDisplay(),
      bsize.x, bsize.y);
    gc.copyArea(image, 0, 0);
    gc.dispose();
    shell.close();

    ImageData imageData = image.getImageData();
    imageData.transparentPixel = imageData
      .palette.getPixel(greenScreen.getRGB());

    return new Image(control.getDisplay(), imageData);
  }


  /**
   * @return the mavenProject
   */
  public IMavenProjectFacade getMavenProject() {
    return mavenProject;
  }

  /**
   * @param mavenProject the mavenProject to set
   */
  public void setMavenProject(IMavenProjectFacade mavenProject) {
    this.mavenProject = mavenProject;
  }
  
  

}
