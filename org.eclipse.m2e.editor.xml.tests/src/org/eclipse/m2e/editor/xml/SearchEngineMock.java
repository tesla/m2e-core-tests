
package org.eclipse.m2e.editor.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.m2e.core.ui.internal.search.util.ArtifactInfo;
import org.eclipse.m2e.core.ui.internal.search.util.Packaging;
import org.eclipse.m2e.core.ui.internal.search.util.SearchEngine;


class SearchEngineMock implements SearchEngine {
  private Set<ArtifactInfo> artifacts = new LinkedHashSet<>();

  void addArtifact(String groupId, String artifactId, String version, String classfier, String type) {
    artifacts.add(new ArtifactInfo(groupId, artifactId, version, classfier, type));
  }

  @Override
public Collection<String> findGroupIds(String searchExpression, Packaging packaging, ArtifactInfo containingArtifact) {
    return null;
  }

  @Override
public Collection<String> findArtifactIds(String groupId, String searchExpression, Packaging packaging,
      ArtifactInfo containingArtifact) {
    Collection<String> result = new ArrayList<>();
    for(ArtifactInfo artifact : artifacts) {
      if(artifact.getGroupId().equals(groupId) && artifact.getArtifactId().startsWith(searchExpression)) {
        result.add(artifact.getArtifactId());
      }
    }
    return result;
  }

  @Override
public Collection<String> findVersions(String groupId, String artifactId, String searchExpression, Packaging packaging) {
    Collection<String> result = new ArrayList<>();
    for(ArtifactInfo artifact : artifacts) {
      if(artifact.getGroupId().equals(groupId) && artifact.getArtifactId().equals(artifactId)
          && artifact.getVersion().startsWith(searchExpression)) {
        result.add(artifact.getVersion());
      }
    }
    return result;
  }

  @Override
public Collection<String> findClassifiers(String groupId, String artifactId, String version, String prefix,
      Packaging packaging) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
public Collection<String> findTypes(String groupId, String artifactId, String version, String prefix,
      Packaging packaging) {
    // TODO Auto-generated method stub
    return null;
  }

}
