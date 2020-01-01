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

  private ILaunchConfigurationType type;

  public MockLaunchConfiguration(Map<String, ?> attributes, ILaunchConfigurationType type) {
    this.attributes = attributes;
    this.type = type;
  }

  public MockLaunchConfiguration(Map<String, ?> attributes) {
    this(attributes, null);
  }

  @Override
  public <T> T getAdapter(Class<T> adapter) {
    return null;
  }

  @Override
  public boolean contentsEqual(ILaunchConfiguration configuration) {
    return false;
  }

  @Override
  public ILaunchConfigurationWorkingCopy copy(String name) {
    return null;
  }

  @Override
  public void delete() {
  }

  @Override
  public boolean exists() {
    return false;
  }

  @Override
  public boolean getAttribute(String attributeName, boolean defaultValue) {
    Boolean attr = (Boolean) attributes.get(attributeName);
    return attr == null ? defaultValue : attr;
  }

  @Override
  public int getAttribute(String attributeName, int defaultValue) {
    Integer attr = (Integer) attributes.get(attributeName);
    return attr == null ? defaultValue : attr;
  }

  @Override
  public List<String> getAttribute(String attributeName, List<String> defaultValue) {
    List<String> attr = (List<String>) attributes.get(attributeName);
    return attr == null ? defaultValue : attr;
  }

  @Override
  public Set<String> getAttribute(String attributeName, Set<String> defaultValue) {
    Set<String> attr = (Set<String>) attributes.get(attributeName);
    return attr == null ? defaultValue : attr;
  }

  @Override
  public Map<String, String> getAttribute(String attributeName, Map<String, String> defaultValue) {
    Map<String, String> attr = (Map<String, String>) attributes.get(attributeName);
    return attr == null ? defaultValue : attr;
  }

  @Override
  public String getAttribute(String attributeName, String defaultValue) {
    Object attr = attributes.get(attributeName);
    return attr == null ? defaultValue : attr.toString();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return (Map<String, Object>) attributes;
  }

  @Override
  public String getCategory() {
    return null;
  }

  @Override
  public IFile getFile() {
    return null;
  }

  @Override
  @Deprecated
  public IPath getLocation() {
    return null;
  }

  @Override
  public IResource[] getMappedResources() {
    return null;
  }

  @Override
  public String getMemento() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Set<String> getModes() {
    return null;
  }

  @Override
  public ILaunchDelegate getPreferredDelegate(Set<String> modes) {
    return null;
  }

  @Override
  public ILaunchConfigurationType getType() {
    return this.type;
  }

  @Override
  public ILaunchConfigurationWorkingCopy getWorkingCopy() {
    return null;
  }

  @Override
  public boolean hasAttribute(String attributeName) {
    return attributes.containsKey(attributeName);
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  @Override
  public boolean isMigrationCandidate() {
    return false;
  }

  @Override
  public boolean isWorkingCopy() {
    return false;
  }

  @Override
  public ILaunch launch(String mode, IProgressMonitor monitor) {
    return null;
  }

  @Override
  public ILaunch launch(String mode, IProgressMonitor monitor, boolean build) {
    return null;
  }

  @Override
  public ILaunch launch(String mode, IProgressMonitor monitor, boolean build, boolean register) {
    return null;
  }

  @Override
  public void migrate() {
  }

  @Override
  public boolean supportsMode(String mode) {
    return false;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public void delete(int flag) {
  }

  @Override
  public ILaunchConfiguration getPrototype() {
    return null;
  }

  @Override
  public boolean isAttributeModified(String attribute) {
    return false;
  }

  @Override
  public boolean isPrototype() {
    return false;
  }

  @Override
  public Collection<ILaunchConfiguration> getPrototypeChildren() {
    return null;
  }

  @Override
  public int getKind() {
    return 0;
  }

  @Override
  public Set<String> getPrototypeVisibleAttributes() {
    return null;
  }

  @Override
  public void setPrototypeAttributeVisibility(String attribute, boolean visible) {
  }

}
