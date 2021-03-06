// Copyright 2014 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.syntax;

import com.google.devtools.build.lib.events.Location;
import java.io.IOException;

/** Syntax node for an index expression. e.g. obj[field], but not obj[from:to] */
public final class IndexExpression extends Expression {

  private final Expression object;

  private final Expression key;

  public IndexExpression(Expression object, Expression key) {
    this.object = object;
    this.key = key;
  }

  public Expression getObject() {
    return object;
  }

  public Expression getKey() {
    return key;
  }

  @Override
  public void prettyPrint(Appendable buffer) throws IOException {
    object.prettyPrint(buffer);
    buffer.append('[');
    key.prettyPrint(buffer);
    buffer.append(']');
  }

  @Override
  Object doEval(Environment env) throws EvalException, InterruptedException {
    Object objValue = object.eval(env);
    return eval(env, objValue);
  }

  /**
   * This method can be used instead of eval(Environment) if we want to avoid `obj` being evaluated
   * several times.
   */
  Object eval(Environment env, Object objValue) throws EvalException, InterruptedException {
    Object keyValue = key.eval(env);
    Location loc = getLocation();

    if (objValue instanceof SkylarkIndexable) {
      Object result = ((SkylarkIndexable) objValue).getIndex(keyValue, loc);
      return SkylarkType.convertToSkylark(result, env);
    } else if (objValue instanceof String) {
      String string = (String) objValue;
      int index = EvalUtils.getSequenceIndex(keyValue, string.length(), loc);
      return string.substring(index, index + 1);
    }

    throw new EvalException(
        loc,
        Printer.format(
            "type '%s' has no operator [](%s)",
            EvalUtils.getDataTypeName(objValue), EvalUtils.getDataTypeName(keyValue)));
  }

  @Override
  public void accept(SyntaxTreeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  void validate(ValidationEnvironment env) throws EvalException {
    object.validate(env);
  }
}
