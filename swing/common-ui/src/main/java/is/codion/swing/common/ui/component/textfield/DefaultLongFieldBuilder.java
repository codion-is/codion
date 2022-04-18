/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import is.codion.common.formats.Formats;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.ComponentValue;

import java.text.NumberFormat;

final class DefaultLongFieldBuilder<B extends NumberField.Builder<Long, B>> extends AbstractNumberFieldBuilder<Long, B> {

  DefaultLongFieldBuilder(Value<Long> linkedValue) {
    super(Long.class, linkedValue);
  }

  @Override
  protected NumberField<Long> createNumberField(NumberFormat format) {
    if (format == null) {
      format = Formats.getNonGroupingIntegerFormat();
    }

    return new NumberField<>(new NumberDocument<>(new NumberParsingDocumentFilter<>(new NumberParser<>(format, Long.class))));
  }

  @Override
  protected ComponentValue<Long, NumberField<Long>> createComponentValue(NumberField<Long> component) {
    return new LongFieldValue(component, true, updateOn);
  }
}
