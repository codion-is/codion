/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.ComponentValue;

import java.text.NumberFormat;

final class DefaultLongFieldBuilder<B extends NumberFieldBuilder<Long, B>> extends AbstractNumberFieldBuilder<Long, B> {

  DefaultLongFieldBuilder(Value<Long> linkedValue) {
    super(Long.class, linkedValue);
  }

  @Override
  protected NumberField<Long> createNumberField(NumberFormat format) {
    return format == null ? NumberField.longField() : NumberField.longField(format);
  }

  @Override
  protected ComponentValue<Long, NumberField<Long>> createComponentValue(NumberField<Long> component) {
    return new LongFieldValue(component, true, updateOn);
  }
}
