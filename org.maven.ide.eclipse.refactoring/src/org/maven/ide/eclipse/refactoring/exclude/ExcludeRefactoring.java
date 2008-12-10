/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.refactoring.exclude;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.maven.ide.components.pom.Dependency;
import org.maven.ide.components.pom.Exclusion;
import org.maven.ide.components.pom.ExclusionsType;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.impl.PomFactoryImpl;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.refactoring.AbstractPomRefactoring;
import org.maven.ide.eclipse.refactoring.PomVisitor;
import org.maven.ide.eclipse.refactoring.RefactoringModelResources;

/**
 * Exclude artifact refactoring implementation
 * 
 * @author Anton Kraev
 *
 */
public class ExcludeRefactoring extends AbstractPomRefactoring {

  private String excludedArtifactId;
  private String excludedGroupId;

  /**
   * @param file
   */
  public ExcludeRefactoring(IFile file, String excludedGroupId, String excludedArtifactId) {
    super(file);
    this.excludedGroupId = excludedGroupId;
    this.excludedArtifactId = excludedArtifactId;
  }

  public PomVisitor getVisitor() {
    return new PomVisitor() {

      @SuppressWarnings("unchecked")
      public CompoundCommand applyChanges(RefactoringModelResources resources, IProgressMonitor pm) throws CoreException, IOException {
        final CompoundCommand command = new CompoundCommand();

        final List<Dependency> toRemove = new ArrayList<Dependency>();
        
        Model model = resources.getTmpModel();
        
        final List<Dependency> deps = model.getDependencies().getDependency();

        pm.beginTask("Loading dependency tree", 1);
        DependencyNode root = MavenPlugin.getDefault().getMavenModelManager().readDependencies(resources.getPomFile(), pm, Artifact.SCOPE_COMPILE);
        pm.worked(1);
        root.accept(new DependencyNodeVisitor() {

          private Dependency findDependency(String groupId, String artifactId) {
            for (Dependency d: deps) {
              if (d.getGroupId().equals(groupId) && d.getArtifactId().equals(artifactId)) {
                return d;
              }
            }
            return null;
          }
          
          public boolean endVisit(DependencyNode arg0) {
            return true;
          }

          public boolean visit(DependencyNode node) {
            Artifact a = node.getArtifact();
            if (a.getGroupId().equals(excludedGroupId) && a.getArtifactId().equals(excludedArtifactId)) {
              DependencyNode parent = node.getParent();
              if (parent == null) {
                // do not touch itself
                return false; 
              }
              if (parent.getParent() == null) {
                // need to remove first-level dependency
                toRemove.add(findDependency(excludedGroupId, excludedArtifactId));
                return false;
              }
              while (parent.getParent().getParent() != null) {
                parent = parent.getParent();
              }
              addExclusion(command, findDependency(parent.getArtifact().getGroupId(), parent.getArtifact().getArtifactId()));
              
            }
            return true;
          }
          
        });
        
        Iterator rem = toRemove.iterator();
        while (rem.hasNext()) {
          command.append(new RemoveCommand(editingDomain, model.getDependencies().getDependency(), rem.next()));
        }
        
        // XXX scan management as well
        
        return command;
      }

      private void addExclusion(CompoundCommand command, Dependency dep) {
        Exclusion exclusion = PomFactoryImpl.eINSTANCE.createExclusion();
        exclusion.setArtifactId(excludedArtifactId);
        exclusion.setGroupId(excludedGroupId);

        if (dep.getExclusions() == null) {
          EStructuralFeature feature = dep.eClass().getEStructuralFeature("exclusions");
          ExclusionsType excl = PomFactoryImpl.eINSTANCE.createExclusionsType();
          excl.getExclusion().add(exclusion);
          command.append(new SetCommand(editingDomain, dep, feature, excl));
        } else {
          command.append(new AddCommand(editingDomain, dep.getExclusions().getExclusion(), exclusion));
        }
      }
    };
  }

  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    return new RefactoringStatus();
  }

  public String getName() {
    return "Exclude Maven Artifact";
  }

  public String getTitle() {
    return "Excluding " + excludedGroupId + ":" + excludedArtifactId + " from " + file.getParent().getName();
  }
  
  public boolean scanAllArtifacts() {
    //do not scan other artifacts
    return false;
  }

}
