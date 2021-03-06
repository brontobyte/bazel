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

package com.google.devtools.build.lib.testutil;

import static com.google.devtools.build.lib.packages.Attribute.attr;
import static com.google.devtools.build.lib.packages.Type.INTEGER;
import static com.google.devtools.build.lib.packages.Type.LABEL_LIST;
import static com.google.devtools.build.lib.packages.Type.OUTPUT_LIST;
import static com.google.devtools.build.lib.packages.Type.STRING_LIST;

import com.google.devtools.build.lib.analysis.BaseRuleClasses;
import com.google.devtools.build.lib.analysis.ConfiguredRuleClassProvider;
import com.google.devtools.build.lib.analysis.RuleDefinition;
import com.google.devtools.build.lib.analysis.RuleDefinitionEnvironment;
import com.google.devtools.build.lib.packages.RuleClass;
import com.google.devtools.build.lib.packages.RuleClass.Builder;
import com.google.devtools.build.lib.util.FileTypeSet;

import java.lang.reflect.Method;

/**
 * Helper class to provide a RuleClassProvider for tests.
 */
public class TestRuleClassProvider {
  private static ConfiguredRuleClassProvider ruleProvider = null;

  /**
   * Adds all the rule classes supported internally within the build tool to the given builder.
   */
  public static void addStandardRules(ConfiguredRuleClassProvider.Builder builder) {
    try {
      Class<?> providerClass = Class.forName(TestConstants.TEST_RULE_CLASS_PROVIDER);
      Method setupMethod = providerClass.getMethod("setup",
          ConfiguredRuleClassProvider.Builder.class);
      setupMethod.invoke(null, builder);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Return a rule class provider.
   */
  public static ConfiguredRuleClassProvider getRuleClassProvider() {
    if (ruleProvider == null) {
      ConfiguredRuleClassProvider.Builder builder =
          new ConfiguredRuleClassProvider.Builder();
      addStandardRules(builder);
      builder.addRuleDefinition(new TestingDummyRule());
      ruleProvider = builder.build();
    }
    return ruleProvider;
  }

  public static final class TestingDummyRule implements RuleDefinition {
    @Override
    public RuleClass build(Builder builder, RuleDefinitionEnvironment env) {
      return builder
          .setUndocumented()
          .add(attr("srcs", LABEL_LIST).allowedFileTypes(FileTypeSet.ANY_FILE))
          .add(attr("outs", OUTPUT_LIST))
          .add(attr("dummystrings", STRING_LIST))
          .add(attr("dummyinteger", INTEGER))
          .build();
    }

    @Override
    public Metadata getMetadata() {
      return RuleDefinition.Metadata.builder()
          .name("testing_dummy_rule")
          .ancestors(BaseRuleClasses.RuleBase.class)
          .factoryClass(UnknownRuleConfiguredTarget.class)
          .build();
    }
  }
}
