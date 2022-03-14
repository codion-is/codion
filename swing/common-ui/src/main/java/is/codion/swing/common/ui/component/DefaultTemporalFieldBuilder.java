/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.DateTimeParser;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.TemporalField;

import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

final class DefaultTemporalFieldBuilder<T extends Temporal, C extends TemporalField<T>>
        extends DefaultTextFieldBuilder<T, C, TemporalFieldBuilder<T, C>> implements TemporalFieldBuilder<T, C> {

  private final TemporalField.Builder<T> builder;

  DefaultTemporalFieldBuilder(Class<T> valueClass, String dateTimePattern, Value<T> linkedValue) {
    super(valueClass, linkedValue);
    this.builder = TemporalField.builder(valueClass, dateTimePattern);
  }

  @Override
  public TemporalFieldBuilder<T, C> dateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
    builder.dateTimeFormatter(dateTimeFormatter);
    return this;
  }

  @Override
  public TemporalFieldBuilder<T, C> dateTimeParser(DateTimeParser<T> dateTimeParser) {
    builder.dateTimeParser(dateTimeParser);
    return this;
  }

  @Override
  public TemporalFieldBuilder<T, C> focusLostBehaviour(int focusLostBehaviour) {
    builder.focusLostBehaviour(focusLostBehaviour);
    return this;
  }

  @Override
  protected C createTextField() {
    return (C) builder.build();
  }

  @Override
  protected ComponentValue<T, C> buildComponentValue(C component) {
    return (ComponentValue<T, C>) component.componentValue(updateOn);
  }

  @Override
  protected void setInitialValue(C component, T initialValue) {
    component.setTemporal(initialValue);
  }
}
