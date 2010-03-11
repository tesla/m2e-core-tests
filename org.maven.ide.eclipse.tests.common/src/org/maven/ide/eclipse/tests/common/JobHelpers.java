/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.tests.common;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;

import org.maven.ide.eclipse.jobs.IBackgroundProcessingQueue;

public class JobHelpers {

  public static void waitForJobsToComplete() {
    try {
      waitForJobsToComplete(new NullProgressMonitor());
    } catch(Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static void waitForJobsToComplete(IProgressMonitor monitor) throws InterruptedException, CoreException {
    waitForBuildJobs();

    /*
     * First, make sure refresh job gets all resource change events
     * 
     * Resource change events are delivered after WorkspaceJob#runInWorkspace returns
     * and during IWorkspace#run. Each change notification is delivered by
     * only one thread/job, so we make sure no other workspaceJob is running then
     * call IWorkspace#run from this thread. 
     * 
     * Unfortunately, this does not catch other jobs and threads that call IWorkspace#run
     * so we have to hard-code workarounds
     *  
     * See http://www.eclipse.org/articles/Article-Resource-deltas/resource-deltas.html
     */
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IJobManager jobManager = Job.getJobManager();
    jobManager.suspend();
    try {
      Job[] jobs = jobManager.find(null);
      for (int i = 0; i < jobs.length; i++) {
        if(jobs[i] instanceof WorkspaceJob || jobs[i].getClass().getName().endsWith("JREUpdateJob")) {
          jobs[i].join();
        }
      }
      workspace.run(new IWorkspaceRunnable() {
        public void run(IProgressMonitor monitor) {
        }
      }, workspace.getRoot(), 0, monitor);

      // Now we flush all background processing queues
      boolean processed = flushProcessingQueues(jobManager, monitor);
      for (int i = 0; i < 10 && processed; i++) {
        processed = flushProcessingQueues(jobManager, monitor);
      }

      Assert.assertFalse("Could not flush background processing queues: " + getProcessingQueues(jobManager), processed);
    } finally {
      jobManager.resume();
    }

    waitForBuildJobs();
  }

  private static boolean flushProcessingQueues(IJobManager jobManager, IProgressMonitor monitor) throws InterruptedException {
    boolean processed = false;
    for (IBackgroundProcessingQueue queue : getProcessingQueues(jobManager)) {
      queue.join();
      if (!queue.isEmpty()) {
        queue.run(monitor);
        processed = true;
      }
      if (queue.isEmpty()) {
        queue.cancel();
      }
    }
    return processed;
  }

  private static List<IBackgroundProcessingQueue> getProcessingQueues(IJobManager jobManager) {
    ArrayList<IBackgroundProcessingQueue> queues = new ArrayList<IBackgroundProcessingQueue>();
    for (Job job : jobManager.find(null)) {
      if (job instanceof IBackgroundProcessingQueue) {
        queues.add((IBackgroundProcessingQueue) job);
      }
    }
    return queues;
  }

  private static void waitForBuildJobs() {
    waitForJobs("(.*\\.AutoBuild.*)|(.*\\.DebugUIPlugin.*)", 30000);
  }

  private static void waitForJobs(String classNameRegex, int maxWaitMillis) {
    final int waitMillis = 100;
    for(int i = maxWaitMillis / waitMillis; i >= 0; i-- ) {
      if(!hasJob(classNameRegex)) {
        return;
      }
      try {
        Thread.sleep(waitMillis);
      } catch(InterruptedException e) {
        // ignore
      }
    }
    Assert.fail("Timeout while waiting for completion of jobs: " + classNameRegex);
  }

  private static boolean hasJob(String classNameRegex) {
    Job[] jobs = Job.getJobManager().find(null);
    for(Job job : jobs) {
      if(job.getClass().getName().matches(classNameRegex)) {
        return true;
      }
    }
    return false;
  }

  public static void waitForLaunchesToComplete(int maxWaitMillis) {
    // wait for any jobs that actually start the launch
    waitForJobs("(.*\\.DebugUIPlugin.*)", maxWaitMillis);

    // wait for the launches themselves
    final int waitMillis = 100;
    for(int i = maxWaitMillis / waitMillis; i >= 0; i-- ) {
      if(!hasActiveLaunch()) {
        return;
      }
      try {
        Thread.sleep(waitMillis);
      } catch(InterruptedException e) {
        // ignore
      }
    }
    Assert.fail("Timeout while waiting for completion of launches");
  }

  private static boolean hasActiveLaunch() {
    ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
    if(launches != null) {
      for(ILaunch launch : launches) {
        if(!launch.isTerminated()) {
          return true;
        }
      }
    }
    return false;
  }

}
