/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.swing.common.model.textfield.DocumentAdapter;
import org.jminor.swing.common.ui.textfield.NumberField;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

abstract class NumberFieldValue<V extends Number, C extends NumberField> extends AbstractComponentValue<V, C> {

  NumberFieldValue(final C numberField, final boolean nullable, final boolean updateOnKeystroke) {
    super(numberField, nullable);
    if (updateOnKeystroke) {
      numberField.getDocument().addDocumentListener((DocumentAdapter) e -> notifyValueChange(get()));
    }
    else {
      numberField.addFocusListener(new FocusAdapter() {
        @Override
        public void focusLost(final FocusEvent e) {
          if (!e.isTemporary()) {
            notifyValueChange(get());
          }
        }
      });
    }
  }
}
