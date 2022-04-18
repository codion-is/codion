/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import is.codion.common.formats.Formats;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.ComponentValue;

import java.text.NumberFormat;

final class DefaultIntegerFieldBuilder<B extends NumberField.Builder<Integer, B>> extends AbstractNumberFieldBuilder<Integer, B> {

  DefaultIntegerFieldBuilder(Value<Integer> linkedValue) {
    super(Integer.class, linkedValue);
  }

  @Override
  protected NumberField<Integer> createNumberField(NumberFormat format) {
    if (format == null) {
      format = Formats.getNonGroupingIntegerFormat();
    }

    return new NumberField<>(new NumberDocument<>(new NumberParsingDocumentFilter<>(new NumberParser<>(format, Integer.class))));
  }

  @Override
  protected ComponentValue<Integer, NumberField<Integer>> createComponentValue(NumberField<Integer> component) {
    return new IntegerFieldValue(component, true, updateOn);
  }
}
