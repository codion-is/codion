/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import org.jminor.common.model.ValueListProvider;
import org.jminor.common.ui.TextInputPanel;
import org.jminor.common.ui.UiUtil;

import javax.swing.JTextField;

/**
 * A InputManager implementation for String values.
 */
public class TextInputProvider extends InputValueProvider {

  public TextInputProvider(final String inputDialogTitle, final ValueListProvider valueListProvider, final String currentValue) {
    super(createTextInputPanel(inputDialogTitle, valueListProvider, currentValue));
  }

  @Override
  public Object getValue() {
    return ((TextInputPanel) getInputComponent()).getText();
  }

  private static TextInputPanel createTextInputPanel(final String inputDialogTitle, final ValueListProvider valueListProvider,
                                                     final Object currentValue) {
    final JTextField txtField = new JTextField(currentValue != null ? currentValue.toString() : "");
    txtField.setColumns(16);
    UiUtil.addLookupDialog(txtField, valueListProvider);

    return new TextInputPanel(txtField, inputDialogTitle);
  }
}
