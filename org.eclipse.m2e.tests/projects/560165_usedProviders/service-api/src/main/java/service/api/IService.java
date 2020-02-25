/*******************************************************************************
 * Copyright (c) 2020 Metron, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Metron, Inc. - initial API and implementation
 *******************************************************************************/

package service.api;

import java.util.Optional;
import java.util.ServiceLoader;


public interface IService {

  static Optional<IService> getImpl() {
    return ServiceLoader.load(IService.class).findFirst();
  }

  String getMessage();

}
