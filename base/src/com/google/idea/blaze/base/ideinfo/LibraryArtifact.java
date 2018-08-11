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
package com.google.idea.blaze.base.ideinfo;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.idea.blaze.base.scope.scopes.IdeaLogScope;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import javax.annotation.Nullable;
import java.util.Arrays;

/** Represents a jar artifact. */
public class LibraryArtifact implements Serializable {
  private static final long serialVersionUID = 3L;

  @Nullable public final ArtifactLocation interfaceJar;
  @Nullable public final ArtifactLocation classJar;
  public final ImmutableList<ArtifactLocation> sourceJars;

  public LibraryArtifact(
      @Nullable ArtifactLocation interfaceJar,
      @Nullable ArtifactLocation classJar,
      ImmutableList<ArtifactLocation> sourceJars) {
    if (interfaceJar == null && classJar == null) {
      throw new IllegalArgumentException("Interface and class jars cannot both be null.");
    }
    // logger.error("Constructing Library Artifact", new Exception());
    logger.info("ZZZ Constructing Library Artifact");
    if (classJar != null) {
      logger.info("ZZZ Class jar: " + classJar);
    }
    if (interfaceJar != null) {
      logger.info("ZZZ Ijar: " + interfaceJar);
    }

    this.interfaceJar = interfaceJar;
    this.classJar = classJar;
    this.sourceJars = checkNotNull(sourceJars);
    logger.info("ZZZ SOURCES ==================SOURCES");
    this.sourceJars.forEach(lib -> logger.info("ZZZ SOURCE PLEASE: " + lib));
    logger.info("ZZZ SOURCES END ==================SOURCES END");
  }

  /**
   * Returns the best jar to add to IntelliJ.
   *
   * <p>We prefer the interface jar if one exists, otherwise the class jar.
   */
  public ArtifactLocation jarForIntellijLibrary() {
    if (classJar != null) {
      return classJar;
    } else {
      return interfaceJar;
    }
  }

  @Override
  public String toString() {
    return String.format("jar=%s, ijar=%s, srcjars=%s", classJar, interfaceJar, sourceJars);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LibraryArtifact that = (LibraryArtifact) o;
    return Objects.equal(interfaceJar, that.interfaceJar)
        && Objects.equal(classJar, that.classJar)
        && Objects.equal(sourceJars, that.sourceJars);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(interfaceJar, classJar, sourceJars);
  }

  public static Builder builder() {
    return new Builder();
  }

  private static final IdeaLogScope logger = new IdeaLogScope();

  /** Builder for library artifacts */
  public static class Builder {
    private ArtifactLocation interfaceJar;
    private ArtifactLocation classJar;
    private final ImmutableList.Builder<ArtifactLocation> sourceJars = ImmutableList.builder();

    public Builder setInterfaceJar(ArtifactLocation artifactLocation) {
      this.interfaceJar = artifactLocation;
      return this;
    }

    public Builder setClassJar(@Nullable ArtifactLocation artifactLocation) {
      this.classJar = artifactLocation;
      return this;
    }

    public Builder addSourceJar(ArtifactLocation... artifactLocations) {
      Arrays.stream(artifactLocations).forEach(lib -> logger.info("ADDD LIB: " + lib));
      this.sourceJars.add(artifactLocations);
      return this;
    }

    public LibraryArtifact build() {
      return new LibraryArtifact(interfaceJar, classJar, sourceJars.build());
    }
  }
}
