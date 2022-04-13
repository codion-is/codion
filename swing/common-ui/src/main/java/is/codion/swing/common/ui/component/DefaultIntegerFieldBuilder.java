/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.textfield.IntegerField;

import java.text.NumberFormat;

final class DefaultIntegerFieldBuilder extends AbstractNumberFieldBuilder<Integer, IntegerField, IntegerFieldBuilder>
        implements IntegerFieldBuilder {

  DefaultIntegerFieldBuilder(Value<Integer> linkedValue) {
    super(Integer.class, linkedValue);
  }

  @Override
  public IntegerFieldBuilder range(int from, int to) {
    minimumValue((double) from);
    maximumValue((double) to);
    return this;
  }

  @Override
  protected IntegerField createNumberField(NumberFormat format) {
    return format == null ? new IntegerField() : new IntegerField(format);
  }

  @Override
  protected ComponentValue<Integer, IntegerField> createComponentValue(IntegerField component) {
    return new IntegerFieldValue(component, true, updateOn);
  }
}
