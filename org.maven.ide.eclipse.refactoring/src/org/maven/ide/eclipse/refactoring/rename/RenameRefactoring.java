/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.refactoring.rename;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.maven.ide.components.pom.Model;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.refactoring.AbstractPomRefactoring;
import org.maven.ide.eclipse.refactoring.PomVisitor;
import org.maven.ide.eclipse.refactoring.RefactoringModelResources;
import org.maven.ide.eclipse.refactoring.RefactoringModelResources.PropertyInfo;


/**
 * Rename artifact refactoring implementation
 * 
 * @author Anton Kraev
 */
@SuppressWarnings("unchecked")
public class RenameRefactoring extends AbstractPomRefactoring {
  private static final Object[] EMPTY_OBJECT_ARRAY = new Object[] {};
  private static final String VERSION = "version";
  private static final String GETVERSION = "getVersion";
  private static final String ARTIFACT_ID = "artifactId";
  private static final String GETARTIFACT_ID = "getArtifactId";
  private static final String GROUP_ID = "groupId";
  private static final String GETGROUP_ID = "getGroupId";

  //XXX: move stuff UP after implementing another refactoring
  //after moving up, use this
  interface ScanVisitor {
    public boolean interested(EObject obj);
  }
  
  //this page contains new values
  MavenRenameWizardPage page;

  //old values
  String oldGroupId;
  String oldArtifactId;
  String oldVersion;

  public RenameRefactoring(IFile file, MavenRenameWizardPage page) {
    super(file);
    this.page = page;
  }
  
  //path (built during traversal of EMF model, used to find in effective model)
  class PathElement {
    String element;
    String artifactId;
    
    public PathElement(String element, String artifactId) {
      this.element = element;
      this.artifactId = artifactId;
    }
    
    public String toString() {
      return "/" + element + "[artifactId=" + artifactId + "]";
    }
  }
  
  class Path {
    List<PathElement> path = new ArrayList<PathElement>();
    
    public void addElement(String element, String artifactId) {
      path.add(new PathElement(element, artifactId));
    }
    
    public String toString() {
      return path.toString();
    }

    public Path clone() {
      Path res = new Path();
      res.path = new ArrayList<PathElement>(this.path);
      return res;
    }
  }
  
  //gets element from effective model based on path
  private Object getElement(Object root, Path path) {
    if (path == null || path.path.size() == 0)
      return root;
    
    PathElement current = path.path.remove(0);
    String getterName = "get" + current.element;
    try {
      if (root instanceof List) {
        List children = (List) root;
        for (int i=0; i<children.size(); i++) {
          Object child = children.get(i);
          Method artifact = child.getClass().getMethod(GETARTIFACT_ID, new Class[] {});
          String artifactId = (String) artifact.invoke(child, EMPTY_OBJECT_ARRAY);
          if (current.artifactId != null && !current.artifactId.equals(artifactId))
            continue;
          
          //found, names are correct
          return getElement(child, path);
        }
      } else {
        Method getter = root.getClass().getMethod(getterName, new Class[] {});
        return getElement(getter.invoke(root, EMPTY_OBJECT_ARRAY), path);
      }
      return null;
    } catch(Exception ex) {
      return null;
    }
  }
  
  class EObjectWithPath {
    public EObject object;
    public Path path;
    
    public EObjectWithPath(EObject object, Path path) {
      this.object = object;
      this.path = path;
    }
  }
  
  /**
   * Finds all potential matched objects in model
   * 
   */
  private List<EObjectWithPath> scanModel(Model model, String groupId, String artifactId, String version, boolean processRoot) {
    List<EObjectWithPath> res = new ArrayList<EObjectWithPath>();
    Path path = new Path();
    if(processRoot) {
      scanObject(path, model, groupId, artifactId, version, res);
    } else {
      scanChildren(path, model, groupId, artifactId, version, res);
    }
    return res;
  }

  //add candidate objects with same artifactId
  private List<EObjectWithPath> scanObject(Path current, EObject obj, String groupId, String artifactId, String version, List<EObjectWithPath> res) {
    if (scanFeature(obj, ARTIFACT_ID, artifactId)) {
      //System.out.println("found object " + obj + " : " + current);
      res.add(new EObjectWithPath(obj, current));
    }
    scanChildren(current, obj, groupId, artifactId, version, res);
    return res;
  }

  private List<EObjectWithPath> scanChildren(Path current, EObject obj, String groupId, String artifactId, String version, List<EObjectWithPath> res) {
    Iterator<EObject> it = obj.eContents().iterator();
    while(it.hasNext()) {
      obj = it.next();
      Path child = current.clone();
      child.addElement(obj.eClass().getName(), artifactId);
      scanObject(child, obj, groupId, artifactId, version, res);
    }
    return res;
  }

  private boolean scanFeature(EObject obj, String featureName, String value) {
    //not searching on this
    if(value == null) {
      return false;
    }
    EStructuralFeature feature = obj.eClass().getEStructuralFeature(featureName);
    if(feature == null) {
      return false;
    }
    String val = obj.eGet(feature) == null ? null : obj.eGet(feature).toString();
    if(value.equals(val)) {
      return true;
    }
    return false;
  }

