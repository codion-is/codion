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
  protected LongField createTextField() {
    final LongField field = format == null ? new LongField() : new LongField(cloneFormat((NumberFormat) format));
    if (minimumValue != null && maximumValue != null) {
      field.setRange(minimumValue, maximumValue);
    }

    return field;
  }

  @Override
  protected ComponentValue<Long, LongField> buildComponentValue(final LongField component) {
    return ComponentValues.longField(component, true, updateOn);
  }
}
