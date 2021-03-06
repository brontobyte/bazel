// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.skyframe;

import com.google.common.base.Preconditions;
import com.google.devtools.build.lib.cmdline.ResolvedTargets;
import com.google.devtools.build.lib.cmdline.TargetParsingException;
import com.google.devtools.build.lib.cmdline.TargetPattern;
import com.google.devtools.build.lib.packages.Target;
import com.google.devtools.build.lib.pkgcache.PathPackageLocator;
import com.google.devtools.build.lib.skyframe.EnvironmentBackedRecursivePackageProvider.MissingDepException;
import com.google.devtools.build.lib.syntax.Label;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

/**
 * TargetPatternFunction translates a target pattern (eg, "foo/...") into a set of resolved
 * Targets.
 */
public class TargetPatternFunction implements SkyFunction {

  private final AtomicReference<PathPackageLocator> pkgPath;

  public TargetPatternFunction(AtomicReference<PathPackageLocator> pkgPath) {
    this.pkgPath = pkgPath;
  }

  @Override
  public SkyValue compute(SkyKey key, Environment env) throws TargetPatternFunctionException,
      InterruptedException {
    TargetPatternValue.TargetPattern patternKey =
        ((TargetPatternValue.TargetPattern) key.argument());
    TargetPattern.Parser parser = new TargetPattern.Parser(patternKey.getOffset());
    ResolvedTargets<Target> resolvedTargets = null;
    try {
      EnvironmentBackedRecursivePackageProvider provider =
          new EnvironmentBackedRecursivePackageProvider(env);
      RecursivePackageProviderBackedTargetPatternResolver resolver =
          new RecursivePackageProviderBackedTargetPatternResolver(provider, env.getListener(),
              patternKey.getPolicy(), pkgPath.get());
      TargetPattern resolvedPattern = parser.parse(patternKey.getPattern());
      resolvedTargets = resolvedPattern.eval(resolver);
    } catch (TargetParsingException e) {
      throw new TargetPatternFunctionException(e);
    } catch (MissingDepException e) {
      // The EnvironmentBackedRecursivePackageProvider constructed above might throw
      // MissingDepException to signal when it has a dependency on a missing Environment value.
      // Note that MissingDepException extends RuntimeException because the methods called
      // on EnvironmentBackedRecursivePackageProvider all belong to an interface shared with other
      // implementations that are unconcerned with MissingDepExceptions.
      return null;
    }
    Preconditions.checkNotNull(resolvedTargets, key);
    ResolvedTargets.Builder<Label> resolvedLabelsBuilder = ResolvedTargets.builder();
    for (Target target : resolvedTargets.getTargets()) {
      resolvedLabelsBuilder.add(target.getLabel());
    }
    for (Target target : resolvedTargets.getFilteredTargets()) {
      resolvedLabelsBuilder.remove(target.getLabel());
    }
    return new TargetPatternValue(resolvedLabelsBuilder.build());
  }

  @Nullable
  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }



  /**
   * Used to declare all the exception types that can be wrapped in the exception thrown by
   * {@link TargetPatternFunction#compute}.
   */
  private static final class TargetPatternFunctionException extends SkyFunctionException {
    public TargetPatternFunctionException(TargetParsingException e) {
      super(e, Transience.PERSISTENT);
    }
  }
}
