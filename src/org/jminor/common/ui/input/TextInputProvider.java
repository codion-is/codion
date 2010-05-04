/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import org.jminor.common.model.valuemap.ValueListProvider;
import org.jminor.common.ui.TextInputPanel;
import org.jminor.common.ui.UiUtil;

import javax.swing.JTextField;

/**
 * A InputProvider implementation for String values.
 */
public class TextInputProvider extends AbstractInputProvider<String> {

  public TextInputProvider(final String inputDialogTitle, final ValueListProvider valueListProvider, final String currentValue) {
    super(createTextInputPanel(inputDialogTitle, valueListProvider, currentValue));
  }

  @Override
  public String getValue() {
    final String value = ((TextInputPanel) getInputComponent()).getText();

    return value.length() == 0 ? null : value;
  }

  private static TextInputPanel createTextInputPanel(final String inputDialogTitle, final ValueListProvider valueListProvider,
                                                     final Object currentValue) {
    final JTextField txtField = new JTextField(currentValue != null ? currentValue.toString() : "");
    txtField.setColumns(16);
    UiUtil.addLookupDialog(txtField, valueListProvider);

    return new TextInputPanel(txtField, inputDialogTitle);
  }
}
