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

/**
 * Implements a search hint for text fields, that is, text that is shown
 * when the field is empty an unfocused.
 */
public final class SearchFieldHint {

  private final JTextField txtField;
  private final String searchHint;
  private Color defaultForegroundColor;

  /**
   * Instantiates a new SearchFieldHint for the given field.
   * @param txtField the text field
   */
  public SearchFieldHint(final JTextField txtField) {
    this(txtField, Messages.get(Messages.SEARCH_FIELD_HINT));
  }

  /**
   * Instantiates a new SearchFieldHint for the given field.
   * @param txtField the text field
   * @param searchHint the search hint
   */
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

  /**
   * @return the search hint string
   */
  public String getSearchHint() {
    return searchHint;
  }

  /**
   * Updates the hint state for the component
   */
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

  /**
   * @return true if the hint is visible
   */
  public boolean isHintVisible() {
    return txtField.getText().equals(searchHint);
  }

  /**
   * Enables the search hint for the given field
   * @param txtField the text field
   * @return the SearchFieldHint instance
   */
  public static SearchFieldHint enable(final JTextField txtField) {
    return new SearchFieldHint(txtField);
  }

  /**
   * Enables the search hint for the given field
   * @param txtField the text field
   * @param searchHint the search hint
   * @return the SearchFieldHint instance
   */
  public static SearchFieldHint enable(final JTextField txtField, final String searchHint) {
    return new SearchFieldHint(txtField, searchHint);
  }

  private FocusListener initializeFocusListener() {
    return new FocusListener() {
      /** {@inheritDoc} */
      public void focusGained(final FocusEvent e) {
        updateState();
      }
      /** {@inheritDoc} */
      public void focusLost(final FocusEvent e) {
        updateState();
      }
    };
  }
}
