/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.jminor.swing.common.ui.TextInputPanel;
import org.jminor.swing.common.ui.textfield.SizedDocument;

import javax.swing.JTextField;

/**
 * A InputProvider implementation for String values.
 */
public final class TextInputProvider extends AbstractInputProvider<String, TextInputPanel> {

  private static final int DEFAULT_COLUMNS = 16;

  /**
   * Instantiates a new TextInputProvider.
   * @param inputDialogTitle the title to use for the lookup input dialog
   * @param initialValue the initial value
   * @param maxLength the maximum input length, -1 for no limit
   */
  public TextInputProvider(final String inputDialogTitle, final String initialValue, final int maxLength) {
    this(createDefaultTextField(initialValue, maxLength), inputDialogTitle);
  }

  /**
   * Instantiates a new TextInputProvider.
   * @param textField the text field to use
   * @param inputDialogTitle the title to use for the lookup input dialog
   */
  public TextInputProvider(final JTextField textField, final String inputDialogTitle) {
    super(new TextInputPanel(textField, inputDialogTitle));
  }

  /** {@inheritDoc} */
  @Override
  public String getValue() {
    final String value = getInputComponent().getText();

    return value.length() == 0 ? null : value;
  }

  /** {@inheritDoc} */
  @Override
  public void setValue(final String value) {
    getInputComponent().setText(value);
  }

  private static JTextField createDefaultTextField(final Object initialValue, final int maxLength) {
    final SizedDocument document = new SizedDocument();
    if (maxLength > 0) {
      document.setMaxLength(maxLength);
    }
    final JTextField textField = new JTextField(document, initialValue != null ? initialValue.toString() : "", DEFAULT_COLUMNS);

    return textField;
  }
}
