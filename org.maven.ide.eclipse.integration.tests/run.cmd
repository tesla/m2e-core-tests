
rem
rem Example script which runs the integration tests.  "tycho.targetPlatform" needs to point to an appropriately set up Eclipse, see setupTarget.cmd 
rem

set M2_HOME=c:\tycho
set PATH=%M2_HOME%\bin;%PATH%

cd ../org.maven.ide.eclipse.parent
mvn clean install -fn -B  -Dtycho.targetPlatform=C:/platforms/eclipse-tp-e34/eclipse -Dmaven.test.failure.ignore=true -Dsurefire.useFile=false -Dm2e.system.test=true
