package org.eclipse.m2e.editor.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.m2e.core.util.search.ArtifactInfo;
import org.eclipse.m2e.core.util.search.Packaging;
import org.eclipse.m2e.core.util.search.SearchEngine;

class SearchEngineMock implements SearchEngine {
  private Set<ArtifactInfo> artifacts = new LinkedHashSet<ArtifactInfo>();

  void addArtifact(String groupId, String artifactId, String version, String classfier, String type) {
    artifacts.add(new ArtifactInfo(groupId, artifactId, version, classfier, type));
  }

  public Collection<String> findGroupIds(String searchExpression, Packaging packaging, ArtifactInfo containingArtifact) {
    // TODO Auto-generated method stub
    return null;
  }

  public Collection<String> findArtifactIds(String groupId, String searchExpression, Packaging packaging,
      ArtifactInfo containingArtifact) {
    Collection<String> result = new ArrayList<String>();
    for(ArtifactInfo artifact : artifacts) {
      if(artifact.getGroupId().equals(groupId) && artifact.getArtifactId().startsWith(searchExpression)) {
        result.add(artifact.getArtifactId());
      }
    }
    return result;
  }

  public Collection<String> findVersions(String groupId, String artifactId, String searchExpression,
      Packaging packaging) {
    Collection<String> result = new ArrayList<String>();
    for(ArtifactInfo artifact : artifacts) {
      if(artifact.getGroupId().equals(groupId) && artifact.getArtifactId().equals(artifactId)
          && artifact.getVersion().startsWith(searchExpression)) {
        result.add(artifact.getVersion());
      }
    }
    return result;
  }

  public Collection<String> findClassifiers(String groupId, String artifactId, String version, String prefix,
      Packaging packaging) {
    // TODO Auto-generated method stub
    return null;
  }

  public Collection<String> findTypes(String groupId, String artifactId, String version, String prefix,
      Packaging packaging) {
    // TODO Auto-generated method stub
    return null;
  }

}