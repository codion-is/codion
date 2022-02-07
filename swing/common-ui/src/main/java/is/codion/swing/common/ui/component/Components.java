/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.textfield.TemporalField;

import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import java.awt.LayoutManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A factory for {@link ComponentBuilder}.
 */
public final class Components {

  private Components() {}

  /**
   * @param <B> the builder type
   * @return a builder for a JButton
   */
  public static <B extends ButtonBuilder<Void, JButton, B>> ButtonBuilder<Void, JButton, B> button() {
    return button(null);
  }

  /**
   * @param <B> the builder type
   * @param action the button action
   * @return a builder for a JButton
   */
  public static <B extends ButtonBuilder<Void, JButton, B>> ButtonBuilder<Void, JButton, B> button(final Action action) {
    return new DefaultButtonBuilder<>(action);
  }

  /**
   * @return a builder for a component
   */
  public static CheckBoxBuilder checkBox() {
    return new DefaultCheckBoxBuilder(null);
  }

  /**
   * @param linkedValue the value to link to the check-box
   * @return a builder for a component
   */
  public static CheckBoxBuilder checkBox(final Value<Boolean> linkedValue) {
    return new DefaultCheckBoxBuilder(linkedValue);
  }

  /**
   * @return a builder for a component
   */
  public static RadioButtonBuilder radioButton() {
    return new DefaultRadioButtonBuilder(null);
  }

  /**
   * @param linkedValue the value to link to the radion button
   * @return a builder for a component
   */
  public static RadioButtonBuilder radioButton(final Value<Boolean> linkedValue) {
    return new DefaultRadioButtonBuilder(linkedValue);
  }

  /**
   * @param <B> the builder type
   * @return a builder for a component
   */
  public static <B extends ButtonBuilder<Boolean, JToggleButton, B>> ButtonBuilder<Boolean, JToggleButton, B> toggleButton() {
    return new DefaultToggleButtonBuilder<>(null);
  }

  /**
   * @param linkedValue the value to link to the button
   * @param <B> the builder type
   * @return a builder for a component
   */
  public static <B extends ButtonBuilder<Boolean, JToggleButton, B>> ButtonBuilder<Boolean, JToggleButton, B> toggleButton(final Value<Boolean> linkedValue) {
    return new DefaultToggleButtonBuilder<>(linkedValue);
  }

