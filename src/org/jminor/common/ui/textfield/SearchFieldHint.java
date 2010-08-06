/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.textfield;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Util;

import javax.swing.JTextField;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public final class SearchFieldHint {

  private final JTextField txtField;
  private final String searchHint;
  private Color defaultForegroundColor;

  public SearchFieldHint(final JTextField txtField) {
    this(txtField, Messages.get(Messages.SEARCH_FIELD_HINT));
  }

  public SearchFieldHint(final JTextField txtField, final String searchHint) {
    Util.rejectNullValue(txtField, "txtField");
    Util.rejectNullValue(searchHint, "searchHint");
    if (searchHint.isEmpty()) {
      throw new IllegalArgumentException("Search hint is null or empty");
    }
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
    final boolean showHint = !hasFocus && txtField.getText().isEmpty();
    if (hideHint) {
      txtField.setText("");
    }
    else if (showHint) {
      txtField.setText(searchHint);
    }
    final boolean specificForeground = !hasFocus && isHintVisible();
    txtField.setForeground(specificForeground ? Color.LIGHT_GRAY : defaultForegroundColor);
  }

  public boolean isHintVisible() {
    return txtField.getText().equals(searchHint);
  }

  public static SearchFieldHint enable(final JTextField txtField) {
    return new SearchFieldHint(txtField);
  }

  public static SearchFieldHint enable(final JTextField txtField, final String searchHint) {
    return new SearchFieldHint(txtField, searchHint);
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
