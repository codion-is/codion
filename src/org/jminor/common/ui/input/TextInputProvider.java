/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import org.jminor.common.model.valuemap.ValueCollectionProvider;
import org.jminor.common.ui.TextInputPanel;
import org.jminor.common.ui.UiUtil;

import javax.swing.JTextField;

/**
 * A InputProvider implementation for String values.
 */
public final class TextInputProvider extends AbstractInputProvider<String, TextInputPanel> {

  private static final int DEFAULT_COLUMNS = 16;

  /**
   * Instantiates a new TextInputProvider.
   * @param inputDialogTitle the title to use for the lookup input dialog
   * @param valueProvider the value provider
   * @param initialValue the initial value
   */
  public TextInputProvider(final String inputDialogTitle, final ValueCollectionProvider valueProvider, final String initialValue) {
    super(createTextInputPanel(inputDialogTitle, valueProvider, initialValue));
  }

  /** {@inheritDoc} */
  @Override
  public String getValue() {
    final String value = getInputComponent().getText();

    return value.isEmpty() ? null : value;
  }

  private static TextInputPanel createTextInputPanel(final String inputDialogTitle, final ValueCollectionProvider valueProvider,
                                                     final Object initialValue) {
    final JTextField txtField = new JTextField(initialValue != null ? initialValue.toString() : "");
    txtField.setColumns(DEFAULT_COLUMNS);
    UiUtil.addLookupDialog(txtField, valueProvider);

    return new TextInputPanel(txtField, inputDialogTitle);
  }
}
