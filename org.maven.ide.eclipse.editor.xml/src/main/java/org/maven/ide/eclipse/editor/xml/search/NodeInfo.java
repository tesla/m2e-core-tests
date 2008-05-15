package org.maven.ide.eclipse.editor.xml.search;

/**
 * Information about the current node in the pom file.
 * @author Lukas Krecan
 *
 */
public interface NodeInfo {
  public String getGroupId();
  
  public String getArtifactId();
  
  public String getVersion();
  
  public String getPrefix();
  /**
   * 
   * Returns containing artifactInfo for exclusions. Otherwise returns null.
   * @return
   */
  public ArtifactInfo getContainingArtifact();

  /**
   * Returns required packaging.
   * @return
   */
  public Packaging getPackaging();
}
