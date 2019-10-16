/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.DateFormats;
import org.jminor.common.StateObserver;

import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

import static java.util.Objects.requireNonNull;

/**
 * A panel for Temporal input
 * @param <T> the Temporal type supplied by this panel
 */
public class TemporalInputPanel<T extends Temporal> extends JPanel {

  private final JFormattedTextField inputField;
  private final String dateFormat;
  private final DateTimeFormatter formatter;
  private final DateFormats.DateParser<T> dateParser;

  /**
   * Instantiates a new TemporalInputPanel.
   * @param inputField the input field
   * @param dateFormat the date format
   * @param dateParser the dateParser
   * @param enabledState a StateObserver controlling the enabled state of the input field and button
   */
  public TemporalInputPanel(final JFormattedTextField inputField, final String dateFormat,
                            final DateFormats.DateParser<T> dateParser, final StateObserver enabledState) {
    super(new BorderLayout());
    this.inputField = requireNonNull(inputField, "inputField");
    this.dateFormat = requireNonNull(dateFormat, "dateFormat");
    this.dateParser = requireNonNull(dateParser, "dateParser");
    this.formatter = DateTimeFormatter.ofPattern(dateFormat);
    add(inputField, BorderLayout.CENTER);
    addFocusListener(new InputFocusAdapter(inputField));
    if (enabledState != null) {
      UiUtil.linkToEnabledState(enabledState, inputField);
    }
  }

  /**
   * @return the input field
   */
  public final JFormattedTextField getInputField() {
    return inputField;
  }

  /**
   * @return the Date currently being displayed, null in case of an incomplete date
   * @throws DateTimeParseException if unable to parse the text
   */
  public final T getTemporal() throws DateTimeParseException {
    final String text = inputField.getText();
    if (!text.contains("_")) {
      return dateParser.parse(text, formatter);
    }

    return null;
  }

    /**
   * Sets the date in the input field, clears the field if {@code date} is null.
   * @param date the date to set
   */
  public final void setTemporal(final Temporal date) {
    inputField.setText(date == null ? "" : formatter.format(date));
  }

  /**
   * @return the format pattern
   */
  public final String getDateFormat() {
    return dateFormat;
  }

  /**
   * @param editable if true then editing is enabled in this panel
   */
  public void setEditable(final boolean editable) {
    inputField.setEditable(editable);
  }

  /**
   * @return the formatter
   */
  protected final DateTimeFormatter getFormatter() {
    return formatter;
  }

  private static final class InputFocusAdapter extends FocusAdapter {
    private final JFormattedTextField inputField;

    private InputFocusAdapter(final JFormattedTextField inputField) {
      this.inputField = inputField;
    }

    @Override
    public void focusGained(final FocusEvent e) {
      inputField.requestFocusInWindow();
    }
  }
}
