/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.common.value.ValueSet;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.component.button.ButtonBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxBuilder;
import is.codion.swing.common.ui.component.button.RadioButtonBuilder;
import is.codion.swing.common.ui.component.button.ToggleButtonBuilder;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.combobox.ItemComboBoxBuilder;
import is.codion.swing.common.ui.component.list.ListBuilder;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.component.slider.SliderBuilder;
import is.codion.swing.common.ui.component.spinner.ItemSpinnerBuilder;
import is.codion.swing.common.ui.component.spinner.ListSpinnerBuilder;
import is.codion.swing.common.ui.component.spinner.NumberSpinnerBuilder;
import is.codion.swing.common.ui.component.text.FileInputPanel;
import is.codion.swing.common.ui.component.text.MaskedTextFieldBuilder;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.PasswordFieldBuilder;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.text.TemporalInputPanel;
import is.codion.swing.common.ui.component.text.TextAreaBuilder;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.component.text.TextInputPanel;

import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

import static is.codion.swing.common.model.component.combobox.ItemComboBoxModel.booleanItemComboBoxModel;
import static java.util.Objects.requireNonNull;

/**
 * A factory for {@link ComponentBuilder} instances.
 */
public final class Components {

  private Components() {}

  /**
   * @param <B> the builder type
   * @return a JButton builder
   */
  public static <B extends ButtonBuilder<Void, JButton, B>> ButtonBuilder<Void, JButton, B> button() {
    return ButtonBuilder.builder();
  }

  /**
   * @param <B> the builder type
   * @param action the button action
   * @return a JButton builder
   */
  public static <B extends ButtonBuilder<Void, JButton, B>> ButtonBuilder<Void, JButton, B> button(Action action) {
    return ButtonBuilder.builder(action);
  }

  /**
   * @return a JCheckBox builder
   */
  public static CheckBoxBuilder checkBox() {
    return CheckBoxBuilder.builder();
  }

  /**
   * @param linkedValue the value to link to the check-box
   * @return a JCheckBox builder
   */
  public static CheckBoxBuilder checkBox(Value<Boolean> linkedValue) {
    return CheckBoxBuilder.builder(linkedValue);
  }

  /**
   * @return a JRadioButton builder
   */
  public static RadioButtonBuilder radioButton() {
    return RadioButtonBuilder.builder();
  }

  /**
   * @param linkedValue the value to link to the radion button
   * @return a JRadioButton builder
   */
  public static RadioButtonBuilder radioButton(Value<Boolean> linkedValue) {
    return RadioButtonBuilder.builder(linkedValue);
  }

  /**
   * @param <B> the builder type
   * @return a JToggleButton builder
   */
  public static <B extends ToggleButtonBuilder<JToggleButton, B>> ToggleButtonBuilder<JToggleButton, B> toggleButton() {
    return ToggleButtonBuilder.builder();
  }

  /**
   * @param linkedValue the value to link to the button
   * @param <B> the builder type
   * @return a JToggleButton builder
   */
  public static <B extends ToggleButtonBuilder<JToggleButton, B>> ToggleButtonBuilder<JToggleButton, B> toggleButton(Value<Boolean> linkedValue) {
    return ToggleButtonBuilder.builder(linkedValue);
  }

