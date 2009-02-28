#!/bin/bash

# Sanity check for m2e update site, installs each feature individually. 
# This must be run manually, when there is an error it fails with Eclipse throwing up an error dialog (and script hangs).

#TODO: Should probably pull the features out of content.xml programatically.

UPDATE_URL=http://m2eclipse.sonatype.org/update
ECLIPSE_TARBALL=./eclipse-SDK-3.4.2-macosx-carbon.tar.gz

feature_groups=( 'org.maven.ide.eclipse.wtp.feature.feature.group' 'org.maven.ide.eclipse.cvs.feature.feature.group' 'org.maven.ide.eclipse.feature.feature.group' 'org.maven.ide.eclipse.editor.feature.feature.group' 'org.maven.ide.eclipse.editor.xml.feature.feature.group' 'org.maven.ide.eclipse.central.feature.feature.group'  'org.maven.ide.eclipse.book.feature.feature.group' 'org.maven.ide.eclipse.doxia.feature.feature.group' 'org.maven.ide.eclipse.scm.feature.feature.group' 'org.maven.ide.components.maven_embedder.feature.feature.group' 'org.maven.ide.eclipse.mylyn3.feature.feature.group' )

for feature_group in ${feature_groups[@]}
do
echo $feature_group
rm -fr eclipse
tar xvf $ECLIPSE_TARBALL
./eclipse/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository $UPDATE_URL -artifactRepository $UPDATE_URL -installIU $feature_group
done

# Subclipse
rm -fr eclipse
tar xvf $ECLIPSE_TARBALL
./eclipse/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://subclipse.tigris.org/update_1.4.x/ -artifactRepository http://subclipse.tigris.org/update_1.4.x/ -installIU org.tigris.subversion.subclipse.feature.group

./eclipse/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository $UPDATE_URL -artifactRepository $UPDATE_URL -installIU org.maven.ide.eclipse.subclipse.feature.feature.group

# AspectJ
rm -fr eclipse
tar xvf $ECLIPSE_TARBALL
./eclipse/eclipse -nosplash -application org.eclipse.equinox.p2.director.app.application -metadataRepository http://download.eclipse.org/tools/ajdt/34/update/ -artifactRepository http://download.eclipse.org/tools/ajdt/34/update/ -installIU org.eclipse.ajdt.feature.group

rm -fr eclipse
