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
package com.google.idea.blaze.android.cppimpl;

import static com.jetbrains.cidr.lang.OCLanguage.LANGUAGE_SUPPORT_DISABLED;

import com.android.tools.ndk.NdkHelper;
import com.google.idea.blaze.base.model.BlazeProjectData;
import com.google.idea.blaze.base.model.primitives.LanguageClass;
import com.google.idea.blaze.base.projectview.ProjectViewSet;
import com.google.idea.blaze.base.scope.BlazeContext;
import com.google.idea.blaze.base.settings.BlazeImportSettings;
import com.google.idea.blaze.base.sync.BlazeSyncParams.SyncMode;
import com.google.idea.blaze.base.sync.SyncListener;
import com.google.idea.sdkcompat.cidr.OCWorkspaceProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.project.Project;
import com.jetbrains.cidr.lang.workspace.OCWorkspace;
import com.jetbrains.cidr.lang.workspace.OCWorkspaceModificationTrackers;

final class BlazeNdkSupportEnabler implements SyncListener {

  @Override
  public void onSyncComplete(
      Project project,
      BlazeContext context,
      BlazeImportSettings importSettings,
      ProjectViewSet projectViewSet,
      BlazeProjectData blazeProjectData,
      SyncMode syncMode,
      SyncResult syncResult) {
    boolean enabled =
        blazeProjectData.getWorkspaceLanguageSettings().isLanguageActive(LanguageClass.C);
    enableCSupportInIde(project, enabled);
  }

  /**
   * If {@code enabled} is true, this method will enable C support in the IDE if it is not already
   * enabled. if {@code enabled} is false this method will clear out any currently stored
   * information in the IDE about C and will disable C support in the IDE, unless support is already
   * disabled. In either case, if the value of enabled matches what the IDE currently does, this
   * method will do nothing.
   *
   * @param project the project to enable or disable c support in.
   * @param enabled if true, turn on C support in the IDE. If false, turn off C support in the IDE.
   */
  private static void enableCSupportInIde(Project project, boolean enabled) {
    OCWorkspace workspace = OCWorkspaceProvider.getWorkspace(project);
    if (workspace == null) {
      // NDK workspace manager plugin isn't enabled.
      return;
    }
    Boolean isCurrentlyEnabled = !LANGUAGE_SUPPORT_DISABLED.get(project, false);
    if (isCurrentlyEnabled != enabled) {
      NdkHelper.disableCppLanguageSupport(project, !enabled);
      rebuildSymbols(project);
    }
  }

  private static void rebuildSymbols(Project project) {
    TransactionGuard.getInstance()
        .submitTransactionLater(
            project,
            () ->
                ApplicationManager.getApplication().runReadAction(() -> doRebuildSymbols(project)));
  }

  private static void doRebuildSymbols(Project project) {
    if (project.isDisposed()) {
      return;
    }
    // Notifying BuildSettingsChangeTracker in unitTestMode will leads to a dead lock.
    // See b/23087433 for more information.
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      OCWorkspaceModificationTrackers.getInstance(project)
          .getBuildSettingsChangesTracker()
          .incModificationCount();
    }
  }
}
