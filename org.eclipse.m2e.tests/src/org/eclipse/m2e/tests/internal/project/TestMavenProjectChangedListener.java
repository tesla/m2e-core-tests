
package org.eclipse.m2e.tests.internal.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;


public class TestMavenProjectChangedListener implements IMavenProjectChangedListener {

  public final static List<MavenProjectChangedEvent> events = new ArrayList<MavenProjectChangedEvent>();

  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    TestMavenProjectChangedListener.events.addAll(Arrays.asList(events));
  }

}