  /**
   * @return a builder for a component
   */
  public static ItemComboBoxBuilder<Boolean> booleanComboBox() {
    return booleanComboBox((Value<Boolean>) null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static ItemComboBoxBuilder<Boolean> booleanComboBox(final Value<Boolean> linkedValue) {
    return booleanComboBox(ItemComboBoxModel.createBooleanModel(), linkedValue);
  }

  /**
   * @param comboBoxModel the combo box model
   * @return a builder for a component
   */
  public static ItemComboBoxBuilder<Boolean> booleanComboBox(final ItemComboBoxModel<Boolean> comboBoxModel) {
    return booleanComboBox(comboBoxModel, null);
  }

  /**
   * @param comboBoxModel the combo box model
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static ItemComboBoxBuilder<Boolean> booleanComboBox(final ItemComboBoxModel<Boolean> comboBoxModel,
                                                             final Value<Boolean> linkedValue) {
    return new DefaultItemComboBoxBuilder<>(comboBoxModel, linkedValue);
  }

  /**
   * @param comboBoxModel the combo box model
   * @param <T> the value type
   * @return a builder for a component
   */
  public static <T> ItemComboBoxBuilder<T> itemComboBox(final ItemComboBoxModel<T> comboBoxModel) {
    return itemComboBox(comboBoxModel, null);
  }

  /**
   * @param comboBoxModel the combo box model
   * @param linkedValue the value to link to the component
   * @param <T> the value type
   * @return a builder for a component
   */
  public static <T> ItemComboBoxBuilder<T> itemComboBox(final ItemComboBoxModel<T> comboBoxModel,
                                                        final Value<T> linkedValue) {
    return new DefaultItemComboBoxBuilder<>(comboBoxModel, linkedValue);
  }

  /**
   * @param values the values
   * @param <T> the value type
   * @return a builder for a component
   */
  public static <T> ItemComboBoxBuilder<T> itemComboBox(final List<Item<T>> values) {
    return itemComboBox(values, null);
  }

  /**
   * @param values the values
   * @param linkedValue the value to link to the component
   * @param <T> the value type
   * @return a builder for a component
   */
  public static <T> ItemComboBoxBuilder<T> itemComboBox(final List<Item<T>> values, final Value<T> linkedValue) {
    return new DefaultItemComboBoxBuilder<>(values, linkedValue);
  }

  /**
   * @param <T> the value type
   * @param <C> the component type
   * @param <B> the builder type
   * @param comboBoxModel the combo box model
   * @return a builder for a component
   */
  public static <T, C extends SteppedComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> comboBox(final ComboBoxModel<T> comboBoxModel) {
    return comboBox(comboBoxModel, null);
  }

  /**
   * @param comboBoxModel the combo box model
   * @param linkedValue the value to link to the component
   * @param <T> the value type
   * @param <C> the component type
   * @param <B> the builder type
   * @return a builder for a component
   */
  public static <T, C extends SteppedComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> comboBox(final ComboBoxModel<T> comboBoxModel, final Value<T> linkedValue) {
    return new DefaultComboBoxBuilder<>(comboBoxModel, linkedValue);
  }

  /**
   * @param <T> the value type
   * @param valueClass the value class
   * @return a builder for a component
   */
  public static <T extends Temporal> TemporalInputPanelBuilder<T> temporalInputPanel(final Class<T> valueClass) {
    return temporalInputPanel(valueClass, null);
  }

  /**
   * @param <T> the value type
   * @param valueClass the value class
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static <T extends Temporal> TemporalInputPanelBuilder<T> temporalInputPanel(final Class<T> valueClass,
                                                                                     final Value<T> linkedValue) {
    return new DefaultTemporalInputPanelBuiler<>(valueClass, null, linkedValue);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalInputPanelBuilder<LocalTime> localTimeInputPanel(final String dateTimePattern) {
    return localTimeInputPanel(dateTimePattern, null);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a temporal component
   */
  public static TemporalInputPanelBuilder<LocalTime> localTimeInputPanel(final String dateTimePattern,
                                                                         final Value<LocalTime> linkedValue) {
    return new DefaultTemporalInputPanelBuiler<>(LocalTime.class, requireNonNull(dateTimePattern), linkedValue);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalInputPanelBuilder<LocalDate> localDateInputPanel(final String dateTimePattern) {
    return localDateInputPanel(dateTimePattern, null);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a temporal component
   */
  public static TemporalInputPanelBuilder<LocalDate> localDateInputPanel(final String dateTimePattern,
                                                                         final Value<LocalDate> linkedValue) {
    return new DefaultTemporalInputPanelBuiler<>(LocalDate.class, requireNonNull(dateTimePattern), linkedValue);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalInputPanelBuilder<LocalDateTime> localDateTimeInputPanel(final String dateTimePattern) {
    return localDateTimeInputPanel(dateTimePattern, null);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a temporal component
   */
  public static TemporalInputPanelBuilder<LocalDateTime> localDateTimeInputPanel(final String dateTimePattern,
                                                                                 final Value<LocalDateTime> linkedValue) {
    return new DefaultTemporalInputPanelBuiler<>(LocalDateTime.class, requireNonNull(dateTimePattern), linkedValue);
  }

  /**
   * @return a builder for a component
   */
  public static TextInputPanelBuilder textInputPanel() {
    return textInputPanel(null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static TextInputPanelBuilder textInputPanel(final Value<String> linkedValue) {
    return new DefaultTextInputPanelBuilder(linkedValue);
  }

  /**
   * @return a builder for a component
   */
  public static TextAreaBuilder textArea() {
    return textArea(null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static TextAreaBuilder textArea(final Value<String> linkedValue) {
    return new DefaultTextAreaBuilder(linkedValue);
  }

  /**
   * @param <B> the builder type
   * @return a builder for a component
   */
  public static <B extends TextFieldBuilder<String, JTextField, B>> B textField() {
    return (B) new DefaultTextFieldBuilder<String, JTextField, B>(String.class, null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @param <B> the builder type
   * @return a builder for a component
   */
  public static <B extends TextFieldBuilder<String, JTextField, B>> B textField(final Value<String> linkedValue) {
    return (B) new DefaultTextFieldBuilder<String, JTextField, B>(String.class, linkedValue);
  }

  /**
   * @param <T> the value type
   * @param <C> the text field type
   * @param <B> the builder type
   * @param valueClass the value class
   * @return a builder for a component
   */
  public static <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> B textField(final Class<T> valueClass) {
    return textField(valueClass, null);
  }

  /**
   * @param <T> the value type
   * @param <C> the text field type
   * @param <B> the builder type
   * @param valueClass the value class
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> B textField(final Class<T> valueClass,
                                                                                           final Value<T> linkedValue) {
    if (valueClass.equals(Integer.class)) {
      return (B) integerField((Value<Integer>) linkedValue);
    }
    else if (valueClass.equals(Long.class)) {
      return (B) longField((Value<Long>) linkedValue);
    }
    else if (valueClass.equals(Double.class)) {
      return (B) doubleField((Value<Double>) linkedValue);
    }
    else if (valueClass.equals(BigDecimal.class)) {
      return (B) bigDecimalField((Value<BigDecimal>) linkedValue);
    }

    return (B) new DefaultTextFieldBuilder<T, C, B>(valueClass, linkedValue);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalFieldBuilder<LocalTime, TemporalField<LocalTime>> localTimeField(final String dateTimePattern) {
    return localTimeField(dateTimePattern, null);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a temporal component
   */
  public static TemporalFieldBuilder<LocalTime, TemporalField<LocalTime>> localTimeField(final String dateTimePattern, final Value<LocalTime> linkedValue) {
    return new DefaultTemporalFieldBuilder<>(LocalTime.class, dateTimePattern, linkedValue);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalFieldBuilder<LocalDate, TemporalField<LocalDate>> localDateField(final String dateTimePattern) {
    return localDateField(dateTimePattern, null);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a temporal component
   */
  public static TemporalFieldBuilder<LocalDate, TemporalField<LocalDate>> localDateField(final String dateTimePattern, final Value<LocalDate> linkedValue) {
    return new DefaultTemporalFieldBuilder<>(LocalDate.class, dateTimePattern, linkedValue);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalFieldBuilder<LocalDateTime, TemporalField<LocalDateTime>> localDateTimeField(final String dateTimePattern) {
    return localDateTimeField(dateTimePattern, null);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a temporal component
   */
  public static TemporalFieldBuilder<LocalDateTime, TemporalField<LocalDateTime>> localDateTimeField(final String dateTimePattern,
                                                                                                     final Value<LocalDateTime> linkedValue) {
    return new DefaultTemporalFieldBuilder<>(LocalDateTime.class, dateTimePattern, linkedValue);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalFieldBuilder<OffsetDateTime, TemporalField<OffsetDateTime>> offsetDateTimeField(final String dateTimePattern) {
    return offsetDateTimeField(dateTimePattern, null);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a temporal component
   */
  public static TemporalFieldBuilder<OffsetDateTime, TemporalField<OffsetDateTime>> offsetDateTimeField(final String dateTimePattern,
                                                                                                        final Value<OffsetDateTime> linkedValue) {
    return new DefaultTemporalFieldBuilder<>(OffsetDateTime.class, dateTimePattern, linkedValue);
  }

  /**
   * @return a builder for a component
   */
  public static IntegerFieldBuilder integerField() {
    return integerField(null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static IntegerFieldBuilder integerField(final Value<Integer> linkedValue) {
    return new DefaultIntegerFieldBuilder(linkedValue);
  }

  /**
   * @return a builder for a component
   */
  public static LongFieldBuilder longField() {
    return longField(null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static LongFieldBuilder longField(final Value<Long> linkedValue) {
    return new DefaultLongFieldBuilder(linkedValue);
  }

  /**
   * @return a builder for a component
   */
  public static DoubleFieldBuilder doubleField() {
    return doubleField(null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static DoubleFieldBuilder doubleField(final Value<Double> linkedValue) {
    return new DefaultDoubleFieldBuilder(linkedValue);
  }

  /**
   * @return a builder for a component
   */
  public static BigDecimalFieldBuilder bigDecimalField() {
    return bigDecimalField(null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static BigDecimalFieldBuilder bigDecimalField(final Value<BigDecimal> linkedValue) {
    return new DefaultBigDecimalFieldBuilder(linkedValue);
  }

  /**
   * @return a builder for a component
   */
  public static FormattedTextFieldBuilder formattedTextField() {
    return formattedTextField(null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static FormattedTextFieldBuilder formattedTextField(final Value<String> linkedValue) {
    return new DefaultFormattedTextFieldBuilder(linkedValue);
  }

  /**
   * @return a builder for a Double based JSpinner
   */
  public static NumberSpinnerBuilder<Double> doubleSpinner() {
    return doubleSpinner((Value<Double>) null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a Double based JSpinner
   */
  public static NumberSpinnerBuilder<Double> doubleSpinner(final Value<Double> linkedValue) {
    return doubleSpinner(new SpinnerNumberModel(), linkedValue);
  }

  /**
   * @param spinnerNumberModel the spinner model
   * @return a builder for a Double based JSpinner
   */
  public static NumberSpinnerBuilder<Double> doubleSpinner(final SpinnerNumberModel spinnerNumberModel) {
    return doubleSpinner(spinnerNumberModel, null);
  }

  /**
   * @param spinnerNumberModel the spinner model
   * @param linkedValue the value to link to the component
   * @return a builder for a Double based JSpinner
   */
  public static NumberSpinnerBuilder<Double> doubleSpinner(final SpinnerNumberModel spinnerNumberModel,
                                                           final Value<Double> linkedValue) {
    return new DefaultNumberSpinnerBuilder<>(spinnerNumberModel, Double.class, linkedValue);
  }

  /**
   * @return a builder for a Integer based JSpinner
   */
  public static NumberSpinnerBuilder<Integer> integerSpinner() {
    return integerSpinner((Value<Integer>) null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a Integer based JSpinner
   */
  public static NumberSpinnerBuilder<Integer> integerSpinner(final Value<Integer> linkedValue) {
    return integerSpinner(new SpinnerNumberModel(), linkedValue);
  }

  /**
   * @param spinnerNumberModel the spinner model
   * @return a builder for a Integer based JSpinner
   */
  public static NumberSpinnerBuilder<Integer> integerSpinner(final SpinnerNumberModel spinnerNumberModel) {
    return integerSpinner(spinnerNumberModel, null);
  }

  /**
   * @param spinnerNumberModel the spinner model
   * @param linkedValue the value to link to the component
   * @return a builder for a Integer based JSpinner
   */
  public static NumberSpinnerBuilder<Integer> integerSpinner(final SpinnerNumberModel spinnerNumberModel,
                                                             final Value<Integer> linkedValue) {
    return new DefaultNumberSpinnerBuilder<>(spinnerNumberModel, Integer.class, linkedValue);
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @return a builder for a JSpinner
   */
  public static <T> ListSpinnerBuilder<T> listSpinner(final SpinnerListModel spinnerModel) {
    return listSpinner(spinnerModel, null);
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @param linkedValue the value to link to the component
   * @return a builder for a JSpinner
   */
  public static <T> ListSpinnerBuilder<T> listSpinner(final SpinnerListModel spinnerModel, final Value<T> linkedValue) {
    return new DefaultListSpinnerBuilder<>(spinnerModel, linkedValue);
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @return a builder for a JSpinner
   */
  public static <T> ItemSpinnerBuilder<T> itemSpinner(final SpinnerListModel spinnerModel) {
    return itemSpinner(spinnerModel, null);
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @param linkedValue the value to link to the component
   * @return a builder for a JSpinner
   */
  public static <T> ItemSpinnerBuilder<T> itemSpinner(final SpinnerListModel spinnerModel, final Value<T> linkedValue) {
    return new DefaultItemSpinnerBuilder<>(spinnerModel, linkedValue);
  }

  /**
   * @param boundedRangeModel the slider model
   * @return a builder for a component
   */
  public static SliderBuilder slider(final BoundedRangeModel boundedRangeModel) {
    return slider(boundedRangeModel, null);
  }

  /**
   * @param boundedRangeModel the slider model
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static SliderBuilder slider(final BoundedRangeModel boundedRangeModel, final Value<Integer> linkedValue) {
    return new DefaultSliderBuilder(boundedRangeModel, linkedValue);
  }

  /**
   * A single selection JList builder.
   * @param <T> the value type
   * @param listModel the list model
   * @return a builder for a JList
   */
  public static <T> ListBuilder<T> list(final ListModel<T> listModel) {
    return list(listModel, null);
  }

  /**
   * A single selection JList builder.
   * @param <T> the value type
   * @param listModel the list model
   * @param linkedValue the value to link to the component
   * @return a builder for a JList
   */
  public static <T> ListBuilder<T> list(final ListModel<T> listModel, final Value<T> linkedValue) {
    return new DefaultListBuilder<>(listModel, linkedValue);
  }

  /**
   * @return a label builder
   */
  public static LabelBuilder label() {
    return label((String) null);
  }

  /**
   * @param linkedValueObserver the value observer to link to the label text
   * @return a label builder
   */
  public static LabelBuilder label(final ValueObserver<String> linkedValueObserver) {
    return new DefaultLabelBuilder(null, linkedValueObserver);
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
    return new DefaultLabelBuilder(text, null);
  }

  /**
   * @return a panel builder
   */
  public static PanelBuilder panel() {
    return new DefaultPanelBuilder((LayoutManager) null);
  }

  /**
   * @param layout the panel layout manager
   * @return a panel builder
   */
  public static PanelBuilder panel(final LayoutManager layout) {
    return new DefaultPanelBuilder(requireNonNull(layout));
  }

  /**
   * @param panel the panel to configure
   * @return a panel builder
   */
  public static PanelBuilder panel(final JPanel panel) {
    return new DefaultPanelBuilder(panel);
  }

  /**
   * @return a tabbed pane builder
   */
  public static TabbedPaneBuilder tabbedPane() {
    return new DefaultTabbedPaneBuilder();
  }

  /**
   * @return a split pane builder
   */
  public static SplitPaneBuilder splitPane() {
    return new DefaultSplitPaneBuilder();
  }

  /**
   * Returns a generic component builder, for configuring components. Configures and returns the given component on build.
   * @param component the component to configure
   * @param <T> the value type
   * @param <C> the component type
   * @param <B> the builder type
   * @return a generic builder, returning the given component on build
   */
  public static <T, C extends JComponent, B extends ComponentBuilder<T, C, B>> ComponentBuilder<T, C, B> component(final C component) {
    return new DefaultComponentBuilder<>(requireNonNull(component, "component"));
  }
}
