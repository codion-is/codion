/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.TemporalField;

import javax.swing.JFormattedTextField;
import java.time.temporal.Temporal;

import static java.util.Objects.requireNonNull;

class DefaultTemporalFieldBuilder<T extends Temporal, C extends TemporalField<T>, B extends TemporalFieldBuilder<T, C, B>>
        extends DefaultTextFieldBuilder<T, C, B> implements TemporalFieldBuilder<T, C, B> {

  private final String dateTimePattern;

  private int focusLostBehaviour = JFormattedTextField.COMMIT;

  DefaultTemporalFieldBuilder(final Class<T> valueClass, final String dateTimePattern, final Value<T> linkedValue) {
    super(valueClass, linkedValue);
    this.dateTimePattern = requireNonNull(dateTimePattern);
  }

  @Override
  public final B focusLostBehaviour(final int focusLostBehaviour) {
    this.focusLostBehaviour = focusLostBehaviour;
    return (B) this;
  }

  @Override
  protected final C createTextField() {
    return (C) TemporalField.builder((Class<Temporal>) getValueClass())
            .dateTimePattern(dateTimePattern)
            .focusLostBehaviour(focusLostBehaviour)
            .build();
  }

  @Override
  protected final ComponentValue<T, C> buildComponentValue(final C component) {
    return (ComponentValue<T, C>) component.componentValue(updateOn);
  }

  @Override
  protected final void setInitialValue(final C component, final T initialValue) {
    component.setTemporal(initialValue);
  }
}
