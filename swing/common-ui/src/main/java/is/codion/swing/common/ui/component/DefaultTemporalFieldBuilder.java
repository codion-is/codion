/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import java.time.temporal.Temporal;

import static java.util.Objects.requireNonNull;

class DefaultTemporalFieldBuilder<T extends Temporal, C extends TemporalField<T>, B extends TemporalFieldBuilder<T, C, B>>
        extends DefaultTextFieldBuilder<T, C, B> implements TemporalFieldBuilder<T, C, B> {

  private final String dateTimePattern;

  DefaultTemporalFieldBuilder(final Class<T> valueClass, final String dateTimePattern) {
    super(valueClass);
    this.dateTimePattern = requireNonNull(dateTimePattern);
  }

  @Override
  protected final C createTextField() {
    return (C) TemporalField.builder((Class<Temporal>) getValueClass())
            .dateTimePattern(dateTimePattern)
            .build();
  }

  @Override
  protected final ComponentValue<T, C> buildComponentValue(final C component) {
    return (ComponentValue<T, C>) ComponentValues.temporalField((TemporalField<Temporal>) component, updateOn);
  }
}
