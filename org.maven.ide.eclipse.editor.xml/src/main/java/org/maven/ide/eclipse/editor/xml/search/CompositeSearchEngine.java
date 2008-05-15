/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.maven.ide.eclipse.editor.xml.Log;


/**
 * Search engine composed from everal other SearchEngines. Calls appropriate methods in its component SearchEngines add
 * puts results to TreeSet
 * 
 * @author Lukas Krecan
 */
public class CompositeSearchEngine implements SearchEngine {
  private static final VersionComparator VERSION_COMPARATOR = new VersionComparator();

  private List<SearchEngine> components = new ArrayList<SearchEngine>();

  private Log log = Log.NULL_LOG;

  /**
   * Callback to be called for every component.
   * 
   * @author Lukas Krecan
   */
  public static interface SearchCallback {
    public Collection<String> search(SearchEngine searchEngine);
  }

  protected SortedSet<String> findInternal(SearchCallback callback) {
    return findInternal(new TreeSet<String>(), callback);
  }

  /**
   * Iterates over components and call callback for every component. Stores results to the provided result set.
   * 
   * @param result
   * @param callback
   * @return result Set
   */
  protected SortedSet<String> findInternal(SortedSet<String> result, SearchCallback callback) {
    for(SearchEngine component : components) {
      try {
        result.addAll(callback.search(component));
      } catch(Exception e) {
        log.logError("Error when searching", e);
      }
    }
    return result;
  }

  public SortedSet<String> findArtifactIds(final String groupId, final String searchExpression, final Packaging packaging, final ArtifactInfo containingArtifact) {
    return findInternal(new SearchCallback() {
      public Collection<String> search(SearchEngine searchEngine) {
        return searchEngine.findArtifactIds(groupId, searchExpression, packaging, containingArtifact);
      }
    });
  }

  public SortedSet<String> findGroupIds(final String searchExpression, final Packaging packaging, final ArtifactInfo containingArtifact) {
    return findInternal(new SearchCallback() {
      public Collection<String> search(SearchEngine searchEngine) {
        return searchEngine.findGroupIds(searchExpression, packaging, containingArtifact);
      }
    });
  }

  public SortedSet<String> findVersions(final String groupId, final String artifactId, final String searchExpression, final Packaging packaging) {
    //I like closures :-/
    return findInternal(new TreeSet<String>(VERSION_COMPARATOR), new SearchCallback() {
      public Collection<String> search(SearchEngine searchEngine) {
        return searchEngine.findVersions(groupId, artifactId, searchExpression, packaging);
      }
    });
  }

  public SortedSet<String> findClassifiers(final String groupId, final String artifactId, final String version, final String prefix, final Packaging packaging) {
      return findInternal(new SearchCallback() {
        public Collection<String> search(SearchEngine searchEngine) {
          return searchEngine.findClassifiers(groupId, artifactId, version, prefix, packaging);
        }
      });
  }
  
  public SortedSet<String> findTypes(final String groupId, final String artifactId, final String version, final String prefix, final Packaging packaging) {
    return findInternal(new SearchCallback() {
      public Collection<String> search(SearchEngine searchEngine) {
        return searchEngine.findTypes(groupId, artifactId, version, prefix, packaging);
      }
    });
  }
  
  public void addSearchEngine(SearchEngine component) {
    components.add(component);
  }

  public Log getLog() {
    return log;
  }

  public void setLog(Log log) {
    this.log = log;
  }



}
