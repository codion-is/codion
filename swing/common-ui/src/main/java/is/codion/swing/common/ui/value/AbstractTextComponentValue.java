/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.model.textfield.DocumentAdapter;
import is.codion.swing.common.ui.textfield.AbstractParsingDocumentFilter;

import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * An abstract {@link ComponentValue} implementation for a text component.
 * Handles value notification.
 * @param <V> the value type
 * @param <C> the component type
 */
public abstract class AbstractTextComponentValue<V, C extends JTextComponent> extends AbstractComponentValue<V, C> {

  /**
   * Instantiates a new {@link AbstractTextComponentValue}, with the {@link UpdateOn#KEYSTROKE}
   * update on policy.
   * @param component the component
   * @param nullValue the value to use instead of null
   */
  public AbstractTextComponentValue(final C component, final V nullValue) {
    this(component, nullValue, UpdateOn.KEYSTROKE);
  }

  /**
   * Instantiates a new {@link AbstractComponentValue}
   * @param component the component
   * @param nullValue the value to use instead of null
   * @param updateOn the update on policy
   */
  public AbstractTextComponentValue(final C component, final V nullValue, final UpdateOn updateOn) {
    super(component, nullValue);
    final DocumentFilter documentFilter = ((AbstractDocument) component.getDocument()).getDocumentFilter();
    if (documentFilter instanceof AbstractParsingDocumentFilter) {
      getValidators().forEach(((AbstractParsingDocumentFilter<V>) documentFilter)::addValidator);
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
