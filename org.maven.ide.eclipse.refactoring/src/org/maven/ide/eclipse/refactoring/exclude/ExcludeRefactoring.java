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
import java.util.Set;

import org.apache.maven.artifact.Artifact;
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
import org.maven.ide.eclipse.project.IMavenProjectFacade;
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
        CompoundCommand command = new CompoundCommand();

        List<Dependency> toRemove = new ArrayList<Dependency>();
        Model model = resources.getTmpModel();
        
        // scan dependencies
        Iterator<?> it = model.getDependencies().eContents().iterator();
        while (it.hasNext()) {
          Dependency dep = (Dependency) it.next();
          if (dep.getGroupId().equals(excludedGroupId) && dep.getArtifactId().equals(excludedArtifactId)) {
            toRemove.add(dep);
            continue;
          }
          
          // XXX do we need to scan on facade at all?
          IMavenProjectFacade facade = mavenPlugin.getMavenProjectManager().getMavenProject(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
          
          if (facade == null) {
            // find by artifacts for binary dependencies
            Set<Artifact> artifacts = resources.getProject().getArtifacts();
            boolean excluded = false;
            for(Artifact a : artifacts) {
              if (a.getGroupId().equals(excludedGroupId) &&
                  a.getArtifactId().equals(excludedArtifactId)) {
                Iterator<String> trail = a.getDependencyTrail().iterator();
                boolean hasDep = false;
                while (trail.hasNext()) {
                  String current = trail.next();
                  if (!hasDep) {
                    hasDep = checkTrail(current, dep.getGroupId(), dep.getArtifactId());
                  }

                  if (hasDep) {
                    break;
                  }
                }
                
                if (hasDep) {
                  excluded = true;
                  break;
                }
              }
            }

            if (excluded) {
              addExclusion(command, dep);
            }
          } else {
            // find by model for dependencies in workspace
            RefactoringModelResources res = loadModel(facade, pm);

            // XXX scan management as well
            Iterator<?> itEffective = res.getEffective().getDependencies().iterator();
            while (itEffective.hasNext()) {
              org.apache.maven.model.Dependency depEffective = (org.apache.maven.model.Dependency) itEffective.next();
              if (depEffective.getArtifactId().equals(excludedArtifactId) && 
                  depEffective.getGroupId().equals(excludedGroupId)) {
                //found, need to add exclusion
                addExclusion(command, dep);
              }
            }
          }
        }
        
        Iterator rem = toRemove.iterator();
        while (rem.hasNext()) {
          command.append(new RemoveCommand(editingDomain, model.getDependencies().getDependency(), rem.next()));
        }
        
        // XXX scan management as well
        
        return command;
      }

      private boolean checkTrail(String trail, String groupId, String artifactId) {
        return trail.startsWith(groupId + ":" + artifactId + ":");
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
