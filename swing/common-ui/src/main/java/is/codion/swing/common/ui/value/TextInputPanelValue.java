/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.model.textfield.DocumentAdapter;
import is.codion.swing.common.ui.textfield.TextInputPanel;

class TextInputPanelValue extends AbstractComponentValue<String, TextInputPanel> {

  TextInputPanelValue(final TextInputPanel textInputPanel) {
    super(textInputPanel);
    textInputPanel.getTextField().getDocument().addDocumentListener((DocumentAdapter) e -> notifyValueChange());
  }

  @Override
  protected String getComponentValue(final TextInputPanel component) {
    final String value = component.getText();

    return value.isEmpty() ? null : value;
  }

  @Override
  protected void setComponentValue(final TextInputPanel component, final String value) {
    component.setText(value);
  }
}
