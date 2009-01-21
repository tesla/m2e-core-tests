
set M2_HOME=c:\tycho
set PATH=%M2_HOME%\bin;%PATH%
mvn clean install -B  -Pm2e-system-test -Dtycho.targetPlatform=C:/test/eclipse -Dmaven.test.failure.ignore=true -Dsurefire.useFile=false -Pm2e-system-test -Dorg.maven.ide.eclipse.tests.skip=true -Dorg.maven.ide.eclipse.integration.tests.skip=false


