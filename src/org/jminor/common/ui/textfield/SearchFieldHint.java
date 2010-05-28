/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.textfield;

import org.jminor.common.i18n.Messages;

import javax.swing.JTextField;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class SearchFieldHint {

  private final JTextField txtField;
  private final String searchHint;
  private Color defaultForegroundColor;

  public SearchFieldHint(final JTextField txtField) {
    this(txtField, Messages.get(Messages.SEARCH_FIELD_HINT));
  }

  public SearchFieldHint(final JTextField txtField, final String searchHint) {
    if (txtField == null)
      throw new IllegalArgumentException("Text field is null");
    if (searchHint == null || searchHint.length() == 0)
      throw new IllegalArgumentException("Search hint is null or empty");
    this.txtField = txtField;
    this.searchHint = searchHint;
    this.defaultForegroundColor = txtField.getForeground();
    this.txtField.addFocusListener(initializeFocusListener());
    updateState();
  }

  public String getSearchHint() {
    return searchHint;
  }

  public void updateState() {
    final boolean hasFocus = txtField.hasFocus();
    final boolean hideHint = hasFocus && txtField.getText().equals(searchHint);
    final boolean showHint = !hasFocus && txtField.getText().length() == 0;
    if (hideHint)
      txtField.setText("");
    else if (showHint)
      txtField.setText(searchHint);
    final boolean specificForeground = !hasFocus && isHintVisible();
    txtField.setForeground(specificForeground ? Color.LIGHT_GRAY : defaultForegroundColor);
  }

  public boolean isHintVisible() {
    return txtField.getText().equals(searchHint);
  }

  private FocusListener initializeFocusListener() {
    return new FocusListener() {
      public void focusGained(final FocusEvent e) {
        updateState();
      }
      public void focusLost(final FocusEvent e) {
        updateState();
      }
    };
  }
}
