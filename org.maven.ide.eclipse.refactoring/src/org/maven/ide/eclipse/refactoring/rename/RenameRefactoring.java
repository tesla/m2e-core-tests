/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.refactoring.rename;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.maven.ide.components.pom.Model;
import org.maven.ide.eclipse.refactoring.AbstractPomRefactoring;
import org.maven.ide.eclipse.refactoring.PomVisitor;


/**
 * Rename artifact refactoring implementation
 * 
 * @author Anton Kraev
 */
public class RenameRefactoring extends AbstractPomRefactoring {
  private static final String VERSION = "version";
  private static final String ARTIFACT_ID = "artifactId";
  private static final String GROUP_ID = "groupId";

  MavenRenameWizardPage page;
  String groupId;
  String artifactId;
  String version;

  public RenameRefactoring(IFile file, MavenRenameWizardPage page) {
    super(file);
    this.page = page;
  }

  /**
   * Finds all potential matched objects in model
   * 
   * @param processRoot
   */
  List<EObject> scanModel(Model model, String groupId, String artifactId, String version, boolean processRoot) {
    List<EObject> res = new ArrayList<EObject>();
    if(processRoot) {
      scanObject(groupId, artifactId, version, res, model);
    }
    Iterator<EObject> it = model.eAllContents();
    while(it.hasNext()) {
      EObject obj = it.next();
      scanObject(groupId, artifactId, version, res, obj);
    }
    return res;
  }

  private void scanObject(String groupId, String artifactId, String version, List<EObject> res, EObject obj) {
    scanFeature(obj, GROUP_ID, groupId, res);
    scanFeature(obj, ARTIFACT_ID, artifactId, res);
    scanFeature(obj, VERSION, version, res);
  }

  private void scanFeature(EObject obj, String featureName, String value, List<EObject> res) {
    //not searching on this
    if(value == null) {
      return;
    }
    EStructuralFeature feature = obj.eClass().getEStructuralFeature(featureName);
    if(feature == null) {
      return;
    }
    String val = obj.eGet(feature) == null ? null : obj.eGet(feature).toString();
    if(value.equals(val) && !res.contains(obj)) {
      res.add(obj);
    }
  }

  /**
   * Applies new values in model
   * 
   * @param editingDomain
   */

  Command applyModel(AdapterFactoryEditingDomain editingDomain, List<EObject> affected, String groupId,
      String artifactId, String version) {
    Iterator<EObject> it = affected.iterator();
    CompoundCommand command = new CompoundCommand();
    while(it.hasNext()) {
      EObject obj = it.next();
      applyObject(editingDomain, command, obj, GROUP_ID, groupId);
      applyObject(editingDomain, command, obj, ARTIFACT_ID, artifactId);
      applyObject(editingDomain, command, obj, VERSION, version);
    }
    return command;
  }

  private void applyObject(AdapterFactoryEditingDomain editingDomain, CompoundCommand command, EObject obj,
      String featureName, String value) {
    EStructuralFeature feature = obj.eClass().getEStructuralFeature(featureName);
    if(feature == null)
      return;
    Object old = obj.eGet(feature);
    if((old == null && value == null) || (old != null && old.equals(value)))
      return;
    command.append(new SetCommand(editingDomain, obj, feature, value));
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    this.artifactId = getModel().getArtifactId();
    this.groupId = getModel().getGroupId();
    this.version = getModel().getVersion();
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

      public Command applyModel(AdapterFactoryEditingDomain editingDomain, List<EObject> list) {
        return RenameRefactoring.this.applyModel(editingDomain, list, page.getNewGroupId(), page.getNewArtifactId(),
            page.getNewVersion());
      }

      public List<EObject> scanModel(IFile file, Model current) {
        return RenameRefactoring.this.scanModel(current, groupId, artifactId, version, file.equals(getFile()));
      }

    };
  }

}
