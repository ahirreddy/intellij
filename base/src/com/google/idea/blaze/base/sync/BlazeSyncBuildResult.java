/*
 * Copyright 2019 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.base.sync;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.idea.blaze.base.logging.utils.BuildPhaseSyncStats;
import com.google.idea.blaze.base.sync.aspects.BlazeBuildOutputs;
import com.google.idea.blaze.base.sync.aspects.BuildResult.Status;
import javax.annotation.Nullable;

/**
 * All the information gathered during the build phase of sync, used as input to the project update
 * phase.
 */
@AutoValue
public abstract class BlazeSyncBuildResult {

  /**
   * Merges this {@link BlazeSyncBuildResult} with the results of a more recent build, prior to the
   * project update phase.
   */
  public BlazeSyncBuildResult updateResult(BlazeSyncBuildResult nextResult) {
    // take the most recent version of the project data, and combine the blaze build outputs
    // TODO(brendandouglas): properly combine failed and successful builds (don't throw away results
    // entirely)
    return nextResult.toBuilder()
        .setProjectState(getProjectState().updateState(nextResult.getProjectState()))
        .setBuildResult(getBuildResult().updateOutputs(nextResult.getBuildResult()))
        .setBuildPhaseStats(
            ImmutableList.<BuildPhaseSyncStats>builder()
                .addAll(getBuildPhaseStats())
                .addAll(nextResult.getBuildPhaseStats())
                .build())
        .build();
  }

  /** Returns false if this build result is incomplete or invalid. */
  public boolean isValid() {
    return getProjectState() != null
        && getBuildResult() != null
        && getBuildResult().buildResult.status != Status.FATAL_ERROR;
  }

  @Nullable
  public abstract SyncProjectState getProjectState();

  @Nullable
  public abstract BlazeBuildOutputs getBuildResult();

  public abstract ImmutableList<BuildPhaseSyncStats> getBuildPhaseStats();

  public static Builder builder() {
    return new AutoValue_BlazeSyncBuildResult.Builder().setBuildPhaseStats(ImmutableList.of());
  }

  private Builder toBuilder() {
    return builder()
        .setProjectState(getProjectState())
        .setBuildPhaseStats(getBuildPhaseStats())
        .setBuildResult(getBuildResult());
  }

  /** A builder for {@link BlazeSyncBuildResult} objects. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setProjectState(SyncProjectState projectState);

    public abstract Builder setBuildResult(BlazeBuildOutputs buildResult);

    public abstract Builder setBuildPhaseStats(Iterable<BuildPhaseSyncStats> stats);

    public abstract BlazeSyncBuildResult build();
  }
}
