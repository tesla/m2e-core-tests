package org.maven.ide.eclipse.ui.internal.preferences;

import org.eclipse.core.runtime.CoreException;

import org.maven.ide.eclipse.internal.lifecycle.LifecycleMappingPropertyPageFactory;
import org.maven.ide.eclipse.internal.project.MissingLifecycleMapping;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;

public class MissingLifecycleMappingPropertyPage extends SimpleLifecycleMappingPropertyPage {

  public MissingLifecycleMappingPropertyPage() {
    super("Missing lifecycle mapping");
  }

  protected String getMessage() {
    try {
      ILifecycleMapping lifecycleMapping = LifecycleMappingPropertyPageFactory.getLifecycleMapping(getProject());
      if (lifecycleMapping instanceof MissingLifecycleMapping) {
        String missingId = ((MissingLifecycleMapping) lifecycleMapping).getMissingMappingId();
        return "Unknown or missing lifecycle mapping with id=`" + missingId + "'. \nCheck the spelling and/or install required Eclipse plugins.";
      }
    } catch(CoreException ex) {
      // this is odd, but lets ignore it anyways
    }
    return super.getMessage();
  }
}
