package org.jminor.swing.common.ui.value;

import org.jminor.swing.common.model.textfield.DocumentAdapter;

import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

abstract class AbstractTextComponentValue<V, C extends JTextComponent> extends AbstractComponentValue<V, C> {

  AbstractTextComponentValue(final C component, final boolean nullable, final boolean updateOnKeystroke) {
    super(component, nullable);
    if (updateOnKeystroke) {
      component.getDocument().addDocumentListener(new NotifyOnContentsChanged());
    }
    else {
      component.addFocusListener(new NotifyOnFocusLost());
    }
  }

  private final class NotifyOnContentsChanged implements DocumentAdapter {
    @Override
    public void contentsChanged(final DocumentEvent e) {
      notifyValueChange(get());
    }
  }

  private final class NotifyOnFocusLost extends FocusAdapter {
    @Override
    public void focusLost(final FocusEvent e) {
      if (!e.isTemporary()) {
        notifyValueChange(get());
      }
    }
  }
}
