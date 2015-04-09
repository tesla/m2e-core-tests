/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.mocks;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchDelegate;


/**
 * MockLaunchConfiguration
 *
 * @author Fred Bricon
 */
public class MockLaunchConfiguration implements ILaunchConfiguration {

  private Map<String, ?> attributes;

  public MockLaunchConfiguration(Map<String, ?> attributes) {
    this.attributes = attributes;
  }

  public Object getAdapter(Class adapter) {
    // TODO Auto-generated method getAdapter
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#contentsEqual(org.eclipse.debug.core.ILaunchConfiguration)
   */
  public boolean contentsEqual(ILaunchConfiguration configuration) {
    // TODO Auto-generated method contentsEqual
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#copy(java.lang.String)
   */
  public ILaunchConfigurationWorkingCopy copy(String name) throws CoreException {
    // TODO Auto-generated method copy
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#delete()
   */
  public void delete() throws CoreException {
    // TODO Auto-generated method delete

  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#exists()
   */
  public boolean exists() {
    // TODO Auto-generated method exists
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getAttribute(java.lang.String, boolean)
   */
  public boolean getAttribute(String attributeName, boolean defaultValue) throws CoreException {
    Boolean attr = (Boolean) attributes.get(attributeName);
    return attr == null ? defaultValue : attr;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getAttribute(java.lang.String, int)
   */
  public int getAttribute(String attributeName, int defaultValue) throws CoreException {
    Integer attr = (Integer) attributes.get(attributeName);
    return attr == null ? defaultValue : attr;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getAttribute(java.lang.String, java.util.List)
   */
  public List<String> getAttribute(String attributeName, List<String> defaultValue) throws CoreException {
    List<String> attr = (List<String>) attributes.get(attributeName);
    return attr == null ? defaultValue : attr;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getAttribute(java.lang.String, java.util.Set)
   */
  public Set<String> getAttribute(String attributeName, Set<String> defaultValue) throws CoreException {
    Set<String> attr = (Set<String>) attributes.get(attributeName);
    return attr == null ? defaultValue : attr;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getAttribute(java.lang.String, java.util.Map)
   */
  public Map<String, String> getAttribute(String attributeName, Map<String, String> defaultValue) throws CoreException {
    Map<String, String> attr = (Map<String, String>) attributes.get(attributeName);
    return attr == null ? defaultValue : attr;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getAttribute(java.lang.String, java.lang.String)
   */
  public String getAttribute(String attributeName, String defaultValue) throws CoreException {
    Object attr = attributes.get(attributeName);
    return attr == null ? defaultValue : attr.toString();
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getAttributes()
   */
  public Map<String, Object> getAttributes() throws CoreException {
    return (Map<String, Object>) attributes;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getCategory()
   */
  public String getCategory() throws CoreException {
    // TODO Auto-generated method getCategory
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getFile()
   */
  public IFile getFile() {
    // TODO Auto-generated method getFile
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getLocation()
   */
  public IPath getLocation() {
    // TODO Auto-generated method getLocation
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getMappedResources()
   */
  public IResource[] getMappedResources() throws CoreException {
    // TODO Auto-generated method getMappedResources
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getMemento()
   */
  public String getMemento() throws CoreException {
    // TODO Auto-generated method getMemento
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getName()
   */
  public String getName() {
    // TODO Auto-generated method getName
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getModes()
   */
  public Set<String> getModes() throws CoreException {
    // TODO Auto-generated method getModes
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getPreferredDelegate(java.util.Set)
   */
  public ILaunchDelegate getPreferredDelegate(Set<String> modes) throws CoreException {
    // TODO Auto-generated method getPreferredDelegate
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getType()
   */
  public ILaunchConfigurationType getType() throws CoreException {
    // TODO Auto-generated method getType
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getWorkingCopy()
   */
  public ILaunchConfigurationWorkingCopy getWorkingCopy() throws CoreException {
    // TODO Auto-generated method getWorkingCopy
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#hasAttribute(java.lang.String)
   */
  public boolean hasAttribute(String attributeName) throws CoreException {
    return attributes.containsKey(attributeName);
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#isLocal()
   */
  public boolean isLocal() {
    // TODO Auto-generated method isLocal
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#isMigrationCandidate()
   */
  public boolean isMigrationCandidate() throws CoreException {
    // TODO Auto-generated method isMigrationCandidate
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#isWorkingCopy()
   */
  public boolean isWorkingCopy() {
    // TODO Auto-generated method isWorkingCopy
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#launch(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
   */
  public ILaunch launch(String mode, IProgressMonitor monitor) throws CoreException {
    // TODO Auto-generated method launch
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#launch(java.lang.String, org.eclipse.core.runtime.IProgressMonitor, boolean)
   */
  public ILaunch launch(String mode, IProgressMonitor monitor, boolean build) throws CoreException {
    // TODO Auto-generated method launch
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#launch(java.lang.String, org.eclipse.core.runtime.IProgressMonitor, boolean, boolean)
   */
  public ILaunch launch(String mode, IProgressMonitor monitor, boolean build, boolean register) throws CoreException {
    // TODO Auto-generated method launch
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#migrate()
   */
  public void migrate() throws CoreException {
    // TODO Auto-generated method migrate

  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#supportsMode(java.lang.String)
   */
  public boolean supportsMode(String mode) throws CoreException {
    // TODO Auto-generated method supportsMode
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#isReadOnly()
   */
  public boolean isReadOnly() {
    // TODO Auto-generated method isReadOnly
    return false;
  }

}
