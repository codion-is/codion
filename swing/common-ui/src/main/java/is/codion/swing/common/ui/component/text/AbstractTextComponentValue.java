/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.Consumer;

/**
 * An abstract {@link ComponentValue} implementation for a text component.
 * Handles value notification.
 * @param <T> the value type
 * @param <C> the component type
 */
public abstract class AbstractTextComponentValue<T, C extends JTextComponent> extends AbstractComponentValue<T, C> {

  /**
   * Instantiates a new {@link AbstractTextComponentValue}, with the {@link UpdateOn#VALUE_CHANGE}
   * update on policy and no null value.
   * @param component the component
   */
  protected AbstractTextComponentValue(C component) {
    this(component, null);
  }

  /**
   * Instantiates a new {@link AbstractTextComponentValue}, with the {@link UpdateOn#VALUE_CHANGE}
   * update on policy.
   * @param component the component
   * @param nullValue the value to use instead of null
   */
  protected AbstractTextComponentValue(C component, T nullValue) {
    this(component, nullValue, UpdateOn.VALUE_CHANGE);
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
      ((ValidationDocumentFilter<T>) documentFilter).addValidator(AbstractTextComponentValue.this::validate);
    }
    if (updateOn == UpdateOn.VALUE_CHANGE) {
      Document document = component.getDocument();
      if (document instanceof NumberDocument) {
        ((NumberDocument<Number>) document).addListener(new NotifyOnNumberChanged());
      }
      else {
        document.addDocumentListener(new NotifyOnContentsChanged());
      }
    }
    else {
      component.addFocusListener(new NotifyOnFocusLost());
    }
  }

  private final class NotifyOnNumberChanged implements Consumer<Number> {
    @Override
    public void accept(Number value) {
      notifyListeners();
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
}
