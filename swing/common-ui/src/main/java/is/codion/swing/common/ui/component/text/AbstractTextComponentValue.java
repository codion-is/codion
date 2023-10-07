/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * An abstract {@link ComponentValue} implementation for a text component.
 * Handles value notification.
 * @param <T> the value type
 * @param <C> the component type
 */
public abstract class AbstractTextComponentValue<T, C extends JTextComponent> extends AbstractComponentValue<T, C> {

  /**
   * Instantiates a new {@link AbstractTextComponentValue}, with the {@link UpdateOn#KEYSTROKE}
   * update on policy and no null value.
   * @param component the component
   */
  protected AbstractTextComponentValue(C component) {
    this(component, null);
  }

  /**
   * Instantiates a new {@link AbstractTextComponentValue}, with the {@link UpdateOn#KEYSTROKE}
   * update on policy.
   * @param component the component
   * @param nullValue the value to use instead of null
   */
  protected AbstractTextComponentValue(C component, T nullValue) {
    this(component, nullValue, UpdateOn.KEYSTROKE);
  }

  /**
   * Instantiates a new {@link AbstractComponentValue}
   * @param component the component
   * @param nullValue the value to use instead of null
   * @param updateOn the update on policy
   * @throws NullPointerException in case component is null
   */
  protected AbstractTextComponentValue(C component, T nullValue, UpdateOn updateOn) {
    super(component, nullValue);
    DocumentFilter documentFilter = ((AbstractDocument) component.getDocument()).getDocumentFilter();
    if (documentFilter instanceof ValidationDocumentFilter) {
      ((ValidationDocumentFilter<T>) documentFilter).addValidator(new CombinedValidator());
    }
    if (updateOn == UpdateOn.KEYSTROKE) {
      component.getDocument().addDocumentListener(new NotifyOnContentsChanged());
    }
    else {
      component.addFocusListener(new NotifyOnFocusLost());
    }
  }

  private final class NotifyOnContentsChanged implements DocumentAdapter {
    @Override
    public void contentsChanged(DocumentEvent e) {
      notifyListeners();
    }
  }

  private final class NotifyOnFocusLost extends FocusAdapter {
    @Override
    public void focusLost(FocusEvent e) {
      if (!e.isTemporary()) {
        notifyListeners();
      }
    }
  }

  private final class CombinedValidator implements Validator<T> {
    @Override
    public void validate(T value) throws IllegalArgumentException {
      validators().forEach(validator -> validator.validate(value));
    }
  }
}
