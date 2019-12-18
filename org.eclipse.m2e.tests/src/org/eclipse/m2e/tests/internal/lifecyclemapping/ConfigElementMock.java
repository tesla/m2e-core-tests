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

package org.eclipse.m2e.tests.internal.lifecyclemapping;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.InvalidRegistryObjectException;


//TODO Remove and use some Mocking framework instead
class ConfigElementMock implements IConfigurationElement {

  @Override
  public Object createExecutableExtension(String propertyName) {
    return null;
  }

  @Override
  public String getAttribute(String name) throws InvalidRegistryObjectException {
    return null;
  }

  @Override
  public String getAttribute(String attrName, String locale) throws InvalidRegistryObjectException {
    return null;
  }

  @Deprecated
  @Override
  public String getAttributeAsIs(String name) {
    return null;
  }

  @Override
  public String[] getAttributeNames() {
    return null;
  }

  @Override
  public IConfigurationElement[] getChildren() {
    return null;
  }

  @Override
  public IConfigurationElement[] getChildren(String name) {
    return null;
  }

  @Override
  public IExtension getDeclaringExtension() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Object getParent() {
    return null;
  }

  @Override
  public String getValue() {
    return null;
  }

  @Override
  public String getValue(String locale) {
    return null;
  }

  @Deprecated
  @Override
  public String getValueAsIs() {
    return null;
  }

  @Deprecated
  @Override
  public String getNamespace() {
    return null;
  }

  @Override
  public String getNamespaceIdentifier() {
    return null;
  }

  @Override
  public IContributor getContributor() {
    return null;
  }

  @Override
  public boolean isValid() {
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.runtime.IConfigurationElement#getHandleId()
   */
  @Override
  public int getHandleId() {
    // TODO Auto-generated method getHandleId
    return 0;
  }
}
