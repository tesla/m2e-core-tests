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

package org.eclipse.m2e.jdt.internal.search;

import java.util.Collections;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jdt.ui.search.IQueryParticipant;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.jdt.ui.search.PatternQuerySpecification;
import org.eclipse.jdt.ui.search.QuerySpecification;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.actions.OpenPomAction;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexManager;
import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.ui.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.jdt.internal.Messages;


/**
 * Java search query participant
 * 
 * @author Eugene Kuleshov
 * @see org.eclipse.jdt.ui.queryParticipants extension point
 */
public class MavenQueryParticipant implements IQueryParticipant, IJavaSearchConstants {

  private static final int TYPES = TYPE | CLASS | INTERFACE | ENUM | ANNOTATION_TYPE | CLASS_AND_ENUM
      | CLASS_AND_INTERFACE /* | INTERFACE_AND_ANNOTATION */;

  public int estimateTicks(QuerySpecification specification) {
    return 1000;
  }

  public IMatchPresentation getUIParticipant() {
    return new IMatchPresentation() {

      public ILabelProvider createLabelProvider() {
        return new LabelProvider() {
          public String getText(Object element) {
            if(element instanceof IndexedArtifact) {
              IndexedArtifact ia = (IndexedArtifact) element;
              return ia.getPackageName() + "." + ia.getClassname() + " - " + ia.getGroupId() + ":" + ia.getArtifactId();  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            return null;
          }
        };
      }

      public void showMatch(Match match, int currentOffset, int currentLength, boolean activate) {
        final IndexedArtifact ia = (IndexedArtifact) match.getElement();
        
        final String className = ia.getClassname();
        
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(new Shell(Display.getCurrent()), //
                Messages.MavenQueryParticipant_searchDialog_title, 
                IIndex.SEARCH_CLASS_NAME, Collections.<ArtifactKey>emptySet());
            dialog.setQuery(className);
            if(dialog.open() == Window.OK) {
              final IndexedArtifact ia = dialog.getSelectedIndexedArtifact();
              final IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
              String name = af.group + ":" + af.artifact + ":" + af.version; //$NON-NLS-1$ //$NON-NLS-2$
              new Job(NLS.bind(Messages.MavenQueryParticipant_job_name, name)) {
                protected IStatus run(IProgressMonitor monitor) {
                  OpenPomAction.openEditor(ia, af, monitor);
                  return Status.OK_STATUS;
                }
              };
            }
          }
        });
        
      }
    };
  }

  public void search(ISearchRequestor requestor, QuerySpecification querySpecification, IProgressMonitor monitor) {
    // IJavaSearchScope scope = querySpecification.getScope();
    if((querySpecification.getLimitTo() & TYPES) > 0) {
      IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();

      String term = null;
      if(querySpecification instanceof ElementQuerySpecification) {
        ElementQuerySpecification spec = (ElementQuerySpecification) querySpecification;
        IJavaElement element = spec.getElement();
        term = element == null ? null : element.getElementName();
      } else if(querySpecification instanceof PatternQuerySpecification) {
        term = ((PatternQuerySpecification) querySpecification).getPattern();
      }

      if(term != null) {
        try {
          Map<String, IndexedArtifact> result = indexManager.search(term, IIndex.SEARCH_CLASS_NAME);
          for(IndexedArtifact ia : result.values()) {
            requestor.reportMatch(new Match(ia, 0, 0));
          }
        } catch(CoreException ex) {
          MavenLogger.log(ex);
        }
      }
    }
  }

}
