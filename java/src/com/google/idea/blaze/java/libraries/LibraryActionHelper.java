/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.java.libraries;

import com.google.idea.blaze.base.model.BlazeProjectData;
import com.google.idea.blaze.base.model.LibraryKey;
import com.google.idea.blaze.base.sync.data.BlazeProjectDataManager;
import com.google.idea.blaze.java.sync.model.BlazeJarLibrary;
import com.google.idea.blaze.java.sync.model.BlazeJavaSyncData;
import com.intellij.ide.projectView.impl.nodes.NamedLibraryElementNode;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.Navigatable;
import javax.annotation.Nullable;
import com.google.idea.blaze.base.scope.scopes.IdeaLogScope;

class LibraryActionHelper {

  private static final IdeaLogScope logger = new IdeaLogScope();

  @Nullable
  static BlazeJarLibrary findLibraryFromIntellijLibrary(
      Project project, BlazeProjectData blazeProjectData, Library library) {
    String libName = library.getName();
    if (libName == null) {
      return null;
    }
    LibraryKey libraryKey = LibraryKey.fromIntelliJLibraryName(libName);
    BlazeJavaSyncData syncData = blazeProjectData.syncState.get(BlazeJavaSyncData.class);
    if (syncData == null) {
      Messages.showErrorDialog(project, "Project isn't synced. Please resync project.", "Error");
      return null;
    }
    return syncData.importResult.libraries.get(libraryKey);
  }

  @Nullable
  static BlazeJarLibrary findBlazeLibraryForAction(Project project, AnActionEvent e) {
    Library library = findLibraryForAction(e);
    if (library == null) {
      return null;
    }
    BlazeProjectData projectData =
        BlazeProjectDataManager.getInstance(project).getBlazeProjectData();
    return projectData != null
        ? findLibraryFromIntellijLibrary(project, projectData, library)
        : null;
  }

  @Nullable
  static Library findLibraryForAction(AnActionEvent e) {
    logger.info("FIND LIBRARY 12345: " + e);
    Project project = e.getProject();
    logger.info("PROJECT 12345: " + project);
    if (project != null) {
      NamedLibraryElementNode node = findLibraryNode(e.getDataContext());
      logger.info("NODE 12345: " + node);
      if (node != null) {
        String libraryName = node.getName();
        logger.info("LIB NAME 12345: " + libraryName);
        if (StringUtil.isNotEmpty(libraryName)) {
          LibraryTable libraryTable = ProjectLibraryTable.getInstance(project);
          logger.info("LIB TABLE 12345: " + libraryTable);
          logger.info("LIB OBJECT 12345: " + libraryTable.getLibraryByName(libraryName));
          return libraryTable.getLibraryByName(libraryName);
        }
      }
    }
    return null;
  }

  @Nullable
  private static NamedLibraryElementNode findLibraryNode(DataContext dataContext) {
    Navigatable[] navigatables = CommonDataKeys.NAVIGATABLE_ARRAY.getData(dataContext);
    if (navigatables != null && navigatables.length == 1) {
      Navigatable navigatable = navigatables[0];
      if (navigatable instanceof NamedLibraryElementNode) {
        return (NamedLibraryElementNode) navigatable;
      }
    }
    return null;
  }
}
