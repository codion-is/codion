/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.IntegerField;

import java.text.NumberFormat;

final class DefaultIntegerFieldBuilder extends AbstractNumberFieldBuilder<Integer, IntegerField, IntegerFieldBuilder>
        implements IntegerFieldBuilder {

  DefaultIntegerFieldBuilder(final Value<Integer> linkedValue) {
    super(Integer.class, linkedValue);
  }

  @Override
  public IntegerFieldBuilder range(final int from, final int to) {
    minimumValue((double) from);
    maximumValue((double) to);
    return this;
  }

  @Override
  protected IntegerField createNumberField(final NumberFormat format) {
    return format == null ? new IntegerField() : new IntegerField(format);
  }

  @Override
  protected ComponentValue<Integer, IntegerField> buildComponentValue(final IntegerField component) {
    return ComponentValues.integerField(component, true, updateOn);
  }
}
