package org.eclipse.m2e.editor.xml;

import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.editor.xml.mojo.IMojoParameterMetadata;
import org.eclipse.m2e.editor.xml.mojo.MojoParameter;
import org.eclipse.m2e.editor.xml.mojo.PlexusConfigHelper;

public class TestMojoParameterMetadata implements IMojoParameterMetadata {

    @Override
    public List<MojoParameter> loadMojoParameters(PluginDescriptor desc, MojoDescriptor mojo, PlexusConfigHelper helper,
            IProgressMonitor monitor) throws CoreException {
        MojoParameter param1 = new MojoParameter("test1", "String");
        MojoParameter param2 = new MojoParameter("test2", "String");
        return Arrays.asList(param1, param2);
    }

}
