/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.gapic;

import com.google.api.codegen.ArtifactType;
import com.google.api.codegen.CodegenTestUtil;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.GapicCodegenTestConfig;
import com.google.api.codegen.MixedPathTestDataLocator;
import com.google.api.codegen.common.CodeGenerator;
import com.google.api.codegen.common.GeneratedResult;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.ApiDefaultsConfig;
import com.google.api.codegen.config.DependenciesConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.PackagingConfig;
import com.google.api.codegen.grpc.ServiceConfig;
import com.google.api.codegen.samplegen.v1p2.SampleConfigProto;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.model.testing.ConfigBaselineTestCase;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;

/** Base class for code generator baseline tests. */
public abstract class GapicTestBase2 extends ConfigBaselineTestCase {

  private static final Date frozenTimestamp = new GregorianCalendar(2019, 7, 1).getTime();

  // Wiring
  // ======

  private final TargetLanguage language;
  private final String[] gapicConfigFileNames;
  private final String[] sampleConfigFileNames;
  @Nullable private final String packageConfigFileName;
  private final ImmutableList<String> snippetNames;
  private ApiDefaultsConfig apiDefaultsConfig;
  private DependenciesConfig dependenciesConfig;
  private PackagingConfig packagingConfig;
  protected ConfigProto gapicConfig;
  protected SampleConfigProto sampleConfig;
  private final String baselineFile;
  private final String protoPackage;
  private final String clientPackage;
  private final TestDataLocator testDataLocator = MixedPathTestDataLocator.create(this.getClass());
  private final String grpcServiceConfigFileName;
  private ServiceConfig grpcServiceConfig;

