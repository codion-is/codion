/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.TemporalField;

import javax.swing.JFormattedTextField;
import java.time.temporal.Temporal;

import static java.util.Objects.requireNonNull;

final class DefaultTemporalFieldBuilder<T extends Temporal, C extends TemporalField<T>>
        extends DefaultTextFieldBuilder<T, C, TemporalFieldBuilder<T, C>> implements TemporalFieldBuilder<T, C> {

  private final String dateTimePattern;

  private int focusLostBehaviour = JFormattedTextField.COMMIT;

  DefaultTemporalFieldBuilder(Class<T> valueClass, String dateTimePattern, Value<T> linkedValue) {
    super(valueClass, linkedValue);
    this.dateTimePattern = requireNonNull(dateTimePattern);
  }

  @Override
  public TemporalFieldBuilder<T, C> focusLostBehaviour(int focusLostBehaviour) {
    this.focusLostBehaviour = focusLostBehaviour;
    return this;
  }

  @Override
  protected C createTextField() {
    return (C) TemporalField.builder((Class<Temporal>) getValueClass(), dateTimePattern)
            .focusLostBehaviour(focusLostBehaviour)
            .build();
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
