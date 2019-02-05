/* Copyright 2019 Google LLC
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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.StandardSampleImportTransformer;
import com.google.api.codegen.viewmodel.OutputView;
import com.google.api.codegen.viewmodel.PrintArgView;
import java.util.List;

/** Generates an ImportSection for standalone samples. */
public class PythonSampleImportTransformer extends StandardSampleImportTransformer {

  public PythonSampleImportTransformer() {
    super(new PythonImportSectionTransformer());
  }

  // void addSampleBodyImports(MethodContext context, CallingForm form) {}
  public void addOutputImports(MethodContext context, List<OutputView> views) {
    for (OutputView view : views) {
      if (view.kind() == OutputView.Kind.LOOP) {
        addOutputImports(context, ((OutputView.LoopView) view).body());
      }
      if (view.kind() == OutputView.Kind.PRINT) {
        addEnumImports(context, (OutputView.PrintView) view);
      }
    }
  }

  // void addInitCodeImports(
  //     MethodContext context, ImportTypeTable initCodeTypeTable, Iterable<InitCodeNode> nodes) {}

  // ImportSectionView generateImportSection(MethodContext context) {}

  private void addEnumImports(MethodContext context, OutputView.PrintView view) {
    boolean shouldImportEnumType =
        view.args()
            .stream()
            .flatMap(arg -> arg.segments().stream())
            .filter(seg -> seg.kind() == PrintArgView.ArgSegmentView.Kind.VARIABLE)
            .map(seg -> ((PrintArgView.VariableSegmentView) seg).variable().type())
            .anyMatch(type -> type != null && type.isEnum());
    if (shouldImportEnumType) {
      context
          .getTypeTable()
          .getAndSaveNicknameFor(context.getNamer().getVersionedDirectoryNamespace() + ".enums");
    }
  }
}
