
set ECLIPSE_DIR=c:\platforms\eclipse-tp-e35\eclipse

set UPDATE_URL=http://download.eclipse.org/releases/galileo/
set WINDOWTESTER_URL=http://download.instantiations.com/WindowTesterPro/integration/latest/update/3.5
set SUBCLIPSE_URL=http://subclipse.tigris.org/update_1.6.x/
set ASPECTJ_URL=http://download.eclipse.org/tools/ajdt/35/update/


rem Eclipse Ganymade dependencies
call :P2_DIRECTOR %UPDATE_URL% org.eclipse.gef.feature.group
call :P2_DIRECTOR %UPDATE_URL% org.eclipse.zest.sdk.feature.group
call :P2_DIRECTOR %UPDATE_URL% org.eclipse.emf.feature.group
call :P2_DIRECTOR %UPDATE_URL% org.eclipse.xsd.feature.group
call :P2_DIRECTOR %UPDATE_URL% org.eclipse.datatools.sdk.feature.feature.group
call :P2_DIRECTOR %UPDATE_URL% org.eclipse.wst.feature.group
call :P2_DIRECTOR %UPDATE_URL% org.eclipse.mylyn_feature.feature.group
call :P2_DIRECTOR %UPDATE_URL% org.eclipse.jst.feature.group

rem Subversive
call :P2_DIRECTOR %SUBCLIPSE_URL% org.tigris.subversion.subclipse.feature.group
call :P2_DIRECTOR %SUBCLIPSE_URL% org.tigris.subversion.clientadapter.svnkit.feature.feature.group
rem call :P2_DIRECTOR %SUBCLIPSE_URL% org.tigris.subversion.subclipse.mylyn

rem AspectJ
call :P2_DIRECTOR %ASPECTJ_URL% org.eclipse.ajdt.feature.group

rem WindowTester
call :P2_DIRECTOR %WINDOWTESTER_URL%? com.instantiations.eclipse.shared.feature.group
call :P2_DIRECTOR %WINDOWTESTER_URL%? com.instantiations.eclipse.core.feature.feature.group
call :P2_DIRECTOR %WINDOWTESTER_URL%? com.instantiations.eclipse.coverage.feature.feature.group
call :P2_DIRECTOR %WINDOWTESTER_URL%? com.windowtester.ide.feature.group
call :P2_DIRECTOR %WINDOWTESTER_URL%? com.windowtester.ide.gef.feature.group
call :P2_DIRECTOR %WINDOWTESTER_URL%? com.windowtester.launcher.feature.group
call :P2_DIRECTOR %WINDOWTESTER_URL%? com.windowtester.runtime.feature.group
call :P2_DIRECTOR %WINDOWTESTER_URL%? com.windowtester.runtime.gef.feature.group

%ECLIPSE_DIR%\eclipse.exe -nosplash -initialize -clean


:P2_DIRECTOR
%ECLIPSE_DIR%\eclipse.exe -nosplash -application org.eclipse.equinox.p2.director -metadataRepository %1 -artifactRepository %1 -installIU %2
exit /b
