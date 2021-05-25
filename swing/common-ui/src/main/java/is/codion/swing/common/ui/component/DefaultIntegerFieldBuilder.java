/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import java.text.NumberFormat;

final class DefaultIntegerFieldBuilder extends AbstractNumberFieldBuilder<Integer, IntegerField, IntegerFieldBuilder> implements IntegerFieldBuilder {

  DefaultIntegerFieldBuilder() {
    super(Integer.class);
  }

  @Override
  protected IntegerField createTextField() {
    final IntegerField field = format == null ? new IntegerField() : new IntegerField(cloneFormat((NumberFormat) format));
    if (minimumValue != null && maximumValue != null) {
      field.setRange(minimumValue, maximumValue);
    }

    return field;
  }

  @Override
  protected ComponentValue<Integer, IntegerField> buildComponentValue(final IntegerField component) {
    return ComponentValues.integerField(component, true, updateOn);
  }
}
