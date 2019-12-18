/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.mocks;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
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

  public <T> T getAdapter(Class<T> adapter) {
    return null;
  }

  public boolean contentsEqual(ILaunchConfiguration configuration) {
    return false;
  }

  public ILaunchConfigurationWorkingCopy copy(String name) {
    return null;
  }

  public void delete() {
  }

  public boolean exists() {
    return false;
  }

  public boolean getAttribute(String attributeName, boolean defaultValue) {
    Boolean attr = (Boolean) attributes.get(attributeName);
    return attr == null ? defaultValue : attr;
  }

  public int getAttribute(String attributeName, int defaultValue) {
    Integer attr = (Integer) attributes.get(attributeName);
    return attr == null ? defaultValue : attr;
  }

  public List<String> getAttribute(String attributeName, List<String> defaultValue) {
    List<String> attr = (List<String>) attributes.get(attributeName);
    return attr == null ? defaultValue : attr;
  }

  public Set<String> getAttribute(String attributeName, Set<String> defaultValue) {
    Set<String> attr = (Set<String>) attributes.get(attributeName);
    return attr == null ? defaultValue : attr;
  }

  public Map<String, String> getAttribute(String attributeName, Map<String, String> defaultValue) {
    Map<String, String> attr = (Map<String, String>) attributes.get(attributeName);
    return attr == null ? defaultValue : attr;
  }

  public String getAttribute(String attributeName, String defaultValue) {
    Object attr = attributes.get(attributeName);
    return attr == null ? defaultValue : attr.toString();
  }

  public Map<String, Object> getAttributes() {
    return (Map<String, Object>) attributes;
  }

  public String getCategory() {
    return null;
  }

  public IFile getFile() {
    return null;
  }

  @Deprecated
  public IPath getLocation() {
    return null;
  }

  public IResource[] getMappedResources() {
    return null;
  }

  public String getMemento() {
    return null;
  }

  public String getName() {
    return null;
  }

  public Set<String> getModes() {
    return null;
  }

  public ILaunchDelegate getPreferredDelegate(Set<String> modes) {
    return null;
  }

  public ILaunchConfigurationType getType() {
    return null;
  }

  public ILaunchConfigurationWorkingCopy getWorkingCopy() {
    return null;
  }

  public boolean hasAttribute(String attributeName) {
    return attributes.containsKey(attributeName);
  }

  public boolean isLocal() {
    return false;
  }

  public boolean isMigrationCandidate() {
    return false;
  }

  public boolean isWorkingCopy() {
    return false;
  }

  public ILaunch launch(String mode, IProgressMonitor monitor) {
    return null;
  }

  public ILaunch launch(String mode, IProgressMonitor monitor, boolean build) {
    return null;
  }

  public ILaunch launch(String mode, IProgressMonitor monitor, boolean build, boolean register) {
    return null;
  }

  public void migrate() {
  }

  public boolean supportsMode(String mode) {
    return false;
  }

  public boolean isReadOnly() {
    return false;
  }

  public void delete(int flag) {
  }

  public ILaunchConfiguration getPrototype() {
    return null;
  }

  public boolean isAttributeModified(String attribute) {
    return false;
  }

  public boolean isPrototype() {
    return false;
  }

  public Collection<ILaunchConfiguration> getPrototypeChildren() {
    return null;
  }

  public int getKind() {
    return 0;
  }

  public Set<String> getPrototypeVisibleAttributes() {
    return null;
  }

  public void setPrototypeAttributeVisibility(String attribute, boolean visible) {
  }

}
