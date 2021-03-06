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
package com.google.idea.blaze.python.run.producers;

import com.google.common.base.Joiner;
import com.google.idea.blaze.base.command.BlazeCommandName;
import com.google.idea.blaze.base.command.BlazeFlags;
import com.google.idea.blaze.base.dependencies.TargetInfo;
import com.google.idea.blaze.base.run.BlazeCommandRunConfiguration;
import com.google.idea.blaze.base.run.BlazeCommandRunConfigurationType;
import com.google.idea.blaze.base.run.BlazeConfigurationNameBuilder;
import com.google.idea.blaze.base.run.TestTargetHeuristic;
import com.google.idea.blaze.base.run.producers.BlazeRunConfigurationProducer;
import com.google.idea.blaze.base.run.smrunner.SmRunnerUtils;
import com.google.idea.blaze.base.run.state.BlazeCommandRunConfigurationCommonState;
import com.google.idea.blaze.python.run.PyTestUtils;
import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyArgumentList;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyDecorator;
import com.jetbrains.python.psi.PyDecoratorList;
import com.jetbrains.python.psi.PyDictLiteralExpression;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.psi.PyKeyValueExpression;
import com.jetbrains.python.psi.PyParenthesizedExpression;
import com.jetbrains.python.psi.PyStringLiteralExpression;
import com.jetbrains.python.psi.PyTupleExpression;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/** Producer for run configurations related to python test classes in Blaze. */
public class BlazePyTestConfigurationProducer
    extends BlazeRunConfigurationProducer<BlazeCommandRunConfiguration> {

  public BlazePyTestConfigurationProducer() {
    super(BlazeCommandRunConfigurationType.getInstance());
  }

  private static class TestLocation {
    @Nullable private final PyClass testClass;
    @Nullable private final PyFunction testFunction;

    private TestLocation(@Nullable PyClass testClass, @Nullable PyFunction testFunction) {
      this.testClass = testClass;
      this.testFunction = testFunction;
    }

    private String getTestFilterForParameters(String testBase, PyDecorator decorator) {
      PyArgumentList parameterizedArgumentList = decorator.getArgumentList();
      if (parameterizedArgumentList == null) {
        return testBase;
      }
      PyExpression[] arguments = parameterizedArgumentList.getArguments();
      if (arguments.length == 0) {
        return testBase;
      }

      ArrayList<String> parameterizedFilters = new ArrayList<>();
      for (int i = 0; i < arguments.length; ++i) {

        parameterizedFilters.add(testBase + i);
      }

      if (parameterizedFilters.isEmpty()) {
        return testBase;
      }

      return Joiner.on(" ").join(parameterizedFilters);
    }

    private String getTestFilterForNamedParameters(String testBase, PyDecorator decorator) {
      PyArgumentList parameterizedArgumentList = decorator.getArgumentList();
      if (parameterizedArgumentList == null) {
        return testBase;
      }
      PyExpression[] arguments = parameterizedArgumentList.getArguments();
      if (arguments.length == 0) {
        return testBase;
      }

      ArrayList<String> parameterizedFilters = new ArrayList<>();
      for (PyExpression argument : arguments) {
        if (argument instanceof PyDictLiteralExpression) {
          // can be defined as a dict, use the name from element 'testcase_name'
          PyDictLiteralExpression dictArgument = (PyDictLiteralExpression) argument;
          for (PyKeyValueExpression keyValueExpression : dictArgument.getElements()) {
            PyExpression key = keyValueExpression.getKey();
            PyExpression value = keyValueExpression.getValue();
            if (key instanceof PyStringLiteralExpression
                && value instanceof PyStringLiteralExpression) {
              PyStringLiteralExpression keyString = (PyStringLiteralExpression) key;
              PyStringLiteralExpression valueString = (PyStringLiteralExpression) value;
              if (keyString.getStringValue().equals("testcase_name")) {
                parameterizedFilters.add(testBase + valueString.getStringValue());
              }
            }
          }
        } else if (argument instanceof PyParenthesizedExpression) {
          // can be defined as a tuple, use the name from the 0th element
          PyExpression contained = ((PyParenthesizedExpression) argument).getContainedExpression();
          if (contained instanceof PyTupleExpression) {
            PyTupleExpression tupleArgument = (PyTupleExpression) contained;
            PyExpression[] tupleElements = tupleArgument.getElements();
            if (tupleElements.length > 0 && tupleElements[0] instanceof PyStringLiteralExpression) {
              PyStringLiteralExpression testcaseName = (PyStringLiteralExpression) tupleElements[0];
              parameterizedFilters.add(testBase + testcaseName.getStringValue());
            }
          }
        }
      }
      if (parameterizedFilters.isEmpty()) {
        return testBase;
      }
      return Joiner.on(" ").join(parameterizedFilters);
    }

    @Nullable
    private String testFilter() {
      if (testClass == null) {
        return null;
      }

      if (testFunction == null) {
        return testClass.getName();
      }

      String nonParameterizedTest = testClass.getName() + "." + testFunction.getName();

      PyDecoratorList decoratorList = testFunction.getDecoratorList();
      if (decoratorList == null) {
        return nonParameterizedTest;
      }

      PyDecorator parameterizedDecorator = decoratorList.findDecorator("parameterized.parameters");
      if (parameterizedDecorator != null) {
        return getTestFilterForParameters(nonParameterizedTest, parameterizedDecorator);
      }

      PyDecorator namedParameterizedDecorator =
          decoratorList.findDecorator("parameterized.named_parameters");
      if (namedParameterizedDecorator != null) {
        return getTestFilterForNamedParameters(nonParameterizedTest, namedParameterizedDecorator);
      }

      return nonParameterizedTest;
    }

    @Nullable
    PsiElement sourceElement(PsiFile file) {
      if (testFunction != null) {
        return testFunction;
      }
      return testClass != null ? testClass : file;
    }
  }

  /**
   * The single selected {@link PsiElement}. Returns null if we're in a SM runner tree UI context
   * (handled by a different producer).
   */
  @Nullable
  private static PsiElement selectedPsiElement(ConfigurationContext context) {
    List<Location<?>> selectedTestUiElements =
        SmRunnerUtils.getSelectedSmRunnerTreeElements(context);
    if (!selectedTestUiElements.isEmpty()) {
      return null;
    }
    Location<?> location = context.getLocation();
    return location != null ? location.getPsiElement() : null;
  }

  @Override
  protected boolean doSetupConfigFromContext(
      BlazeCommandRunConfiguration configuration,
      ConfigurationContext context,
      Ref<PsiElement> sourceElement) {

    PsiElement element = selectedPsiElement(context);
    if (element == null) {
      return false;
    }
    PsiFile file = element.getContainingFile();
    if (!(file instanceof PyFile) || !PyTestUtils.isTestFile((PyFile) file)) {
      return false;
    }
    TargetInfo testTarget = TestTargetHeuristic.testTargetForPsiElement(element);
    if (testTarget == null) {
      return false;
    }
    configuration.setTargetInfo(testTarget);

    TestLocation testLocation = testLocation(element);
    sourceElement.set(testLocation.sourceElement(file));
    BlazeCommandRunConfigurationCommonState handlerState =
        configuration.getHandlerStateIfType(BlazeCommandRunConfigurationCommonState.class);
    if (handlerState == null) {
      return false;
    }
    handlerState.getCommandState().setCommand(BlazeCommandName.TEST);

    // remove conflicting flags from initial configuration
    List<String> flags = new ArrayList<>(handlerState.getBlazeFlagsState().getRawFlags());
    flags.removeIf((flag) -> flag.startsWith(BlazeFlags.TEST_FILTER));
    String filter = testLocation.testFilter();
    if (filter != null) {
      flags.add(BlazeFlags.TEST_FILTER + "=" + filter);
    }
    handlerState.getBlazeFlagsState().setRawFlags(flags);

    BlazeConfigurationNameBuilder nameBuilder = new BlazeConfigurationNameBuilder(configuration);
    if (filter != null) {
      nameBuilder.setTargetString(String.format("%s (%s)", filter, testTarget.label.toString()));
    } else {
      nameBuilder.setTargetString(testTarget.label);
    }
    configuration.setName(nameBuilder.build());
    configuration.setNameChangedByUser(true); // don't revert to generated name
    return true;
  }

  @Override
  protected boolean doIsConfigFromContext(
      BlazeCommandRunConfiguration configuration, ConfigurationContext context) {

    BlazeCommandRunConfigurationCommonState handlerState =
        configuration.getHandlerStateIfType(BlazeCommandRunConfigurationCommonState.class);
    if (handlerState == null
        || !BlazeCommandName.TEST.equals(handlerState.getCommandState().getCommand())) {
      return false;
    }

    PsiElement element = selectedPsiElement(context);
    if (element == null) {
      return false;
    }
    if (!(element.getContainingFile() instanceof PyFile)) {
      return false;
    }
    TargetInfo testTarget = TestTargetHeuristic.testTargetForPsiElement(element);
    if (testTarget == null || !testTarget.label.equals(configuration.getTarget())) {
      return false;
    }
    String filter = testLocation(element).testFilter();

    String filterFlag = handlerState.getTestFilterFlag();
    return (filterFlag == null && filter == null)
        || Objects.equals(filterFlag, BlazeFlags.TEST_FILTER + "=" + filter);
  }

  private static TestLocation testLocation(PsiElement element) {
    PyClass pyClass = PsiTreeUtil.getParentOfType(element, PyClass.class, false);
    if (pyClass == null || !PyTestUtils.isTestClass(pyClass)) {
      return new TestLocation(null, null);
    }
    PyFunction pyFunction = PsiTreeUtil.getParentOfType(element, PyFunction.class, false);
    if (pyFunction != null && PyTestUtils.isTestFunction(pyFunction)) {
      return new TestLocation(pyClass, pyFunction);
    }
    return new TestLocation(pyClass, null);
  }
}
