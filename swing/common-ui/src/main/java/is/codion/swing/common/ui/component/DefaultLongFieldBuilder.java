/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.LongField;

import java.text.NumberFormat;

class DefaultLongFieldBuilder extends AbstractNumberFieldBuilder<Long, LongField, LongFieldBuilder> implements LongFieldBuilder {

  DefaultLongFieldBuilder(Value<Long> linkedValue) {
    super(Long.class, linkedValue);
  }

  @Override
  public LongFieldBuilder range(long from, long to) {
    minimumValue((double) from);
    maximumValue((double) to);
    return this;
  }

  @Override
  protected LongField createNumberField(NumberFormat format) {
    return format == null ? new LongField() : new LongField(format);
  }

  @Override
  protected ComponentValue<Long, LongField> buildComponentValue(LongField component) {
    return new LongFieldValue(component, true, updateOn);
  }
}
