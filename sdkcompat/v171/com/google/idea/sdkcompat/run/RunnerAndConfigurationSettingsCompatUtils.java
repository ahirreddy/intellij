/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.sdkcompat.run;

import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.openapi.util.InvalidDataException;
import org.jdom.Element;

/** SDK compatibility bridge for {@link RunnerAndConfigurationSettingsImpl}. */
public class RunnerAndConfigurationSettingsCompatUtils {

  public static void readConfiguration(RunnerAndConfigurationSettingsImpl settings, Element element)
      throws InvalidDataException {
    settings.readExternal(element);
  }
}
