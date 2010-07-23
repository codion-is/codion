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

  public TextInputProvider(final String inputDialogTitle, final ValueCollectionProvider valueProvider, final String currentValue) {
    super(createTextInputPanel(inputDialogTitle, valueProvider, currentValue));
  }

  @Override
  public String getValue() {
    final String value = getInputComponent().getText();

    return value.length() == 0 ? null : value;
  }

  private static TextInputPanel createTextInputPanel(final String inputDialogTitle, final ValueCollectionProvider valueProvider,
                                                     final Object currentValue) {
    final JTextField txtField = new JTextField(currentValue != null ? currentValue.toString() : "");
    txtField.setColumns(DEFAULT_COLUMNS);
    UiUtil.addLookupDialog(txtField, valueProvider);

    return new TextInputPanel(txtField, inputDialogTitle);
  }
}
