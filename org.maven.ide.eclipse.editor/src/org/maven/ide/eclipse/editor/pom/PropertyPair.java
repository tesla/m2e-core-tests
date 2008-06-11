package org.maven.ide.eclipse.editor.pom;

/**
 * Property key/value holder
 */
public class PropertyPair {
  private String key;
  private String value;

  public PropertyPair(String key, String value) {
    this.key = key;
    this.value = value;
  }
  
  public String getKey() {
    return key;
  }
  
  public String getValue() {
    return value;
  }

}