
package org.maven.ide.eclipse.internal.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.project.IMavenProjectChangedListener;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;


public class TestMavenProjectChangedListener implements IMavenProjectChangedListener {

  public final static List<MavenProjectChangedEvent> events = new ArrayList<MavenProjectChangedEvent>();

  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    TestMavenProjectChangedListener.events.addAll(Arrays.asList(events));
  }

}
