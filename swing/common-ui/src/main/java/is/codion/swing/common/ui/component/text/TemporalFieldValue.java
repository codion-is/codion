/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.ui.component.AbstractComponentValue;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.temporal.Temporal;

final class TemporalFieldValue<T extends Temporal> extends AbstractComponentValue<T, TemporalField<T>> {

  TemporalFieldValue(TemporalField<T> component, UpdateOn updateOn) {
    super(component);
    if (updateOn == UpdateOn.KEYSTROKE) {
      component.addTemporalListener(value -> notifyValueChange());
    }
    else {
      component.addFocusListener(new NotifyOnFocusLost());
    }
  }

  @Override
  protected T getComponentValue() {
    return getComponent().getTemporal();
  }

  @Override
  protected void setComponentValue(T value) {
    getComponent().setTemporal(value);
  }

  private final class NotifyOnFocusLost extends FocusAdapter {
    @Override
    public void focusLost(FocusEvent e) {
      if (!e.isTemporary()) {
        notifyValueChange();
      }
    }
  }
}
