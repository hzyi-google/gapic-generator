/* Copyright 2018 Google LLC
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

package com.google.api.codegen.transformer.java;

import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.OutputTransformer;
import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.viewmodel.OutputView;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class JavaSampleOutputImportTransformer
    implements OutputTransformer.OutputImportTransformer {

  @Override
  public ImmutableList<ImportFileView> generateOutputImports(
      MethodContext context, List<OutputView> outputViews) {
    outputViews.stream().forEach(view -> generateOutputImports(context, view));
    return ImmutableList.of();
  }

  private TypeModel generateOutputImports(MethodContext context, OutputView outputView) {
    ImportTypeTable typeTable = context.getTypeTable();
    TypeModel typeModel = null;
    switch (outputView.kind()) {
      case DEFINE:
        typeModel = ((OutputView.DefineView) outputView).reference().type();
        break;
      case LOOP:
        typeModel = ((OutputView.LoopView) outputView).collection().type();
        break;
      case PRINT:
      case COMMENT:
        break;
      default:
        throw new IllegalArgumentException(
            String.format("Unrecognized output view kind: %s", outputView.kind()));
    }
    if (typeModel != null) {
      typeTable.getAndSaveNicknameFor(typeModel);
    }
    return typeModel;
  }
}