  public GapicTestBase2(GapicCodegenTestConfig testConfig) {
    this.language = testConfig.targetLanguage();
    this.gapicConfigFileNames = testConfig.gapicConfigFileNames().stream().toArray(String[]::new);
    this.sampleConfigFileNames = testConfig.sampleConfigFileNames().stream().toArray(String[]::new);
    this.packageConfigFileName = testConfig.packageConfigFileName();
    this.snippetNames = ImmutableList.copyOf(testConfig.snippetNames());
    this.baselineFile = testConfig.baseline();
    this.clientPackage = testConfig.clientPackage();
    this.grpcServiceConfigFileName = testConfig.grpcServiceConfigFileName();

    // Represents the test value for the --package flag.
    this.protoPackage = testConfig.protoPackage();

    String dir = language.toString().toLowerCase();
    if ("python".equals(dir)) {
      dir = "py";
    }
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, dir);
    getTestDataLocator().addTestDataSource(getClass(), "testdata/" + dir);
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc/common");
  }

  @Override
  protected TestDataLocator getTestDataLocator() {
    return this.testDataLocator;
  }

  @Override
  protected void test(String... baseNames) throws Exception {
    super.test(new GapicTestModelGenerator(getTestDataLocator(), tempDir), baseNames);
  }

  @Override
  protected void setupModel() {
    super.setupModel();
    if (gapicConfigFileNames.length != 0) {
      gapicConfig =
          CodegenTestUtil.readConfig(
              model.getDiagReporter().getDiagCollector(),
              getTestDataLocator(),
              gapicConfigFileNames);
    }

    if (sampleConfigFileNames.length != 0) {
      sampleConfig =
          CodegenTestUtil.readSampleConfig(
              model.getDiagReporter().getDiagCollector(),
              getTestDataLocator(),
              sampleConfigFileNames);
    }
    try {
      apiDefaultsConfig = ApiDefaultsConfig.load();
      dependenciesConfig =
          DependenciesConfig.loadFromURL(
              getTestDataLocator().findTestData("frozen_dependencies.yaml"));
      if (!Strings.isNullOrEmpty(packageConfigFileName)) {
        packagingConfig =
            PackagingConfig.loadFromURL(getTestDataLocator().findTestData(packageConfigFileName));
      } else {
        packagingConfig = null;
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Problem creating packageConfig");
    }

    if (!Strings.isNullOrEmpty(grpcServiceConfigFileName)) {
      grpcServiceConfig =
          CodegenTestUtil.readGRPCServiceConfig(
              model.getDiagReporter().getDiagCollector(),
              testDataLocator,
              grpcServiceConfigFileName);
    }

    // TODO (garrettjones) depend on the framework to take care of this.
    if (model.getDiagReporter().getDiagCollector().getErrorCount() > 0) {
      for (Diag diag : model.getDiagReporter().getDiagCollector().getDiags()) {
        System.err.println(diag.toString());
      }
      throw new IllegalArgumentException("Problem creating Generator");
    }
  }

  @Override
  protected boolean suppressDiagnosis() {
    // Suppress linter warnings
    return true;
  }

  @Override
  protected String baselineFileName() {
    return baselineFile;
  }

  @Override
  public Map<String, ?> run() throws IOException {
    return runWithArtifacts(new ArrayList<>(Arrays.asList("surface", "test", "samples")));
  }

  protected Map<String, ?> runWithArtifacts(List<String> enabledArtifacts) throws IOException {
    model.establishStage(Merged.KEY);
    if (model.getDiagReporter().getDiagCollector().getErrorCount() > 0) {
      for (Diag diag : model.getDiagReporter().getDiagCollector().getDiags()) {
        System.err.println(diag.toString());
      }
      return null;
    }
    if (sampleConfig == null) {
      sampleConfig = SampleConfigProto.getDefaultInstance();
    }
    GapicProductConfig productConfig =
        GapicProductConfig.create(
            model,
            gapicConfig,
            sampleConfig,
            protoPackage,
            clientPackage,
            language,
            grpcServiceConfig);

    if (productConfig == null) {
      for (Diag diag : model.getDiagReporter().getDiagCollector().getDiags()) {
        System.err.println(diag.toString());
      }
      return null;
    }
    productConfig = productConfig.withGenerationTimestamp(frozenTimestamp);
    ArtifactFlags artifactFlags =
        new ArtifactFlags(enabledArtifacts, ArtifactType.LEGACY_GAPIC_AND_PACKAGE, true);

    PackagingConfig actualPackagingConfig = packagingConfig;
    if (actualPackagingConfig == null) {
      actualPackagingConfig =
          PackagingConfig.loadFromProductConfig(productConfig.getInterfaceConfigMap());
    }
    PackageMetadataConfig packageConfig =
        PackageMetadataConfig.createFromPackaging(
            apiDefaultsConfig, dependenciesConfig, actualPackagingConfig);

    List<CodeGenerator<?>> generators =
        GapicGeneratorFactory.create(language, model, productConfig, packageConfig, artifactFlags);

    // Don't run any generators we're not testing.
    ArrayList<CodeGenerator<?>> testedGenerators = new ArrayList<>();
    for (CodeGenerator<?> generator : generators) {
      if (!Collections.disjoint(generator.getInputFileNames(), snippetNames)) {
        testedGenerators.add(generator);
      }
    }

    Map<String, Object> output = new TreeMap<>();
    for (CodeGenerator<?> generator : testedGenerators) {
      Map<String, ? extends GeneratedResult<?>> out = generator.generate();

      if (!Collections.disjoint(out.keySet(), output.keySet())) {
        throw new IllegalStateException("file conflict");
      }
      for (Map.Entry<String, ? extends GeneratedResult<?>> entry : out.entrySet()) {
        Object value =
            (entry.getValue().getBody() instanceof byte[])
                ? "Static or binary file content is not shown."
                : entry.getValue().getBody();
        output.put(entry.getKey(), value);
      }
    }

    return output;
  }

  private static boolean hasSmokeTestConfig(GapicProductConfig productConfig) {
    return productConfig
        .getInterfaceConfigMap()
        .values()
        .stream()
        .anyMatch(config -> config.getSmokeTestConfig() != null);
  }
}
