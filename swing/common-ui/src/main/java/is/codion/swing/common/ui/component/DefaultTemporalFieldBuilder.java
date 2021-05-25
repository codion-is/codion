/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import java.time.temporal.Temporal;

class DefaultTemporalFieldBuilder<T extends Temporal, C extends TemporalField<T>, B extends TemporalFieldBuilder<T, C, B>>
  extends DefaultTextFieldBuilder<T, C, B> implements TemporalFieldBuilder<T, C, B> {

  private String dateTimePattern;

  DefaultTemporalFieldBuilder(final Class<T> valueClass) {
    super(valueClass);
  }

  @Override
  public final B dateTimePattern(final String dateTimePattern) {
    this.dateTimePattern = dateTimePattern;
    return (B) this;
  }

  @Override
  protected final C createTextField() {
    if (dateTimePattern == null) {
      throw new IllegalStateException("dateTimePattern must be specified for temporal fields");
    }
    return (C) TemporalField.builder((Class<Temporal>) valueClass)
            .dateTimePattern(dateTimePattern)
            .build();
  }

  @Override
  protected final ComponentValue<T, C> buildComponentValue(final C component) {
    return (ComponentValue<T, C>) ComponentValues.temporalField((TemporalField<Temporal>) component, updateOn);
  }
}
