/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.textfield.NumberField;

import java.text.NumberFormat;

class DefaultLongFieldBuilder extends AbstractNumberFieldBuilder<Long, NumberField<Long>, LongFieldBuilder> implements LongFieldBuilder {

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
