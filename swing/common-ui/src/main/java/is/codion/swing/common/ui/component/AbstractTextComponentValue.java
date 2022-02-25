/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.model.textfield.DocumentAdapter;
import is.codion.swing.common.ui.textfield.ValidationDocumentFilter;

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
   * update on policy.
   * @param component the component
   * @param nullValue the value to use instead of null
   */
  public AbstractTextComponentValue(final C component, final T nullValue) {
    this(component, nullValue, UpdateOn.KEYSTROKE);
  }

  /**
   * Instantiates a new {@link AbstractComponentValue}
   * @param component the component
   * @param nullValue the value to use instead of null
   * @param updateOn the update on policy
   */
  public AbstractTextComponentValue(final C component, final T nullValue, final UpdateOn updateOn) {
    super(component, nullValue);
    DocumentFilter documentFilter = ((AbstractDocument) component.getDocument()).getDocumentFilter();
    if (documentFilter instanceof ValidationDocumentFilter) {
      ((ValidationDocumentFilter<T>) documentFilter).addValidator(value ->
              getValidators().forEach(validator -> validator.validate(value)));
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
    public void contentsChanged(final DocumentEvent e) {
      notifyValueChange();
    }
  }

  private final class NotifyOnFocusLost extends FocusAdapter {
    @Override
    public void focusLost(final FocusEvent e) {
      if (!e.isTemporary()) {
        notifyValueChange();
      }
    }
  }
}
