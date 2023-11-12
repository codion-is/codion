/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;

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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A panel for Temporal input with a button for displaying a calendar
 * @param <T> the Temporal type supplied by this panel
 * @see #supports(Class)
 */
public final class TemporalInputPanel<T extends Temporal> extends JPanel {

  private static final List<Class<?>> SUPPORTED_TYPES = Arrays.asList(LocalDate.class, LocalDateTime.class);

  private final TemporalField<T> temporalField;
  private final JButton calendarButton;

  TemporalInputPanel(DefaultBuilder<T> builder) {
    super(new BorderLayout());
    this.temporalField = requireNonNull(builder.createTemporalField());
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
   * {@link TemporalInputPanel} supports {@link LocalDate} and {@link LocalDateTime}.
   * @param temporalClass the temporal type
   * @return true if {@link TemporalInputPanel} supports the given type
   * @param <T> the temporal type
   */
  public static <T extends Temporal> boolean supports(Class<T> temporalClass) {
    return SUPPORTED_TYPES.contains(requireNonNull(temporalClass));
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
          extends AbstractComponentBuilder<T, TemporalInputPanel<T>, Builder<T>>
          implements Builder<T> {

    private final Class<T> valueClass;
    private final String dateTimePattern;

    private int columns;
    private UpdateOn updateOn = UpdateOn.VALUE_CHANGE;
    private boolean selectAllOnFocusGained;
    private boolean buttonFocusable;
    private ImageIcon calendarIcon;

    private DefaultBuilder(Class<T> valueClass, String dateTimePattern, Value<T> linkedValue) {
      super(linkedValue);
      if (!supports(valueClass)) {
        throw new IllegalArgumentException("Unsupported temporal type: " + valueClass);
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
    public Builder<T> calendarIcon(ImageIcon calendarIcon) {
      this.calendarIcon = calendarIcon;
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
              .calendarIcon(calendarIcon)
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
