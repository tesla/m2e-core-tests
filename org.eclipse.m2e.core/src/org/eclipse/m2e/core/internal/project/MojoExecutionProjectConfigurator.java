/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

/**
 * MojoExecutionProjectConfigurator
 *
 * @author igor
 */
public class MojoExecutionProjectConfigurator extends AbstractProjectConfigurator {
  
  private final String groupId;
  private final String artifactId;
  private final VersionRange range;
  private final Set<String> goals;
  private final boolean runOnIncremental;

  MojoExecutionProjectConfigurator(String groupId, String artifactId, VersionRange range, Set<String> goals, boolean runOnIncremental) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.range = range;
    this.goals = goals;
    this.runOnIncremental = runOnIncremental;
  }

  public boolean isMatch(MojoExecution execution) {
    return groupId.equals(execution.getGroupId()) //
        && artifactId.equals(execution.getArtifactId()) //
        && (range == null || range.containsVersion(new DefaultArtifactVersion(execution.getVersion()))) //
        && (goals == null || goals.contains(execution.getGoal()));
  }

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
    // do nothing
  }

  public AbstractBuildParticipant getBuildParticipant(MojoExecution execution) {
    if (isMatch(execution)) {
      return new MojoExecutionBuildParticipant(execution, runOnIncremental);
    }
    
    return null;
  }

  public static MojoExecutionProjectConfigurator fromString(String str, boolean runOnIncremental) {
    if (str == null || str.trim().length() <= 0) {
      return null;
    }

    int p, c;

    p = 0;
    c = nextColonIndex(str, p);
    String groupId = substring(str, p, c);

    p = c + 1;
    c = nextColonIndex(str, p);
    String artifactId = substring(str, p, c);

    p = c + 1;
    c = nextColonIndex(str, p);
    String versionStr = substring(str, p, c);
    VersionRange version;
    try {
      version = versionStr != null? VersionRange.createFromVersionSpec(versionStr): null;
    } catch(InvalidVersionSpecificationException ex) {
      throw new IllegalArgumentException("Invalid mojo execution template: " + str, ex);
    }

    p = c + 1;
    String goalsStr = substring(str, p, str.length());
    Set<String> goals = goalsStr != null? new HashSet<String>(Arrays.asList(goalsStr.split(","))): null; //$NON-NLS-1$

    return new MojoExecutionProjectConfigurator(groupId, artifactId, version, goals, runOnIncremental);
  }

  private static String substring(String str, int start, int end) {
    String substring = str.substring(start, end);
    return "".equals(substring) ? null : substring; //$NON-NLS-1$
  }

  private static int nextColonIndex(String str, int pos) {
    int idx = str.indexOf(':', pos);
    if (idx < 0) {
      throw new IllegalArgumentException("Invalid mojo execution template: " + str);
    }
    return idx;
  }

  public String getName() {
    StringBuilder sb = new StringBuilder();

    sb.append(groupId);
    sb.append(':').append(artifactId);

    sb.append(':');
    if (range != null) {
      sb.append(range.toString());
    }

    sb.append(':');
    if (goals != null) {
      boolean first = true;
      for (String goal : goals) {
        if (!first) {
          sb.append(',');
        }
        sb.append(goal);
        first = false;
      }
    }

    
    return sb.toString();
  }
}
