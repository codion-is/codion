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
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.calendar.CalendarPanel;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;

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
import java.time.temporal.Temporal;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A panel for a TemporalField with button for displaying a calendar
 * @param <T> the Temporal type supplied by this panel
 * @see #supports(Class)
 */
public final class TemporalFieldPanel<T extends Temporal> extends JPanel {

  private final TemporalField<T> temporalField;
  private final JButton calendarButton;

  TemporalFieldPanel(DefaultBuilder<T> builder) {
    super(new BorderLayout());
    temporalField = requireNonNull(builder.createTemporalField());
    add(temporalField, BorderLayout.CENTER);
    Control calendarControl = temporalField.calendarControl()
            .orElseThrow(() -> new IllegalArgumentException("TemporalField does not support a calendar for: " + temporalField.temporalClass()));
    calendarButton = new JButton(calendarControl);
    calendarButton.setPreferredSize(new Dimension(temporalField.getPreferredSize().height, temporalField.getPreferredSize().height));
    calendarButton.setFocusable(builder.buttonFocusable);
    add(calendarButton, BorderLayout.EAST);
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
  public JButton calendarButton() {
    return calendarButton;
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
  }

  @Override
  public void setToolTipText(String text) {
    temporalField.setToolTipText(text);
  }

  /**
   * {@link TemporalFieldPanel} supports {@link LocalDate} and {@link LocalDateTime}.
   * @param temporalClass the temporal type
   * @return true if {@link TemporalFieldPanel} supports the given type
   * @param <T> the temporal type
   */
  public static <T extends Temporal> boolean supports(Class<T> temporalClass) {
    return CalendarPanel.supportedTypes().contains(requireNonNull(temporalClass));
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

  /**
   * Builds a {@link TemporalFieldPanel}
   * @param <T> the temporal type
   */
  public interface Builder<T extends Temporal> extends ComponentBuilder<T, TemporalFieldPanel<T>, Builder<T>> {

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
     * Default false.
     * @param buttonFocusable true if the calendar button should be focusable
     * @return this builder instance
     */
    Builder<T> buttonFocusable(boolean buttonFocusable);

    /**
     * @param calendarIcon the calendar icon
     * @return this builder instance
     */
    Builder<T> calendarIcon(ImageIcon calendarIcon);
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
          extends AbstractComponentBuilder<T, TemporalFieldPanel<T>, Builder<T>>
          implements Builder<T> {

    private final TemporalField.Builder<T> temporalFieldBuilder;

    private boolean buttonFocusable;

    private DefaultBuilder(Class<T> valueClass, String dateTimePattern, Value<T> linkedValue) {
      super(linkedValue);
      if (!supports(valueClass)) {
        throw new IllegalArgumentException("Unsupported temporal type: " + valueClass);
      }
      temporalFieldBuilder = TemporalField.builder(valueClass, requireNonNull(dateTimePattern));
    }

    @Override
    public Builder<T> selectAllOnFocusGained(boolean selectAllOnFocusGained) {
      temporalFieldBuilder.selectAllOnFocusGained(selectAllOnFocusGained);
      return this;
    }

    @Override
    public Builder<T> columns(int columns) {
      temporalFieldBuilder.columns(columns);
      return this;
    }

    @Override
    public Builder<T> updateOn(UpdateOn updateOn) {
      temporalFieldBuilder.updateOn(updateOn);
      return this;
    }

    @Override
    public Builder<T> buttonFocusable(boolean buttonFocusable) {
      this.buttonFocusable = buttonFocusable;
      return this;
    }

    @Override
    public Builder<T> calendarIcon(ImageIcon calendarIcon) {
      temporalFieldBuilder.calendarIcon(calendarIcon);
      return this;
    }

    @Override
    protected TemporalFieldPanel<T> createComponent() {
      return new TemporalFieldPanel<>(this);
    }

    @Override
    protected ComponentValue<T, TemporalFieldPanel<T>> createComponentValue(TemporalFieldPanel<T> component) {
      return new TemporalFieldPanelValue<>(component);
    }

    @Override
    protected void enableTransferFocusOnEnter(TemporalFieldPanel<T> component) {
      component.setTransferFocusOnEnter(true);
    }

    @Override
    protected void setInitialValue(TemporalFieldPanel<T> component, T initialValue) {
      component.setTemporal(initialValue);
    }

    private TemporalField<T> createTemporalField() {
      return temporalFieldBuilder.clear().build();
    }
  }

  private static final class TemporalFieldPanelValue<T extends Temporal> extends AbstractComponentValue<T, TemporalFieldPanel<T>> {

    private TemporalFieldPanelValue(TemporalFieldPanel<T> inputPanel) {
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
