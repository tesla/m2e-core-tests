/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.embedder;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.embedder.MavenExecutionContext;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class MavenExecutionContextTest extends AbstractMavenProjectTestCase {

  MavenImpl maven;

  protected void setUp() throws Exception {
    super.setUp();
    maven = (MavenImpl) MavenPlugin.getMaven();
  }

  public void testBasic() throws Exception {
    final MavenExecutionContext context = maven.createExecutionContext();
    context.execute(new ICallable<Void>() {
      public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
        assertNotNull(context.getLocalRepository());
        assertNotNull(context.getRepositorySession());
        assertNotNull(context.getSession());
        return null;
      }
    }, monitor);
  }

  public void testNested() throws Exception {
    final String outerProperty = "outer-property";
    final String nestedProperty = "nested-property";
    final MavenExecutionContext outer = maven.createExecutionContext();
    outer.getExecutionRequest().getUserProperties().put(outerProperty, "true");
    outer.execute(new ICallable<Void>() {
      public Void call(final IMavenExecutionContext outerParam, IProgressMonitor monitor) throws CoreException {
        assertSame(outer, outerParam);
        assertTrue(outerParam.getSession().getUserProperties().containsKey(outerProperty));
        final MavenExecutionContext nested = maven.createExecutionContext();
        assertNotSame(outer, nested);
        assertNotSame(outer.getExecutionRequest(), nested.getExecutionRequest());
        nested.getExecutionRequest().getUserProperties().put(nestedProperty, "false");
        final MavenSession outerSession = outerParam.getSession();
        nested.execute(new ICallable<Void>() {
          public Void call(final IMavenExecutionContext nestedParam, IProgressMonitor monitor) throws CoreException {
            assertSame(nested, nestedParam);
            assertNotSame(outerParam, nestedParam);
            assertNotSame(outerSession, nestedParam.getSession());
            assertTrue(nestedParam.getSession().getUserProperties().containsKey(nestedProperty));
            return null;
          }
        }, monitor);
        // check that nested context did not mess up outer context configuration 
        assertFalse(outerParam.getSession().getUserProperties().containsKey(nestedProperty));
        return null;
      }
    }, monitor);
  }

  public void testReenterShortcut() throws Exception {
    final MavenExecutionContext context = maven.createExecutionContext();
    context.execute(new ICallable<Void>() {
      public Void call(IMavenExecutionContext contextParam, IProgressMonitor monitor) throws CoreException {
        final MavenSession session1 = contextParam.getSession();
        final RepositorySystemSession repositorySession1 = contextParam.getRepositorySession();
        final TransferListener transferListener1 = ((IMavenExecutionContext) context).getRepositorySession()
            .getTransferListener();
        contextParam.execute(new ICallable<Void>() {
          public Void call(IMavenExecutionContext contextParam2, IProgressMonitor monitor) throws CoreException {
            assertSame(session1, contextParam2.getSession());
            assertSame(repositorySession1, contextParam2.getRepositorySession());
            assertNotSame(transferListener1, contextParam2.getRepositorySession().getTransferListener());
            return null;
          }
        }, monitor);
        // transfer listener is restored
        assertSame(transferListener1, contextParam.getRepositorySession().getTransferListener());
        return null;
      }
    }, monitor);
  }

  public void testIllegalState() throws Exception {
    final MavenExecutionContext context = maven.createExecutionContext();

    try {
      context.getLocalRepository();
      fail();
    } catch(IllegalStateException expected) {
    }

    try {
      context.getRepositorySession();
      fail();
    } catch(IllegalStateException expected) {
    }

    try {
      context.getSession();
      fail();
    } catch(IllegalStateException expected) {
    }

    try {
      context.execute(new ICallable<Void>() {
        public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
          context.getExecutionRequest().addActiveProfile("profile");
          return null;
        }
      }, monitor);
      fail();
    } catch(IllegalStateException expected) {
    }
  }

  public void testExecutionRequestContainsSystemProperties() throws Exception {
    maven.execute(new ICallable<Void>() {
      public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
        MavenExecutionRequest request = context.getExecutionRequest();
        assertNotNull(request);
        assertNotNull(request.getSystemProperties());
        assertNotNull(request.getSystemProperties().getProperty("java.version"));
        assertNotNull(request.getSystemProperties().getProperty("java.home"));
        return null;
      }
    }, monitor);
  }
}