  private String getValue(EObject obj, String featureName) {
    EStructuralFeature feature = obj.eClass().getEStructuralFeature(featureName);
    if(feature == null) {
      return null;
    }
    return obj.eGet(feature) == null ? null : obj.eGet(feature).toString();
  }

  public String getNewProjectName() {
    return page.getRenamed()? page.getNewArtifactId(): null;
  }
  
  /**
   * Applies new values in model
   * @param editingDomain 
   * 
   * @param editingDomain
   * @param renameProject 
   */

  public CompoundCommand applyModel(AdapterFactoryEditingDomain editingDomain, RefactoringModelResources model, 
      String newGroupId, String newArtifactId, String newVersion, boolean processRoot) {
    //find all affected objects in EMF model
    List<EObjectWithPath> affected = scanModel(model.getTmpModel(), this.oldGroupId, this.oldArtifactId, this.oldVersion, processRoot);
    
    //go through all affected objects, check in effective model
    Iterator<EObjectWithPath> i = affected.iterator();
    CompoundCommand command = new CompoundCommand();
    while (i.hasNext()) {
      EObjectWithPath obj = i.next();
      Object effectiveObj = getElement(model.getEffective(), obj.path.clone());
      try {
        if (effectiveObj == null) {
          //System.out.println("cannot find effective for: " + obj.object);
          continue;
        }
        Method method = effectiveObj.getClass().getMethod(GETVERSION, new Class[] {});
        String effectiveVersion = (String) method.invoke(effectiveObj, EMPTY_OBJECT_ARRAY);
        method = effectiveObj.getClass().getMethod(GETGROUP_ID, new Class[] {});
        String effectiveGroupId = (String) method.invoke(effectiveObj, EMPTY_OBJECT_ARRAY);
        //if version from effective POM is different from old version, skip it
        if (this.oldVersion != null && !this.oldVersion.equals(effectiveVersion)) {
          continue;
        }
        
        //only set groupId if effective group id is the same as old group id
        if (oldGroupId != null && oldGroupId.equals(effectiveGroupId))
          applyObject(editingDomain, command, obj.object, GROUP_ID, newGroupId);
        //set artifact id unconditionally
        applyObject(editingDomain, command, obj.object, ARTIFACT_ID, newArtifactId);
        //only set version if effective version is the same (already checked by the above)
        //and new version is not empty
        if (!"".equals(newVersion)) {
          PropertyInfo info = null;
          String old = getValue(obj.object, VERSION);
          if (old.startsWith("${")) {
            //this is a property, go find it
            String pName = old.substring(2);
            pName = pName.substring(0, pName.length() - 1).trim();
            info = model.getProperties().get(pName);
          }
          if (info != null)
            info.setNewValue(new SetCommand(editingDomain, info.getPair(), info.getPair().eClass().getEStructuralFeature("value"), newVersion));
          else
            applyObject(editingDomain, command, obj.object, VERSION, newVersion);
        }
        
      } catch(Exception e) {
        MavenLogger.log("Error processing refactoring", e);
      }
    }

    //XXX - just a refactoring mark
//    if (!command.isEmpty()) {
//      Set props = model.getProperties().keySet();
//      Object[] arr = props.toArray();
//      double koeff = System.currentTimeMillis() % 10;
//      koeff = koeff / 10;
//      PropertyInfo info = model.getProperties().get(arr[(int) (arr.length * koeff)]);
//      info.setNewValue(new SetCommand(editingDomain, info.getPair(), info.getPair().eClass().getEStructuralFeature("value"), "" + koeff));
//      int sum = 0;
//      for (int f=0; f<10000000; f++)
//        sum+= f;
//      System.out.println(info.getPair().getKey() + ":" + koeff + ":" + sum);
//    }
    
    return command.isEmpty()? null: command;
  }

  private void applyObject(AdapterFactoryEditingDomain editingDomain, CompoundCommand command, EObject obj,
      String featureName, String value) {
    EStructuralFeature feature = obj.eClass().getEStructuralFeature(featureName);
    if(feature == null) {
      return; 
    }
    Object old = obj.eGet(feature);
    if(old == null || old.equals(value)) {
      return;
    }
    command.append(new SetCommand(editingDomain, obj, feature, value));
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    this.oldArtifactId = getModel().getArtifactId();
    this.oldGroupId = getModel().getGroupId();
    this.oldVersion = getModel().getVersion();
    RefactoringStatus res = new RefactoringStatus();
    return res;
  }

  @Override
  public String getName() {
    return "Rename artifact";
  }

  @Override
  public PomVisitor getVisitor() {
    return new PomVisitor() {

      public CompoundCommand applyChanges(AdapterFactoryEditingDomain editingDomain, IFile file, RefactoringModelResources current) {
        //process <project> element only for the refactored file itself
        boolean processRoot = file.getParent().equals(getFile().getParent());
        return RenameRefactoring.this.applyModel(editingDomain, current, page.getNewGroupId(), 
            page.getNewArtifactId(), page.getNewVersion(), processRoot);
      }
    };
  }

}
