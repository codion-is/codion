/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.jminor.common.model.valuemap.ValueCollectionProvider;
import org.jminor.swing.common.ui.TextInputPanel;
import org.jminor.swing.common.ui.UiUtil;
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
   * @param valueProvider the value provider, if specified a lookup dialog accessed by CTRL-SPACE is added to the field
   * @param initialValue the initial value
   * @param maxLength the maximum input length, -1 for no limit
   */
  public TextInputProvider(final String inputDialogTitle, final ValueCollectionProvider valueProvider,
                           final String initialValue, final int maxLength) {
    this(createDefaultTextField(valueProvider, initialValue, maxLength), inputDialogTitle);
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

  private static JTextField createDefaultTextField(final ValueCollectionProvider valueProvider, final Object initialValue,
                                                   final int maxLength) {
    final SizedDocument document = new SizedDocument();
    if (maxLength > 0) {
      document.setMaxLength(maxLength);
    }
    final JTextField txtField = new JTextField(document, initialValue != null ? initialValue.toString() : "", DEFAULT_COLUMNS);
    if (valueProvider != null) {
      UiUtil.addLookupDialog(txtField, valueProvider);
    }

    return txtField;
  }
}
