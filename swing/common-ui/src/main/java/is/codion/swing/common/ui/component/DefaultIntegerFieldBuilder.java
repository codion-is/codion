/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.textfield.NumberField;

import java.text.NumberFormat;

final class DefaultIntegerFieldBuilder extends AbstractNumberFieldBuilder<Integer, IntegerFieldBuilder>
        implements IntegerFieldBuilder {

  DefaultIntegerFieldBuilder(Value<Integer> linkedValue) {
    super(Integer.class, linkedValue);
  }

  @Override
  protected NumberField<Integer> createNumberField(NumberFormat format) {
    return format == null ? NumberField.integerField() : NumberField.integerField(format);
  }

  @Override
  protected ComponentValue<Integer, NumberField<Integer>> createComponentValue(NumberField<Integer> component) {
    return new IntegerFieldValue(component, true, updateOn);
  }
}
