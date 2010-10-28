
rem
rem Example script which runs the integration tests. 
rem

set M2_HOME=c:\tycho
set PATH=%M2_HOME%\bin;%PATH%

cd ../org.eclipse.m2e.parent
mvn clean install -B -Dsurefire.useFile=false -Dtarget.platform=m2e-e34 -Dm2e.system.test=true

