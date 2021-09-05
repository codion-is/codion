/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;

import javax.swing.BoundedRangeModel;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A factory for {@link ComponentBuilder}.
 */
public final class ComponentBuilders {

  private ComponentBuilders() {}

  /**
   * @return a builder for a component
   */
  public static CheckBoxBuilder checkBox() {
    return new DefaultCheckBoxBuilder();
  }

  /**
   * @return a builder for a component
   */
  public static ToggleButtonBuilder toggleButton() {
    return new DefaultToggleButtonBuilder();
  }

  /**
   * @return a builder for a component
   */
  public static BooleanComboBoxBuilder booleanComboBox() {
    return booleanComboBox(ItemComboBoxModel.createBooleanModel());
  }

  /**
   * @param comboBoxModel the combo box model
   * @return a builder for a component
   */
  public static BooleanComboBoxBuilder booleanComboBox(final ItemComboBoxModel<Boolean> comboBoxModel) {
    return new DefaultBooleanComboBoxBuilder(comboBoxModel);
  }

  /**
   * @param comboBoxModel the combo box model
   * @param <T> the value type
   * @return a builder for a component
   */
  public static <T> ItemComboBoxBuilder<T> itemComboBox(final ItemComboBoxModel<T> comboBoxModel) {
    return new DefaultItemComboBoxBuilder<>(comboBoxModel);
  }

  /**
   * @param values the values
   * @param <T> the value type
   * @return a builder for a component
   */
  public static <T> ItemComboBoxBuilder<T> itemComboBox(final List<Item<T>> values) {
    return new DefaultItemComboBoxBuilder<>(values);
  }

  /**
   * @param valueClass the value class
   * @param comboBoxModel the combo box model
   * @param <T> the value type
   * @return a builder for a component
   */
  public static <T> ComboBoxBuilder<T> comboBox(final Class<T> valueClass, final ComboBoxModel<T> comboBoxModel) {
    return new DefaultComboBoxBuilder<>(valueClass, comboBoxModel);
  }

