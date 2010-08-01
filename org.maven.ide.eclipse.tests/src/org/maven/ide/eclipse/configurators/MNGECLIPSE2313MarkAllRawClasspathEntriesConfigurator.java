
package org.maven.ide.eclipse.configurators;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.JavaCore;
import org.maven.ide.eclipse.jdt.IClasspathDescriptor;
import org.maven.ide.eclipse.jdt.IClasspathEntryDescriptor;
import org.maven.ide.eclipse.jdt.IJavaProjectConfigurator;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;


public class MNGECLIPSE2313MarkAllRawClasspathEntriesConfigurator extends AbstractProjectConfigurator implements
    IJavaProjectConfigurator {

  public static final IClasspathAttribute ATTR = JavaCore.newClasspathAttribute(
      MNGECLIPSE2313MarkAllRawClasspathEntriesConfigurator.class.getName(), "bar");

  public MNGECLIPSE2313MarkAllRawClasspathEntriesConfigurator() {
  }

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
  }

  public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor)
      throws CoreException {
  }

  public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {

    for(IClasspathEntryDescriptor entry : classpath.getEntryDescriptors()) {
      entry.setClasspathAttribute(ATTR.getName(), ATTR.getValue());
    }

  }

}