  /**
   * @return a boolean based JComboBox builder
   */
  public static ItemComboBoxBuilder<Boolean> booleanComboBox() {
    return ItemComboBoxBuilder.builder(booleanItemComboBoxModel());
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a boolean based JComboBox builder
   */
  public static ItemComboBoxBuilder<Boolean> booleanComboBox(Value<Boolean> linkedValue) {
    return ItemComboBoxBuilder.builder(booleanItemComboBoxModel(), requireNonNull(linkedValue));
  }

  /**
   * @param comboBoxModel the combo box model
   * @return a boolean based JComboBox builder
   */
  public static ItemComboBoxBuilder<Boolean> booleanComboBox(ItemComboBoxModel<Boolean> comboBoxModel) {
    return ItemComboBoxBuilder.builder(comboBoxModel);
  }

  /**
   * @param comboBoxModel the combo box model
   * @param linkedValue the value to link to the component
   * @return a boolean based JComboBox builder
   */
  public static ItemComboBoxBuilder<Boolean> booleanComboBox(ItemComboBoxModel<Boolean> comboBoxModel,
                                                             Value<Boolean> linkedValue) {
    return ItemComboBoxBuilder.builder(comboBoxModel, linkedValue);
  }

  /**
   * @param comboBoxModel the combo box model
   * @param <T> the value type
   * @return a {@link Item} based JComboBox builder
   */
  public static <T> ItemComboBoxBuilder<T> itemComboBox(ItemComboBoxModel<T> comboBoxModel) {
    return ItemComboBoxBuilder.builder(comboBoxModel);
  }

  /**
   * @param comboBoxModel the combo box model
   * @param linkedValue the value to link to the component
   * @param <T> the value type
   * @return a {@link Item} based JComboBox builder
   */
  public static <T> ItemComboBoxBuilder<T> itemComboBox(ItemComboBoxModel<T> comboBoxModel,
                                                        Value<T> linkedValue) {
    return ItemComboBoxBuilder.builder(comboBoxModel, linkedValue);
  }

  /**
   * @param values the values
   * @param <T> the value type
   * @return a {@link Item} based JComboBox builder
   */
  public static <T> ItemComboBoxBuilder<T> itemComboBox(List<Item<T>> values) {
    return ItemComboBoxBuilder.builder(values);
  }

  /**
   * @param values the values
   * @param linkedValue the value to link to the component
   * @param <T> the value type
   * @return a {@link Item} based JComboBox builder
   */
  public static <T> ItemComboBoxBuilder<T> itemComboBox(List<Item<T>> values, Value<T> linkedValue) {
    return ItemComboBoxBuilder.builder(values, linkedValue);
  }

  /**
   * @param <T> the value type
   * @param <C> the component type
   * @param <B> the builder type
   * @param comboBoxModel the combo box model
   * @return a JComboBox builder
   */
  public static <T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> comboBox(ComboBoxModel<T> comboBoxModel) {
    return ComboBoxBuilder.builder(comboBoxModel);
  }

  /**
   * @param comboBoxModel the combo box model
   * @param linkedValue the value to link to the component
   * @param <T> the value type
   * @param <C> the component type
   * @param <B> the builder type
   * @return a JComboBox builder
   */
  public static <T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> comboBox(ComboBoxModel<T> comboBoxModel,
                                                                                                                  Value<T> linkedValue) {
    return ComboBoxBuilder.builder(comboBoxModel, linkedValue);
  }

  /**
   * @param <T> the value type
   * @param valueClass the value class
   * @param dateTimePattern the date time pattern
   * @return a {@link TemporalInputPanel} builder
   */
  public static <T extends Temporal> TemporalInputPanel.Builder<T> temporalInputPanel(Class<T> valueClass,
                                                                                      String dateTimePattern) {
    return TemporalInputPanel.builder(valueClass, dateTimePattern);
  }

  /**
   * @param <T> the value type
   * @param valueClass the value class
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a {@link TemporalInputPanel} builder
   */
  public static <T extends Temporal> TemporalInputPanel.Builder<T> temporalInputPanel(Class<T> valueClass,
                                                                                      String dateTimePattern,
                                                                                      Value<T> linkedValue) {
    return TemporalInputPanel.builder(valueClass, dateTimePattern, linkedValue);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a {@link LocalTime} based {@link TemporalInputPanel} builder
   */
  public static TemporalInputPanel.Builder<LocalTime> localTimeInputPanel(String dateTimePattern) {
    return temporalInputPanel(LocalTime.class, dateTimePattern);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a {@link LocalTime} based {@link TemporalInputPanel} builder
   */
  public static TemporalInputPanel.Builder<LocalTime> localTimeInputPanel(String dateTimePattern,
                                                                          Value<LocalTime> linkedValue) {
    return temporalInputPanel(LocalTime.class, dateTimePattern, linkedValue);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a {@link LocalDate} based {@link TemporalInputPanel} builder
   */
  public static TemporalInputPanel.Builder<LocalDate> localDateInputPanel(String dateTimePattern) {
    return temporalInputPanel(LocalDate.class, dateTimePattern);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a {@link LocalDate} based {@link TemporalInputPanel} builder
   */
  public static TemporalInputPanel.Builder<LocalDate> localDateInputPanel(String dateTimePattern,
                                                                          Value<LocalDate> linkedValue) {
    return temporalInputPanel(LocalDate.class, dateTimePattern, linkedValue);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a {@link LocalDateTime} based {@link TemporalInputPanel} builder
   */
  public static TemporalInputPanel.Builder<LocalDateTime> localDateTimeInputPanel(String dateTimePattern) {
    return temporalInputPanel(LocalDateTime.class, dateTimePattern);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a {@link LocalDateTime} based {@link TemporalInputPanel} builder
   */
  public static TemporalInputPanel.Builder<LocalDateTime> localDateTimeInputPanel(String dateTimePattern,
                                                                                  Value<LocalDateTime> linkedValue) {
    return temporalInputPanel(LocalDateTime.class, dateTimePattern, linkedValue);
  }

  /**
   * @return a {@link TextInputPanel} builder
   */
  public static TextInputPanel.Builder textInputPanel() {
    return TextInputPanel.builder();
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a {@link TextInputPanel} builder
   */
  public static TextInputPanel.Builder textInputPanel(Value<String> linkedValue) {
    return TextInputPanel.builder(linkedValue);
  }

  /**
   * @return a JTextArea builder
   */
  public static TextAreaBuilder textArea() {
    return TextAreaBuilder.builder();
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a JTextArea builder
   */
  public static TextAreaBuilder textArea(Value<String> linkedValue) {
    return TextAreaBuilder.builder(linkedValue);
  }

  /**
   * @param <B> the builder type
   * @return a JTextField builder
   */
  public static <B extends TextFieldBuilder<String, JTextField, B>> TextFieldBuilder<String, JTextField, B> textField() {
    return TextFieldBuilder.builder(String.class);
  }

  /**
   * @param linkedValue the value to link to the component
   * @param <B> the builder type
   * @return a JTextField builder
   */
  public static <B extends TextFieldBuilder<String, JTextField, B>> TextFieldBuilder<String, JTextField, B> textField(Value<String> linkedValue) {
    return TextFieldBuilder.builder(String.class, linkedValue);
  }

  /**
   * @param <T> the value type
   * @param <C> the text field type
   * @param <B> the builder type
   * @param valueClass the value class
   * @return a JTextField builder
   */
  public static <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> TextFieldBuilder<T, C, B> textField(Class<T> valueClass) {
    if (Number.class.isAssignableFrom(valueClass)) {
      return (TextFieldBuilder<T, C, B>) NumberField.builder((Class<Number>) valueClass);
    }

    return TextFieldBuilder.builder(valueClass);
  }

  /**
   * @param <T> the value type
   * @param <C> the text field type
   * @param <B> the builder type
   * @param valueClass the value class
   * @param linkedValue the value to link to the component
   * @return a JTextField builder
   */
  public static <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> TextFieldBuilder<T, C, B> textField(Class<T> valueClass,
                                                                                                                   Value<T> linkedValue) {
    if (Number.class.isAssignableFrom(valueClass)) {
      return (TextFieldBuilder<T, C, B>) NumberField.builder((Class<Number>) valueClass, (Value<Number>) linkedValue);
    }

    return TextFieldBuilder.builder(valueClass, linkedValue);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a {@link LocalTime} based {@link TemporalField} builder
   */
  public static TemporalField.Builder<LocalTime> localTimeField(String dateTimePattern) {
    return TemporalField.builder(LocalTime.class, dateTimePattern);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a {@link LocalTime} based {@link TemporalField} builder
   */
  public static TemporalField.Builder<LocalTime> localTimeField(String dateTimePattern,
                                                                Value<LocalTime> linkedValue) {
    return TemporalField.builder(LocalTime.class, dateTimePattern, requireNonNull(linkedValue));
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a {@link LocalDate} based {@link TemporalField} builder
   */
  public static TemporalField.Builder<LocalDate> localDateField(String dateTimePattern) {
    return TemporalField.builder(LocalDate.class, dateTimePattern);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a {@link LocalDate} based {@link TemporalField} builder
   */
  public static TemporalField.Builder<LocalDate> localDateField(String dateTimePattern,
                                                                Value<LocalDate> linkedValue) {
    return TemporalField.builder(LocalDate.class, dateTimePattern, requireNonNull(linkedValue));
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a {@link LocalDateTime} based {@link TemporalField} builder
   */
  public static TemporalField.Builder<LocalDateTime> localDateTimeField(String dateTimePattern) {
    return TemporalField.builder(LocalDateTime.class, dateTimePattern);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a {@link LocalDateTime} based {@link TemporalField} builder
   */
  public static TemporalField.Builder<LocalDateTime> localDateTimeField(String dateTimePattern,
                                                                        Value<LocalDateTime> linkedValue) {
    return TemporalField.builder(LocalDateTime.class, dateTimePattern, requireNonNull(linkedValue));
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a {@link OffsetDateTime} based {@link TemporalField} builder
   */
  public static TemporalField.Builder<OffsetDateTime> offsetDateTimeField(String dateTimePattern) {
    return TemporalField.builder(OffsetDateTime.class, dateTimePattern);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a {@link OffsetDateTime} based {@link TemporalField} builder
   */
  public static TemporalField.Builder<OffsetDateTime> offsetDateTimeField(String dateTimePattern,
                                                                          Value<OffsetDateTime> linkedValue) {
    return TemporalField.builder(OffsetDateTime.class, dateTimePattern, requireNonNull(linkedValue));
  }

  /**
   * @param <T> the temporal type
   * @param temporalClass the temporal class
   * @param dateTimePattern the date time pattern
   * @return a {@link TemporalField} builder
   */
  public static <T extends Temporal> TemporalField.Builder<T> temporalField(Class<T> temporalClass,
                                                                            String dateTimePattern) {
    return TemporalField.builder(temporalClass, dateTimePattern);
  }

  /**
   * @param <T> the temporal type
   * @param temporalClass the temporal class
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a {@link TemporalField} builder
   */
  public static <T extends Temporal> TemporalField.Builder<T> temporalField(Class<T> temporalClass,
                                                                            String dateTimePattern,
                                                                            Value<T> linkedValue) {
    return TemporalField.builder(temporalClass, dateTimePattern, requireNonNull(linkedValue));
  }

  /**
   * @return a Short based {@link NumberField} builder
   */
  public static NumberField.Builder<Short> shortField() {
    return NumberField.builder(Short.class);
  }

  /**
   * @return a Integer based {@link NumberField} builder
   */
  public static NumberField.Builder<Integer> integerField() {
    return NumberField.builder(Integer.class);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a Integer based {@link NumberField} builder
   */
  public static NumberField.Builder<Integer> integerField(Value<Integer> linkedValue) {
    return NumberField.builder(Integer.class, linkedValue);
  }

  /**
   * @return a Long based {@link NumberField} builder
   */
  public static NumberField.Builder<Long> longField() {
    return NumberField.builder(Long.class);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a Long based {@link NumberField} builder
   */
  public static NumberField.Builder<Long> longField(Value<Long> linkedValue) {
    return NumberField.builder(Long.class, linkedValue);
  }

  /**
   * @return a Double based {@link NumberField} builder
   */
  public static NumberField.Builder<Double> doubleField() {
    return NumberField.builder(Double.class);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a Double based {@link NumberField} builder
   */
  public static NumberField.Builder<Double> doubleField(Value<Double> linkedValue) {
    return NumberField.builder(Double.class, linkedValue);
  }

  /**
   * @return a BigDecimal based {@link NumberField} builder
   */
  public static NumberField.Builder<BigDecimal> bigDecimalField() {
    return NumberField.builder(BigDecimal.class);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a BigDecimal based {@link NumberField} builder
   */
  public static NumberField.Builder<BigDecimal> bigDecimalField(Value<BigDecimal> linkedValue) {
    return NumberField.builder(BigDecimal.class, linkedValue);
  }

  /**
   * @return a JFormattedTextField builder
   */
  public static MaskedTextFieldBuilder maskedTextField() {
    return MaskedTextFieldBuilder.builder();
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a JFormattedTextField builder
   */
  public static MaskedTextFieldBuilder maskedTextField(Value<String> linkedValue) {
    return MaskedTextFieldBuilder.builder(linkedValue);
  }

  /**
   * @return a JPasswordField builder
   */
  public static PasswordFieldBuilder passwordField() {
    return PasswordFieldBuilder.builder();
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a JPasswordField builder
   */
  public static PasswordFieldBuilder passwordField(Value<String> linkedValue) {
    return PasswordFieldBuilder.builder(linkedValue);
  }

  /**
   * @return a Double based JSpinner builder
   */
  public static NumberSpinnerBuilder<Double> doubleSpinner() {
    return NumberSpinnerBuilder.builder(new SpinnerNumberModel(), Double.class);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a Double based JSpinner builder
   */
  public static NumberSpinnerBuilder<Double> doubleSpinner(Value<Double> linkedValue) {
    return NumberSpinnerBuilder.builder(new SpinnerNumberModel(), Double.class, linkedValue);
  }

  /**
   * @param spinnerNumberModel the spinner model
   * @return a Double based JSpinner builder
   */
  public static NumberSpinnerBuilder<Double> doubleSpinner(SpinnerNumberModel spinnerNumberModel) {
    return NumberSpinnerBuilder.builder(spinnerNumberModel, Double.class);
  }

  /**
   * @param spinnerNumberModel the spinner model
   * @param linkedValue the value to link to the component
   * @return a Double based JSpinner builder
   */
  public static NumberSpinnerBuilder<Double> doubleSpinner(SpinnerNumberModel spinnerNumberModel,
                                                           Value<Double> linkedValue) {
    return NumberSpinnerBuilder.builder(spinnerNumberModel, Double.class, linkedValue);
  }

  /**
   * @return a Integer based JSpinner builder
   */
  public static NumberSpinnerBuilder<Integer> integerSpinner() {
    return NumberSpinnerBuilder.builder(new SpinnerNumberModel(), Integer.class);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a Integer based JSpinner builder
   */
  public static NumberSpinnerBuilder<Integer> integerSpinner(Value<Integer> linkedValue) {
    return NumberSpinnerBuilder.builder(new SpinnerNumberModel(), Integer.class, linkedValue);
  }

  /**
   * @param spinnerNumberModel the spinner model
   * @return a Integer based JSpinner builder
   */
  public static NumberSpinnerBuilder<Integer> integerSpinner(SpinnerNumberModel spinnerNumberModel) {
    return NumberSpinnerBuilder.builder(spinnerNumberModel, Integer.class);
  }

  /**
   * @param spinnerNumberModel the spinner model
   * @param linkedValue the value to link to the component
   * @return a Integer based JSpinner builder
   */
  public static NumberSpinnerBuilder<Integer> integerSpinner(SpinnerNumberModel spinnerNumberModel,
                                                             Value<Integer> linkedValue) {
    return NumberSpinnerBuilder.builder(spinnerNumberModel, Integer.class, linkedValue);
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @return a JSpinner builder
   */
  public static <T> ListSpinnerBuilder<T> listSpinner(SpinnerListModel spinnerModel) {
    return ListSpinnerBuilder.builder(spinnerModel);
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @param linkedValue the value to link to the component
   * @return a JSpinner builder
   */
  public static <T> ListSpinnerBuilder<T> listSpinner(SpinnerListModel spinnerModel, Value<T> linkedValue) {
    return ListSpinnerBuilder.builder(spinnerModel, linkedValue);
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @return a JSpinner builder
   */
  public static <T> ItemSpinnerBuilder<T> itemSpinner(SpinnerListModel spinnerModel) {
    return ItemSpinnerBuilder.builder(spinnerModel);
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @param linkedValue the value to link to the component
   * @return a JSpinner builder
   */
  public static <T> ItemSpinnerBuilder<T> itemSpinner(SpinnerListModel spinnerModel, Value<T> linkedValue) {
    return ItemSpinnerBuilder.builder(spinnerModel, linkedValue);
  }

  /**
   * @param boundedRangeModel the slider model
   * @return a JSlider builder
   */
  public static SliderBuilder slider(BoundedRangeModel boundedRangeModel) {
    return SliderBuilder.builder(boundedRangeModel);
  }

  /**
   * @param boundedRangeModel the slider model
   * @param linkedValue the value to link to the component
   * @return a JSlider builder
   */
  public static SliderBuilder slider(BoundedRangeModel boundedRangeModel, Value<Integer> linkedValue) {
    return SliderBuilder.builder(boundedRangeModel, linkedValue);
  }

  /**
   * A multi selection JList builder.
   * @param <T> the value type
   * @param listModel the list model
   * @return a JList builder
   */
  public static <T> ListBuilder<T> list(ListModel<T> listModel) {
    return ListBuilder.builder(listModel, null);
  }

  /**
   * A multi selection JList builder.
   * @param <T> the value type
   * @param listModel the list model
   * @param linkedValueSet the value set to link to the component
   * @return a JList builder
   */
  public static <T> ListBuilder<T> list(ListModel<T> listModel, ValueSet<T> linkedValueSet) {
    return ListBuilder.builder(listModel, requireNonNull(linkedValueSet));
  }

  /**
   * @param <T> the type to display in the label (using value.toString() or "" for null).
   * @return a JLabel builder
   */
  public static <T> LabelBuilder<T> label() {
    return new DefaultLabelBuilder<>((String) null);
  }

  /**
   * @param <T> the type to display in the label (using value.toString() or "" for null).
   * @param linkedValueObserver the value observer to link to the label text
   * @return a JLabel builder
   */
  public static <T> LabelBuilder<T> label(ValueObserver<T> linkedValueObserver) {
    return new DefaultLabelBuilder<>(linkedValueObserver);
  }

  /**
   * @param <T> the type to display in the label (using value.toString() or "" for null).
   * @param icon the label icon
   * @return a JLabel builder
   */
  public static <T> LabelBuilder<T> label(Icon icon) {
    return new DefaultLabelBuilder<>(icon);
  }

  /**
   * @param text the label text
   * @return a JLabel builder
   */
  public static LabelBuilder<String> label(String text) {
    return new DefaultLabelBuilder<>(text);
  }

  /**
   * @return a JPanel builder
   */
  public static PanelBuilder panel() {
    return PanelBuilder.builder();
  }

  /**
   * @param layout the panel layout manager
   * @return a JPanel builder
   */
  public static PanelBuilder panel(LayoutManager layout) {
    return PanelBuilder.builder(layout);
  }

  /**
   * @param panel the panel to configure
   * @return a JPanel builder
   */
  public static PanelBuilder panel(JPanel panel) {
    return PanelBuilder.builder(panel);
  }

  /**
   * @return a JTabbedPane builder
   */
  public static TabbedPaneBuilder tabbedPane() {
    return new DefaultTabbedPaneBuilder();
  }

  /**
   * @return a JSplitPane builder
   */
  public static SplitPaneBuilder splitPane() {
    return new DefaultSplitPaneBuilder();
  }

  /**
   * @param view the view component
   * @return a JScrollPane builder
   */
  public static ScrollPaneBuilder scrollPane(JComponent view) {
    return new DefaultScrollPaneBuilder(view);
  }

  /**
   * @param boundedRangeModel the model
   * @return a JProgressBar builder
   */
  public static ProgressBarBuilder progressBar(BoundedRangeModel boundedRangeModel) {
    return new DefaultProgressBarBuilder(boundedRangeModel);
  }

  /**
   * @return a {@link FileInputPanel} builder
   */
  public static FileInputPanelBuilder fileInputPanel() {
    return fileInputPanel(textField()
            .editable(false)
            .focusable(false)
            .build());
  }

  /**
   * @param filePathField the file path field
   * @return a {@link FileInputPanel} builder
   */
  public static FileInputPanelBuilder fileInputPanel(JTextField filePathField) {
    return new DefaultFileInputPanelBuilder(filePathField);
  }

  /**
   * @return a {@link javax.swing.JToolBar} builder
   */
  public static ToolBarBuilder toolBar() {
    return new DefaultToolBarBuilder();
  }
}
