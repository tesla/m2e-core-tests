#!/bin/bash
# Set up an Eclipse 3.5 instance with plug-ins necessary to run integration tests
# Adjust ECLIPSE_DIR to point to your target Eclipse 3.5 installation (this should be an Eclipse Classic SDK with no addons))

ECLIPSE_DIR=/Users/rseddon/test/eclipse/350/eclipse


UPDATE_URL=http://download.eclipse.org/releases/galileo/
WINDOWTESTER_URL=http://download.instantiations.com/WindowTesterPro/integration/latest/update/3.5
SUBVERSIVE_URL=http://subclipse.tigris.org/update_1.6.x/
ASPECTJ_URL=http://download.eclipse.org/tools/ajdt/35/update/

function p2_director {
  $ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director -metadataRepository $1 -artifactRepository $1 -installIU $2
}

# Eclipse Ganymade dependencies
p2_director $UPDATE_URL org.eclipse.gef.feature.group
p2_director $UPDATE_URL org.eclipse.zest.sdk.feature.group
p2_director $UPDATE_URL org.eclipse.emf.feature.group
p2_director $UPDATE_URL org.eclipse.xsd.feature.group
p2_director $UPDATE_URL org.eclipse.datatools.sdk.feature.feature.group
p2_director $UPDATE_URL org.eclipse.wst.web_core.feature.feature.group
p2_director $UPDATE_URL org.eclipse.wst.web_ui.feature.feature.group
p2_director $UPDATE_URL org.eclipse.mylyn_feature.feature.group
p2_director $UPDATE_URL org.eclipse.jst.web_ui.feature.feature.group
p2_director $UPDATE_URL org.eclipse.jst.server_adapters.feature.feature.group
p2_director $UPDATE_URL org.eclipse.jst.server_ui.feature.feature.group
p2_director $UPDATE_URL org.eclipse.jst.enterprise_core.feature.feature.group

# Subversive
p2_director $SUBVERSIVE_URL org.tigris.subversion.subclipse.feature.group
p2_director $SUBVERSIVE_URL org.tigris.subversion.clientadapter.svnkit.feature.feature.group
#p2_director $SUBVERSIVE_URL org.tigris.subversion.subclipse.mylyn

# AspectJ
p2_director $ASPECTJ_URL org.eclipse.ajdt.feature.group

# WindowTester
p2_director $WINDOWTESTER_URL com.instantiations.eclipse.shared.feature.group
p2_director $WINDOWTESTER_URL com.instantiations.eclipse.core.feature.feature.group
p2_director $WINDOWTESTER_URL com.instantiations.eclipse.coverage.feature.feature.group
p2_director $WINDOWTESTER_URL com.windowtester.ide.feature.group
p2_director $WINDOWTESTER_URL com.windowtester.ide.gef.feature.group
p2_director $WINDOWTESTER_URL com.windowtester.launcher.feature.group
p2_director $WINDOWTESTER_URL com.windowtester.runtime.feature.group
p2_director $WINDOWTESTER_URL com.windowtester.runtime.gef.feature.group

$ECLIPSE_DIR/eclipse -nosplash -initialize -clean


