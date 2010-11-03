/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

/*
 * Copyright 2001-2006 Codehaus Foundation.
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

/**
 * Indicates an error during <code>Launcher</code> configuration.
 * 
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 */
public class ConfigurationException extends Exception {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  /**
   * Construct.
   * 
   * @param msg The message.
   */
  public ConfigurationException(String msg) {
    super(msg);
  }

  /**
   * Construct.
   * 
   * @param msg The message.
   * @param lineNo The number of configuration line where the problem occurred.
   * @param line The configuration line where the problem occurred.
   */
  public ConfigurationException(String msg, int lineNo, String line) {
    super(msg + " (" + lineNo + "): " + line); //$NON-NLS-1$ //$NON-NLS-2$
  }

  public ConfigurationException(Exception cause) {
    super(cause);
  }
}
