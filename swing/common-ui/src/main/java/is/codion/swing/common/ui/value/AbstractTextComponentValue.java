/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.value.Nullable;
import is.codion.swing.common.model.textfield.DocumentAdapter;

import javax.swing.event.DocumentEvent;
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
   * @param nullable {@link Nullable#NO} if this value can not be null
   */
  public AbstractTextComponentValue(final C component, final Nullable nullable) {
    this(component, nullable, UpdateOn.KEYSTROKE);
  }

  /**
   * Instantiates a new {@link AbstractComponentValue}
   * @param component the component
   * @param nullable {@link Nullable#NO} if this value can not be null
   * @param updateOn the update on policy
   */
  public AbstractTextComponentValue(final C component, final Nullable nullable, final UpdateOn updateOn) {
    super(component, nullable);
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
