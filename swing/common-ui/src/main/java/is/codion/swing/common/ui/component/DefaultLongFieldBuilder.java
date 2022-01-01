/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.LongField;

import java.text.NumberFormat;

class DefaultLongFieldBuilder extends AbstractNumberFieldBuilder<Long, LongField, LongFieldBuilder> implements LongFieldBuilder {

  DefaultLongFieldBuilder(final Value<Long> linkedValue) {
    super(Long.class, linkedValue);
  }

  @Override
  public LongFieldBuilder range(final long from, final long to) {
    minimumValue((double) from);
    maximumValue((double) to);
    return this;
  }

  @Override
  protected LongField createNumberField(final NumberFormat format) {
    return format == null ? new LongField() : new LongField(format);
  }

  @Override
  protected ComponentValue<Long, LongField> buildComponentValue(final LongField component) {
    return ComponentValues.longField(component, true, updateOn);
  }
}
