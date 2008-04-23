/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * Special scheduling rule to serialize project import and background dependency 
 * update jobs.
 */
public class SchedulingRule implements ISchedulingRule {
  
  private final boolean refresh;

  public SchedulingRule(boolean refresh) {
    this.refresh = refresh;
  }

  public boolean contains(ISchedulingRule rule) {
    return rule == this || rule instanceof IResource;
  }

  public boolean isConflicting(ISchedulingRule rule) {
    return rule instanceof SchedulingRule
        || (!refresh && rule instanceof IResource);
  }
}
