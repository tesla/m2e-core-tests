#!/bin/bash
# Set up an Eclipse 3.4 instance with plug-ins necessary to run integration tests
# Adjust ECLIPSE_DIR to point to your target Eclipse 3.4 installation (this should be an Eclipse Classic SDK with no addons))

ECLIPSE_DIR=/Users/hudson/platforms/eclipse-tp-e34/eclipse


UPDATE_URL=http://download.eclipse.org/releases/ganymede/
WINDOWTESTER_URL=http://download.instantiations.com/WindowTesterPro/integration/latest/update/3.4
SUBVERSIVE_URL=http://subclipse.tigris.org/update_1.6.x/
ASPECTJ_URL=http://download.eclipse.org/tools/ajdt/34/update/

function p2_director {
  $ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director -metadataRepository $1 -artifactRepository $1 -installIU $2
}

# Eclipse Ganymade dependencies
p2_director $UPDATE_URL org.eclipse.gef.feature.group
p2_director $UPDATE_URL org.eclipse.zest.sdk.feature.group
p2_director $UPDATE_URL org.eclipse.emf.feature.group
p2_director $UPDATE_URL org.eclipse.xsd.feature.group
p2_director $UPDATE_URL org.eclipse.datatools.sdk.feature.feature.group
p2_director $UPDATE_URL org.eclipse.wst.feature.group
p2_director $UPDATE_URL org.eclipse.mylyn_feature.feature.group
p2_director $UPDATE_URL org.eclipse.jst.feature.group

# Subversive
p2_director $SUBVERSIVE_URL org.tigris.subversion.subclipse.feature.group
p2_director $SUBVERSIVE_URL org.tigris.subversion.clientadapter.svnkit.feature.feature.group
p2_director $SUBVERSIVE_URL org.tigris.subversion.subclipse.mylyn

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