  /**
   * @param <T> the value type
   * @param valueClass the value class
   * @return a builder for a component
   */
  public static <T extends Temporal> TemporalInputPanelBuilder<T> temporalInputPanel(final Class<T> valueClass) {
    return new DefaultTemporalInputPanelBuiler<>(valueClass, null);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalInputPanelBuilder<LocalTime> localTimeInputPanel(final String dateTimePattern) {
    return new DefaultTemporalInputPanelBuiler<>(LocalTime.class, requireNonNull(dateTimePattern));
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalInputPanelBuilder<LocalDate> localDateInputPanel(final String dateTimePattern) {
    return new DefaultTemporalInputPanelBuiler<>(LocalDate.class, requireNonNull(dateTimePattern));
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalInputPanelBuilder<LocalDateTime> localDateTimeInputPanel(final String dateTimePattern) {
    return new DefaultTemporalInputPanelBuiler<>(LocalDateTime.class, requireNonNull(dateTimePattern));
  }

  /**
   * @return a builder for a component
   */
  public static TextInputPanelBuilder textInputPanel() {
    return new DefaultTextInputPanelBuilder();
  }

  /**
   * @return a builder for a component
   */
  public static TextAreaBuilder textArea() {
    return new DefaultTextAreaBuilder();
  }

  /**
   * @param <B> the builder type
   * @return a builder for a component
   */
  public static <B extends TextFieldBuilder<String, JTextField, B>> B textField() {
    return (B) new DefaultTextFieldBuilder<String, JTextField, B>(String.class);
  }

  /**
   * @param <T> the value type
   * @param <C> the text field type
   * @param <B> the builder type
   * @param valueClass the value class
   * @return a builder for a component
   */
  public static <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> B textField(final Class<T> valueClass) {
    if (valueClass.equals(Integer.class)) {
      return (B) integerField();
    }
    else if (valueClass.equals(Long.class)) {
      return (B) longField();
    }
    else if (valueClass.equals(Double.class)) {
      return (B) doubleField();
    }
    else if (valueClass.equals(BigDecimal.class)) {
      return (B) bigDecimalField();
    }

    return (B) new DefaultTextFieldBuilder<T, C, B>(valueClass);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static LocalTimeFieldBuilder localTimeField(final String dateTimePattern) {
    return new DefaultLocalTimeFieldBuilder(dateTimePattern);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static LocalDateFieldBuilder localDateField(final String dateTimePattern) {
    return new DefaultLocalDateFieldBuilder(dateTimePattern);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static LocalDateTimeFieldBuilder localDateTimeField(final String dateTimePattern) {
    return new DefaultLocalDateTimeFieldBuilder(dateTimePattern);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static OffsetDateTimeFieldBuilder offsetDateTimeField(final String dateTimePattern) {
    return new DefaultOffsetDateTimeFieldBuilder(dateTimePattern);
  }

  /**
   * @return a builder for a component
   */
  public static IntegerFieldBuilder integerField() {
    return new DefaultIntegerFieldBuilder();
  }

  /**
   * @return a builder for a component
   */
  public static LongFieldBuilder longField() {
    return new DefaultLongFieldBuilder();
  }

  /**
   * @return a builder for a component
   */
  public static DoubleFieldBuilder doubleField() {
    return new DefaultDoubleFieldBuilder();
  }

  /**
   * @return a builder for a component
   */
  public static BigDecimalFieldBuilder bigDecimalField() {
    return new DefaultBigDecimalFieldBuilder();
  }

  /**
   * @return a builder for a component
   */
  public static FormattedTextFieldBuilder formattedTextField() {
    return new DefaultFormattedTextFieldBuilder();
  }

  /**
   * @return a builder for a Double based JSpinner
   */
  public static NumberSpinnerBuilder<Double> doubleSpinner() {
    return doubleSpinner(new SpinnerNumberModel());
  }

  /**
   * @param spinnerNumberModel the spinner model
   * @return a builder for a Double based JSpinner
   */
  public static NumberSpinnerBuilder<Double> doubleSpinner(final SpinnerNumberModel spinnerNumberModel) {
    return new DefaultNumberSpinnerBuilder<>(spinnerNumberModel, Double.class);
  }

  /**
   * @return a builder for a Integer based JSpinner
   */
  public static NumberSpinnerBuilder<Integer> integerSpinner() {
    return integerSpinner(new SpinnerNumberModel());
  }

  /**
   * @param spinnerNumberModel the spinner model
   * @return a builder for a Integer based JSpinner
   */
  public static NumberSpinnerBuilder<Integer> integerSpinner(final SpinnerNumberModel spinnerNumberModel) {
    return new DefaultNumberSpinnerBuilder<>(spinnerNumberModel, Integer.class);
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @return a builder for a JSpinner
   */
  public static <T> ListSpinnerBuilder<T> listSpinner(final SpinnerListModel spinnerModel) {
    return new DefaultListSpinnerBuilder<>(spinnerModel);
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @return a builder for a JSpinner
   */
  public static <T> ItemSpinnerBuilder<T> itemSpinner(final SpinnerListModel spinnerModel) {
    return new DefaultItemSpinnerBuilder<>(spinnerModel);
  }

  /**
   * @param boundedRangeModel the slider model
   * @return a builder for a component
   */
  public static SliderBuilder slider(final BoundedRangeModel boundedRangeModel) {
    return new DefaultSliderBuilder(boundedRangeModel);
  }

  /**
   * @param <T> the value type
   * @param listModel the list model
   * @return a builder for a JList
   */
  public static <T> SingleSelectionListBuilder<T> listSingleSelection(final ListModel<T> listModel) {
    return new DefaultSingleSelectionListBuilder<>(listModel);
  }

  /**
   * @return a label builder
   */
  public static LabelBuilder label() {
    return label((String) null);
  }

  /**
   * @param icon the label icon
   * @return a label builder
   */
  public static LabelBuilder label(final Icon icon) {
    return new DefaultLabelBuilder(icon);
  }

  /**
   * @param text the label text
   * @return a label builder
   */
  public static LabelBuilder label(final String text) {
    return new DefaultLabelBuilder(text);
  }

  /**
   * Returns a generic component builder, for configuring components. Configures and returns the given component on build.
   * @param component the component to configure
   * @param <T> the value type
   * @param <C> the component type
   * @param <B> the builder type
   * @return a generic builder, returning the given component on build
   */
  public static <T, C extends JComponent, B extends ComponentBuilder<T, C, B>> ComponentBuilder<T, C, B> builder(final C component) {
    return new DefaultComponentBuilder<>(requireNonNull(component, "component"));
  }
}
