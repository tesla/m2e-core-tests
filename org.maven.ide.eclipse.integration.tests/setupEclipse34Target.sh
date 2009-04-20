#!/bin/bash
# Set up an Eclipse 3.4 instance with plug-ins necessary to run integration tests
# Adjust ECLIPSE_DIR to point to your target Eclipse 3.4 installation (this should be an Eclipse Classic SDK with no addons))

ECLIPSE_DIR=/home/hudson/platforms/eclipse34


# Eclipse Ganymade dependencies
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://download.eclipse.org/releases/ganymede/ -artifactRepository http://download.eclipse.org/releases/ganymede/ -installIU org.eclipse.gef.feature.group
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://download.eclipse.org/releases/ganymede/ -artifactRepository http://download.eclipse.org/releases/ganymede/ -installIU org.eclipse.zest.sdk.feature.group
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://download.eclipse.org/releases/ganymede/ -artifactRepository http://download.eclipse.org/releases/ganymede/ -installIU org.eclipse.emf.feature.group
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://download.eclipse.org/releases/ganymede/ -artifactRepository http://download.eclipse.org/releases/ganymede/ -installIU org.eclipse.xsd.feature.group
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://download.eclipse.org/releases/ganymede/ -artifactRepository http://download.eclipse.org/releases/ganymede/ -installIU org.eclipse.datatools.sdk.feature.feature.group
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://download.eclipse.org/releases/ganymede/ -artifactRepository http://download.eclipse.org/releases/ganymede/ -installIU org.eclipse.wst.feature.group
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://download.eclipse.org/releases/ganymede/ -artifactRepository http://download.eclipse.org/releases/ganymede/ -installIU org.eclipse.mylyn_feature.feature.group
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://download.eclipse.org/releases/ganymede/ -artifactRepository http://download.eclipse.org/releases/ganymede/ -installIU org.eclipse.jst.feature.group

# Subversive
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://subclipse.tigris.org/update_1.6.x/ -artifactRepository http://subclipse.tigris.org/update_1.6.x/ -installIU org.tigris.subversion.subclipse.feature.group
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://subclipse.tigris.org/update_1.6.x/ -artifactRepository http://subclipse.tigris.org/update_1.6.x/ -installIU org.tigris.subversion.clientadapter.svnkit.feature.feature.group
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://subclipse.tigris.org/update_1.6.x/ -artifactRepository http://subclipse.tigris.org/update_1.6.x/ -installIU org.tigris.subversion.subclipse.mylyn

# AspectJ
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://download.eclipse.org/tools/ajdt/34/update/ -artifactRepository http://download.eclipse.org/tools/ajdt/34/update/ -installIU org.eclipse.ajdt.feature.group

# WindowTester
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://update.instantiations.com/WindowTesterPro/E-3.4/ -artifactRepository http://update.instantiations.com/WindowTesterPro/E-3.4/ -installIU com.instantiations.eclipse.shared.feature.group
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://update.instantiations.com/WindowTesterPro/E-3.4/ -artifactRepository http://update.instantiations.com/WindowTesterPro/E-3.4/ -installIU com.instantiations.eclipse.core.feature.feature.group
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://update.instantiations.com/WindowTesterPro/E-3.4/ -artifactRepository http://update.instantiations.com/WindowTesterPro/E-3.4/ -installIU com.instantiations.eclipse.coverage.feature.feature.group
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://update.instantiations.com/WindowTesterPro/E-3.4/ -artifactRepository http://update.instantiations.com/WindowTesterPro/E-3.4/ -installIU com.windowtester.ide.feature.group
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://update.instantiations.com/WindowTesterPro/E-3.4/ -artifactRepository http://update.instantiations.com/WindowTesterPro/E-3.4/ -installIU com.windowtester.ide.gef.feature.group
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://update.instantiations.com/WindowTesterPro/E-3.4/ -artifactRepository http://update.instantiations.com/WindowTesterPro/E-3.4/ -installIU com.windowtester.launcher.feature.group
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://update.instantiations.com/WindowTesterPro/E-3.4/ -artifactRepository http://update.instantiations.com/WindowTesterPro/E-3.4/ -installIU com.windowtester.runtime.feature.group
$ECLIPSE_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://update.instantiations.com/WindowTesterPro/E-3.4/ -artifactRepository http://update.instantiations.com/WindowTesterPro/E-3.4/ -installIU com.windowtester.runtime.gef.feature.group

$ECLIPSE_DIR/eclipse -nosplash -initialize -clean
