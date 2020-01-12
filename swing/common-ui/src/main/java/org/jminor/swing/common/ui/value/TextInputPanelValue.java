/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.swing.common.model.textfield.DocumentAdapter;
import org.jminor.swing.common.ui.TextInputPanel;
import org.jminor.swing.common.ui.textfield.SizedDocument;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;

class TextInputPanelValue extends AbstractComponentValue<String, TextInputPanel> {

  private static final int DEFAULT_COLUMNS = 16;

  TextInputPanelValue(final String inputDialogTitle, final String initialValue, final int maxLength) {
    super(new TextInputPanel(createDefaultTextField(initialValue, maxLength), inputDialogTitle));
    getComponent().getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void contentsChanged(final DocumentEvent e) {
        notifyValueChange(get());
      }
    });
  }

  /** {@inheritDoc} */
  @Override
  public String get() {
    final String value = getComponent().getText();

    return value.length() == 0 ? null : value;
  }

  /** {@inheritDoc} */
  @Override
  protected void setComponentValue(final String value) {
    getComponent().setText(value);
  }

  private static JTextField createDefaultTextField(final String initialValue, final int maxLength) {
    final SizedDocument document = new SizedDocument();
    if (maxLength > 0) {
      document.setMaxLength(maxLength);
    }

    return new JTextField(document, initialValue != null ? initialValue : "", DEFAULT_COLUMNS);
  }
}
