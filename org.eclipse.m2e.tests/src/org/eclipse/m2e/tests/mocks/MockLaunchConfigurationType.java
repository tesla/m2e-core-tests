/*******************************************************************************
 * Copyright (c) 2020 Till Brychcy.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Till Brychcy - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.tests.mocks;

import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;


@SuppressWarnings("deprecation")
public class MockLaunchConfigurationType implements ILaunchConfigurationType {
  private Map<String, String> attributes;

  public MockLaunchConfigurationType(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  @Override
  public <T> T getAdapter(Class<T> adapter) {
    return null;
  }

  @Override
  public String getAttribute(String attributeName) {
    return attributes.get(attributeName);
  }

  @Override
  public String getCategory() {
    return null;
  }

  @Override
  public ILaunchConfigurationDelegate getDelegate() {
    return null;
  }

  @Override
  public ILaunchConfigurationDelegate getDelegate(String mode) {
    return null;
  }

  @Override
  public ILaunchDelegate[] getDelegates(Set<String> modes) {
    return null;
  }

  @Override
  public ILaunchDelegate getPreferredDelegate(Set<String> modes) {
    return null;
  }

  @Override
  public void setPreferredDelegate(Set<String> modes, ILaunchDelegate delegate) {
  }

  @Override
  public boolean supportsModeCombination(Set<String> modes) {
    return false;
  }

  @Override
  public String getIdentifier() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public String getPluginIdentifier() {
    return null;
  }

  @Override
  public String getSourceLocatorId() {
    return null;
  }

  @Override
  public ISourcePathComputer getSourcePathComputer() {
    return null;
  }

  @Override
  public Set<String> getSupportedModes() {
    return null;
  }

  @Override
  public Set<Set<String>> getSupportedModeCombinations() {
    return null;
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  @Override
  public ILaunchConfigurationWorkingCopy newInstance(IContainer container, String name) {
    return null;
  }

  @Override
  public boolean supportsMode(String mode) {
    return false;
  }

  @Override
  public String getContributorName() {
    return null;
  }

  @Override
  public ILaunchConfiguration[] getPrototypes() {
    return null;
  }

  @Override
  public ILaunchConfigurationWorkingCopy newPrototypeInstance(IContainer container, String name) {
    return null;
  }

  @Override
  public boolean supportsPrototypes() {
    return false;
  }

  @Override
  public boolean supportsCommandLine() {
    return false;
  }

  @Override
  public boolean supportsOutputMerging() {
    return false;
  }
}