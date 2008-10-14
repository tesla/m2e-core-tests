package org.maven.ide.eclipse.refactoring;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.maven.ide.components.pom.Model;

/**
 * This interface defines refactoring visitor
 * 
 * @author Anton Kraev
 *
 */
public interface PomVisitor {
  /*
   * Scans the model for affected objects 
   * 
   */
  public List<EObject> scanModel(IFile file, Model current);
  
  /*
   * Applies refactoring changes through undoable command (command will
   * be undone immediately after comparing the text to get refactoring diff)
   */
  public Command applyModel(AdapterFactoryEditingDomain editingDomain, List<EObject> list);
}
