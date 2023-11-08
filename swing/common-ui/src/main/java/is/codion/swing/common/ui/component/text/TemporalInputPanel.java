/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static java.awt.event.KeyEvent.VK_INSERT;
import static java.util.Objects.requireNonNull;

/**
 * A panel for Temporal input
 * @param <T> the Temporal type supplied by this panel
 */
public final class TemporalInputPanel<T extends Temporal> extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(TemporalInputPanel.class.getName());

  private final TemporalField<T> temporalField;
  private final JButton calendarButton;
  private final ImageIcon buttonIcon;

  TemporalInputPanel(DefaultBuilder<T> builder) {
    super(new BorderLayout());
    this.temporalField = requireNonNull(builder.createTemporalField());
    this.buttonIcon = builder.buttonIcon;
    add(temporalField, BorderLayout.CENTER);
    if (supportsCalendar(temporalField.temporalClass())) {
      Control displayCalendarControl = Control.builder(this::displayCalendar)
              .name(buttonIcon == null ? "..." : null)
              .smallIcon(buttonIcon)
              .description(MESSAGES.getString("display_calendar"))
              .build();
      KeyEvents.builder(VK_INSERT)
              .action(displayCalendarControl)
              .enable(temporalField);
      calendarButton = new JButton(displayCalendarControl);
      calendarButton.setPreferredSize(new Dimension(temporalField.getPreferredSize().height, temporalField.getPreferredSize().height));
      calendarButton.setFocusable(builder.buttonFocusable);
      add(calendarButton, BorderLayout.EAST);
    }
    else {
      calendarButton = null;
    }
    addFocusListener(new InputFocusAdapter(temporalField));
  }

  /**
   * @return the temporal input field
   */
  public TemporalField<T> temporalField() {
    return temporalField;
  }

  /**
   * @return the calendar button
   */
  public Optional<JButton> calendarButton() {
    return Optional.ofNullable(calendarButton);
  }

  /**
   * @return the Temporal value currently being displayed, an empty Optional in case of an incomplete/unparseable date
   */
  public Optional<T> optional() {
    return temporalField.optional();
  }

  /**
   * @return the Temporal value currently being displayed, null in case of an incomplete/unparseable date
   */
  public T getTemporal() {
    return temporalField.getTemporal();
  }

  /**
   * Sets the date in the input field, clears the field if {@code date} is null.
   * @param temporal the temporal value to set
   */
  public void setTemporal(Temporal temporal) {
    temporalField.setTemporal(temporal);
  }

  /**
   * @param transferFocusOnEnter specifies whether focus should be transferred on Enter
   */
  public void setTransferFocusOnEnter(boolean transferFocusOnEnter) {
    if (transferFocusOnEnter) {
      TransferFocusOnEnter.enable(temporalField);
      if (calendarButton != null) {
        TransferFocusOnEnter.enable(calendarButton);
      }
    }
    else {
      TransferFocusOnEnter.disable(temporalField);
      if (calendarButton != null) {
        TransferFocusOnEnter.disable(calendarButton);
      }
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    temporalField.setEnabled(enabled);
    if (calendarButton != null) {
      calendarButton.setEnabled(enabled);
    }
  }

  @Override
  public void setToolTipText(String text) {
    temporalField.setToolTipText(text);
  }

  /**
   * @param <T> the value type
   * @param valueClass the value class
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static <T extends Temporal> Builder<T> builder(Class<T> valueClass,
                                                        String dateTimePattern) {
    return new DefaultBuilder<>(valueClass, dateTimePattern, null);
  }

  /**
   * @param <T> the value type
   * @param valueClass the value class
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a temporal component
   */
  public static <T extends Temporal> Builder<T> builder(Class<T> valueClass,
                                                        String dateTimePattern,
                                                        Value<T> linkedValue) {
    return new DefaultBuilder<>(valueClass, dateTimePattern, requireNonNull(linkedValue));
  }

  private void displayCalendar() {
    if (LocalDate.class.equals(temporalField.temporalClass())) {
      Dialogs.calendarDialog()
              .owner(temporalField)
              .icon(buttonIcon)
              .initialValue((LocalDate) getTemporal())
              .selectLocalDate()
              .ifPresent(temporalField::setTemporal);
    }
    else if (LocalDateTime.class.equals(temporalField.temporalClass())) {
      Dialogs.calendarDialog()
              .owner(temporalField)
              .icon(buttonIcon)
              .initialValue((LocalDateTime) getTemporal())
              .selectLocalDateTime()
              .ifPresent(temporalField::setTemporal);
    }
    else {
      throw new IllegalArgumentException("Unsupported temporal type: " + temporalField.temporalClass());
    }
  }

  private static boolean supportsCalendar(Class<?> temporalClass) {
    return LocalDate.class.equals(temporalClass) || LocalDateTime.class.equals(temporalClass);
  }

  /**
   * Builds a TemporalInputPanel.
   * @param <T> the temporal type
   */
  public interface Builder<T extends Temporal> extends ComponentBuilder<T, TemporalInputPanel<T>, Builder<T>> {

    /**
     * @param selectAllOnFocusGained if true the component will select contents on focus gained
     * @return this builder instance
     */
    Builder<T> selectAllOnFocusGained(boolean selectAllOnFocusGained);

    /**
     * @param columns the number of colums in the temporal field
     * @return this builder instance
     */
    Builder<T> columns(int columns);

    /**
     * @param updateOn specifies when the underlying value should be updated
     * @return this builder instance
     */
    Builder<T> updateOn(UpdateOn updateOn);

    /**
     * @param buttonFocusable true if the calendar button should be focusable
     * @return this builder instance
     */
    Builder<T> buttonFocusable(boolean buttonFocusable);

    /**
     * @param buttonIcon the button icon
     * @return this builder instance
     */
    Builder<T> buttonIcon(ImageIcon buttonIcon);
  }

  private static final class InputFocusAdapter extends FocusAdapter {
    private final JFormattedTextField inputField;

    private InputFocusAdapter(JFormattedTextField inputField) {
      this.inputField = inputField;
    }

    @Override
    public void focusGained(FocusEvent e) {
      inputField.requestFocusInWindow();
    }
  }

  private static final class DefaultBuilder<T extends Temporal>
          extends AbstractComponentBuilder<T, TemporalInputPanel<T>, Builder<T>>
          implements Builder<T> {

    private static final List<Class<?>> SUPPORTED_TYPES =
            Arrays.asList(LocalTime.class, LocalDate.class, LocalDateTime.class, OffsetDateTime.class);

    private final Class<T> valueClass;
    private final String dateTimePattern;

    private int columns;
    private UpdateOn updateOn = UpdateOn.VALUE_CHANGE;
    private boolean selectAllOnFocusGained;
    private boolean buttonFocusable;
    private ImageIcon buttonIcon;

    private DefaultBuilder(Class<T> valueClass, String dateTimePattern, Value<T> linkedValue) {
      super(linkedValue);
      if (!SUPPORTED_TYPES.contains(requireNonNull(valueClass))) {
        throw new IllegalStateException("Unsupported temporal type: " + valueClass);
      }
      this.valueClass = valueClass;
      this.dateTimePattern = requireNonNull(dateTimePattern);
    }

    @Override
    public Builder<T> selectAllOnFocusGained(boolean selectAllOnFocusGained) {
      this.selectAllOnFocusGained = selectAllOnFocusGained;
      return this;
    }

    @Override
    public Builder<T> columns(int columns) {
      this.columns = columns;
      return this;
    }

    @Override
    public Builder<T> updateOn(UpdateOn updateOn) {
      this.updateOn = requireNonNull(updateOn);
      return this;
    }

    @Override
    public Builder<T> buttonFocusable(boolean buttonFocusable) {
      this.buttonFocusable = buttonFocusable;
      return this;
    }

    @Override
    public Builder<T> buttonIcon(ImageIcon buttonIcon) {
      this.buttonIcon = buttonIcon;
      return this;
    }

    @Override
    protected TemporalInputPanel<T> createComponent() {
      return new TemporalInputPanel<>(this);
    }

    @Override
    protected ComponentValue<T, TemporalInputPanel<T>> createComponentValue(TemporalInputPanel<T> component) {
      return new TemporalInputPanelValue<>(component);
    }

    @Override
    protected void enableTransferFocusOnEnter(TemporalInputPanel<T> component) {
      component.setTransferFocusOnEnter(true);
    }

    @Override
    protected void setInitialValue(TemporalInputPanel<T> component, T initialValue) {
      component.setTemporal(initialValue);
    }

    private TemporalField<T> createTemporalField() {
      return TemporalField.builder(valueClass, dateTimePattern)
              .updateOn(updateOn)
              .selectAllOnFocusGained(selectAllOnFocusGained)
              .columns(columns)
              .build();
    }
  }

  private static final class TemporalInputPanelValue<T extends Temporal> extends AbstractComponentValue<T, TemporalInputPanel<T>> {

    private TemporalInputPanelValue(TemporalInputPanel<T> inputPanel) {
      super(inputPanel);
      inputPanel.temporalField().addListener(temporal -> notifyListeners());
    }

    @Override
    protected T getComponentValue() {
      return component().getTemporal();
    }

    @Override
    protected void setComponentValue(T value) {
      component().setTemporal(value);
    }
  }
}
