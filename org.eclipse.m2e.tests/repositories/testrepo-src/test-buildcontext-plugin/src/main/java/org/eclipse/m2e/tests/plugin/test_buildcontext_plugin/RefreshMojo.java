package org.eclipse.m2e.tests.plugin.test_buildcontext_plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Goal sending a refresh to the build context
 */
@Mojo( name = "refresh", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresProject=true )
public class RefreshMojo extends AbstractMojo {
	@Parameter( defaultValue = "${project.basedir}", property = "directory", required = true )
	private File directory;

	@Component
	private BuildContext buildContext;

	public void execute() throws MojoExecutionException {
		try {
			Files.write(new File(directory, "refreshTest" + System.currentTimeMillis()).toPath(), "done".getBytes(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		buildContext.refresh(directory);
	}
}
