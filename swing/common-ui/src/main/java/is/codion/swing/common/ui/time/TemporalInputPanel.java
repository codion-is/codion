/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.time;

import is.codion.common.DateTimeParser;
import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.Components;

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

  /**
   * Specifies whether a {@link TemporalInputPanel} should contain a button for opening a Calendar for input entry.
   * Only applies to temporal values containing a date part, as in, not those that contain time only.
   */
  public enum CalendarButton {
    /**
     * Include a calendar button.
     */
    YES,
    /**
     * Don't include a calendar button.
     */
    NO
  }

  private final JFormattedTextField inputField;
  private final String dateFormat;
  private final DateTimeFormatter formatter;
  private final DateTimeParser<T> dateTimeParser;

  /**
   * Instantiates a new TemporalInputPanel.
   * @param inputField the input field
   * @param dateFormat the date format
   * @param dateTimeParser the dateTimeParser
   * @param enabledState a StateObserver controlling the enabled state of the input field and button
   */
  public TemporalInputPanel(final JFormattedTextField inputField, final String dateFormat,
                            final DateTimeParser<T> dateTimeParser, final StateObserver enabledState) {
    super(new BorderLayout());
    this.inputField = requireNonNull(inputField, "inputField");
    this.dateFormat = requireNonNull(dateFormat, "dateFormat");
    this.dateTimeParser = requireNonNull(dateTimeParser, "dateTimeParser");
    this.formatter = DateTimeFormatter.ofPattern(dateFormat);
    add(inputField, BorderLayout.CENTER);
    addFocusListener(new InputFocusAdapter(inputField));
    if (enabledState != null) {
      Components.linkToEnabledState(enabledState, inputField);
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
      return dateTimeParser.parse(text, formatter);
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
