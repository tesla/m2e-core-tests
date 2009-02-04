cd ../org.maven.ide.eclipse.parent
mvn clean install -B  -Dtycho.targetPlatform=C:/platforms/eclipse-tp-e34/eclipse -Dmaven.test.failure.ignore=true -Dsurefire.useFile=false -Dm2e.system.test=true
