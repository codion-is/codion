/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import java.text.NumberFormat;

class DefaultLongFieldBuilder extends AbstractNumberFieldBuilder<Long, LongField, LongFieldBuilder> implements LongFieldBuilder {

  DefaultLongFieldBuilder() {
    super(Long.class);
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
