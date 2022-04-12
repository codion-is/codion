/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.component.textfield.TemporalField;

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
    return new DefaultButtonBuilder<>(null);
  }

  /**
   * @param <B> the builder type
   * @param action the button action
   * @return a builder for a JButton
   */
  public static <B extends ButtonBuilder<Void, JButton, B>> ButtonBuilder<Void, JButton, B> button(Action action) {
    return new DefaultButtonBuilder<>(requireNonNull(action));
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
  public static CheckBoxBuilder checkBox(Value<Boolean> linkedValue) {
    return new DefaultCheckBoxBuilder(requireNonNull(linkedValue));
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
  public static RadioButtonBuilder radioButton(Value<Boolean> linkedValue) {
    return new DefaultRadioButtonBuilder(requireNonNull(linkedValue));
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
  public static <B extends ButtonBuilder<Boolean, JToggleButton, B>> ButtonBuilder<Boolean, JToggleButton, B> toggleButton(Value<Boolean> linkedValue) {
    return new DefaultToggleButtonBuilder<>(requireNonNull(linkedValue));
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
  public static ItemComboBoxBuilder<Boolean> booleanComboBox(Value<Boolean> linkedValue) {
    return new DefaultItemComboBoxBuilder<>(ItemComboBoxModel.createBooleanModel(), requireNonNull(linkedValue));
  }

  /**
   * @param comboBoxModel the combo box model
   * @return a builder for a component
   */
  public static ItemComboBoxBuilder<Boolean> booleanComboBox(ItemComboBoxModel<Boolean> comboBoxModel) {
    return new DefaultItemComboBoxBuilder<>(comboBoxModel, null);
  }

  /**
   * @param comboBoxModel the combo box model
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static ItemComboBoxBuilder<Boolean> booleanComboBox(ItemComboBoxModel<Boolean> comboBoxModel,
                                                             Value<Boolean> linkedValue) {
    return new DefaultItemComboBoxBuilder<>(comboBoxModel, requireNonNull(linkedValue));
  }

  /**
   * @param comboBoxModel the combo box model
   * @param <T> the value type
   * @return a builder for a component
   */
  public static <T> ItemComboBoxBuilder<T> itemComboBox(ItemComboBoxModel<T> comboBoxModel) {
    return new DefaultItemComboBoxBuilder<>(comboBoxModel, null);
  }

  /**
   * @param comboBoxModel the combo box model
   * @param linkedValue the value to link to the component
   * @param <T> the value type
   * @return a builder for a component
   */
  public static <T> ItemComboBoxBuilder<T> itemComboBox(ItemComboBoxModel<T> comboBoxModel,
                                                        Value<T> linkedValue) {
    return new DefaultItemComboBoxBuilder<>(comboBoxModel, requireNonNull(linkedValue));
  }

  /**
   * @param values the values
   * @param <T> the value type
   * @return a builder for a component
   */
  public static <T> ItemComboBoxBuilder<T> itemComboBox(List<Item<T>> values) {
    return new DefaultItemComboBoxBuilder<>(values, null);
  }

  /**
   * @param values the values
   * @param linkedValue the value to link to the component
   * @param <T> the value type
   * @return a builder for a component
   */
  public static <T> ItemComboBoxBuilder<T> itemComboBox(List<Item<T>> values, Value<T> linkedValue) {
    return new DefaultItemComboBoxBuilder<>(values, requireNonNull(linkedValue));
  }

  /**
   * @param <T> the value type
   * @param <C> the component type
   * @param <B> the builder type
   * @param comboBoxModel the combo box model
   * @return a builder for a component
   */
  public static <T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> comboBox(ComboBoxModel<T> comboBoxModel) {
    return new DefaultComboBoxBuilder<>(comboBoxModel, null);
  }

  /**
   * @param comboBoxModel the combo box model
   * @param linkedValue the value to link to the component
   * @param <T> the value type
   * @param <C> the component type
   * @param <B> the builder type
   * @return a builder for a component
   */
  public static <T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> comboBox(ComboBoxModel<T> comboBoxModel,
                                                                                                                  Value<T> linkedValue) {
    return new DefaultComboBoxBuilder<>(comboBoxModel, requireNonNull(linkedValue));
  }

  /**
   * @param <T> the value type
   * @param valueClass the value class
   * @param dateTimePattern the date time pattern
   * @return a builder for a component
   */
  public static <T extends Temporal> TemporalInputPanelBuilder<T> temporalInputPanel(Class<T> valueClass,
                                                                                     String dateTimePattern) {
    return new DefaultTemporalInputPanelBuiler<>(valueClass, dateTimePattern, null);
  }

  /**
   * @param <T> the value type
   * @param valueClass the value class
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static <T extends Temporal> TemporalInputPanelBuilder<T> temporalInputPanel(Class<T> valueClass,
                                                                                     String dateTimePattern,
                                                                                     Value<T> linkedValue) {
    return new DefaultTemporalInputPanelBuiler<>(valueClass, dateTimePattern, requireNonNull(linkedValue));
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalInputPanelBuilder<LocalTime> localTimeInputPanel(String dateTimePattern) {
    return new DefaultTemporalInputPanelBuiler<>(LocalTime.class, dateTimePattern, null);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a temporal component
   */
  public static TemporalInputPanelBuilder<LocalTime> localTimeInputPanel(String dateTimePattern,
                                                                         Value<LocalTime> linkedValue) {
    return new DefaultTemporalInputPanelBuiler<>(LocalTime.class, dateTimePattern, requireNonNull(linkedValue));
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalInputPanelBuilder<LocalDate> localDateInputPanel(String dateTimePattern) {
    return new DefaultTemporalInputPanelBuiler<>(LocalDate.class, dateTimePattern, null);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a temporal component
   */
  public static TemporalInputPanelBuilder<LocalDate> localDateInputPanel(String dateTimePattern,
                                                                         Value<LocalDate> linkedValue) {
    return new DefaultTemporalInputPanelBuiler<>(LocalDate.class, dateTimePattern, linkedValue);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalInputPanelBuilder<LocalDateTime> localDateTimeInputPanel(String dateTimePattern) {
    return new DefaultTemporalInputPanelBuiler<>(LocalDateTime.class, dateTimePattern, null);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a temporal component
   */
  public static TemporalInputPanelBuilder<LocalDateTime> localDateTimeInputPanel(String dateTimePattern,
                                                                                 Value<LocalDateTime> linkedValue) {
    return new DefaultTemporalInputPanelBuiler<>(LocalDateTime.class, dateTimePattern, requireNonNull(linkedValue));
  }

  /**
   * @return a builder for a component
   */
  public static TextInputPanelBuilder textInputPanel() {
    return new DefaultTextInputPanelBuilder(null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static TextInputPanelBuilder textInputPanel(Value<String> linkedValue) {
    return new DefaultTextInputPanelBuilder(requireNonNull(linkedValue));
  }

  /**
   * @return a builder for a component
   */
  public static TextAreaBuilder textArea() {
    return new DefaultTextAreaBuilder(null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static TextAreaBuilder textArea(Value<String> linkedValue) {
    return new DefaultTextAreaBuilder(requireNonNull(linkedValue));
  }

  /**
   * @param <B> the builder type
   * @return a builder for a component
   */
  public static <B extends TextFieldBuilder<String, JTextField, B>> TextFieldBuilder<String, JTextField, B> textField() {
    return new DefaultTextFieldBuilder<>(String.class, null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @param <B> the builder type
   * @return a builder for a component
   */
  public static <B extends TextFieldBuilder<String, JTextField, B>> TextFieldBuilder<String, JTextField, B> textField(Value<String> linkedValue) {
    return new DefaultTextFieldBuilder<>(String.class, requireNonNull(linkedValue));
  }

  /**
   * @param <T> the value type
   * @param <C> the text field type
   * @param <B> the builder type
   * @param valueClass the value class
   * @return a builder for a component
   */
  public static <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> TextFieldBuilder<T, C, B> textField(Class<T> valueClass) {
    requireNonNull(valueClass);
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

    return new DefaultTextFieldBuilder<>(valueClass, null);
  }

  /**
   * @param <T> the value type
   * @param <C> the text field type
   * @param <B> the builder type
   * @param valueClass the value class
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> TextFieldBuilder<T, C, B> textField(Class<T> valueClass,
                                                                                                                   Value<T> linkedValue) {
    requireNonNull(valueClass);
    requireNonNull(linkedValue);
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

    return new DefaultTextFieldBuilder<>(valueClass, linkedValue);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalFieldBuilder<LocalTime, TemporalField<LocalTime>> localTimeField(String dateTimePattern) {
    return new DefaultTemporalFieldBuilder<>(LocalTime.class, dateTimePattern, null);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a temporal component
   */
  public static TemporalFieldBuilder<LocalTime, TemporalField<LocalTime>> localTimeField(String dateTimePattern,
                                                                                         Value<LocalTime> linkedValue) {
    return new DefaultTemporalFieldBuilder<>(LocalTime.class, dateTimePattern, requireNonNull(linkedValue));
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalFieldBuilder<LocalDate, TemporalField<LocalDate>> localDateField(String dateTimePattern) {
    return new DefaultTemporalFieldBuilder<>(LocalDate.class, dateTimePattern, null);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a temporal component
   */
  public static TemporalFieldBuilder<LocalDate, TemporalField<LocalDate>> localDateField(String dateTimePattern,
                                                                                         Value<LocalDate> linkedValue) {
    return new DefaultTemporalFieldBuilder<>(LocalDate.class, dateTimePattern, requireNonNull(linkedValue));
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalFieldBuilder<LocalDateTime, TemporalField<LocalDateTime>> localDateTimeField(String dateTimePattern) {
    return new DefaultTemporalFieldBuilder<>(LocalDateTime.class, dateTimePattern, null);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a temporal component
   */
  public static TemporalFieldBuilder<LocalDateTime, TemporalField<LocalDateTime>> localDateTimeField(String dateTimePattern,
                                                                                                     Value<LocalDateTime> linkedValue) {
    return new DefaultTemporalFieldBuilder<>(LocalDateTime.class, dateTimePattern, requireNonNull(linkedValue));
  }

  /**
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static TemporalFieldBuilder<OffsetDateTime, TemporalField<OffsetDateTime>> offsetDateTimeField(String dateTimePattern) {
    return new DefaultTemporalFieldBuilder<>(OffsetDateTime.class, dateTimePattern, null);
  }

  /**
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a temporal component
   */
  public static TemporalFieldBuilder<OffsetDateTime, TemporalField<OffsetDateTime>> offsetDateTimeField(String dateTimePattern,
                                                                                                        Value<OffsetDateTime> linkedValue) {
    return new DefaultTemporalFieldBuilder<>(OffsetDateTime.class, dateTimePattern, requireNonNull(linkedValue));
  }

  /**
   * @param <T> the temporal type
   * @param temporalClass the temporal class
   * @param dateTimePattern the date time pattern
   * @return a builder for a temporal component
   */
  public static <T extends Temporal> TemporalFieldBuilder<T, TemporalField<T>> temporalField(Class<T> temporalClass,
                                                                                             String dateTimePattern) {
    return new DefaultTemporalFieldBuilder<>(temporalClass, dateTimePattern, null);
  }

  /**
   * @param <T> the temporal type
   * @param temporalClass the temporal class
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @return a builder for a temporal component
   */
  public static <T extends Temporal> TemporalFieldBuilder<T, TemporalField<T>> temporalField(Class<T> temporalClass,
                                                                                             String dateTimePattern,
                                                                                             Value<T> linkedValue) {
    return new DefaultTemporalFieldBuilder<>(temporalClass, dateTimePattern, requireNonNull(linkedValue));
  }

  /**
   * @return a builder for a component
   */
  public static IntegerFieldBuilder integerField() {
    return new DefaultIntegerFieldBuilder(null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static IntegerFieldBuilder integerField(Value<Integer> linkedValue) {
    return new DefaultIntegerFieldBuilder(requireNonNull(linkedValue));
  }

  /**
   * @return a builder for a component
   */
  public static LongFieldBuilder longField() {
    return new DefaultLongFieldBuilder(null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static LongFieldBuilder longField(Value<Long> linkedValue) {
    return new DefaultLongFieldBuilder(requireNonNull(linkedValue));
  }

  /**
   * @return a builder for a component
   */
  public static DoubleFieldBuilder doubleField() {
    return new DefaultDoubleFieldBuilder(null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static DoubleFieldBuilder doubleField(Value<Double> linkedValue) {
    return new DefaultDoubleFieldBuilder(requireNonNull(linkedValue));
  }

  /**
   * @return a builder for a component
   */
  public static BigDecimalFieldBuilder bigDecimalField() {
    return new DefaultBigDecimalFieldBuilder(null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static BigDecimalFieldBuilder bigDecimalField(Value<BigDecimal> linkedValue) {
    return new DefaultBigDecimalFieldBuilder(requireNonNull(linkedValue));
  }

  /**
   * @return a builder for a component
   */
  public static FormattedTextFieldBuilder formattedTextField() {
    return new DefaultFormattedTextFieldBuilder(null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static FormattedTextFieldBuilder formattedTextField(Value<String> linkedValue) {
    return new DefaultFormattedTextFieldBuilder(requireNonNull(linkedValue));
  }

  /**
   * @return a builder for a Double based JSpinner
   */
  public static NumberSpinnerBuilder<Double> doubleSpinner() {
    return new DefaultNumberSpinnerBuilder<>(new SpinnerNumberModel(), Double.class, null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a Double based JSpinner
   */
  public static NumberSpinnerBuilder<Double> doubleSpinner(Value<Double> linkedValue) {
    return new DefaultNumberSpinnerBuilder<>(new SpinnerNumberModel(), Double.class, requireNonNull(linkedValue));
  }

  /**
   * @param spinnerNumberModel the spinner model
   * @return a builder for a Double based JSpinner
   */
  public static NumberSpinnerBuilder<Double> doubleSpinner(SpinnerNumberModel spinnerNumberModel) {
    return new DefaultNumberSpinnerBuilder<>(spinnerNumberModel, Double.class, null);
  }

  /**
   * @param spinnerNumberModel the spinner model
   * @param linkedValue the value to link to the component
   * @return a builder for a Double based JSpinner
   */
  public static NumberSpinnerBuilder<Double> doubleSpinner(SpinnerNumberModel spinnerNumberModel,
                                                           Value<Double> linkedValue) {
    return new DefaultNumberSpinnerBuilder<>(spinnerNumberModel, Double.class, requireNonNull(linkedValue));
  }

  /**
   * @return a builder for a Integer based JSpinner
   */
  public static NumberSpinnerBuilder<Integer> integerSpinner() {
    return new DefaultNumberSpinnerBuilder<>(new SpinnerNumberModel(), Integer.class, null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a Integer based JSpinner
   */
  public static NumberSpinnerBuilder<Integer> integerSpinner(Value<Integer> linkedValue) {
    return new DefaultNumberSpinnerBuilder<>(new SpinnerNumberModel(), Integer.class, requireNonNull(linkedValue));
  }

  /**
   * @param spinnerNumberModel the spinner model
   * @return a builder for a Integer based JSpinner
   */
  public static NumberSpinnerBuilder<Integer> integerSpinner(SpinnerNumberModel spinnerNumberModel) {
    return new DefaultNumberSpinnerBuilder<>(spinnerNumberModel, Integer.class, null);
  }

  /**
   * @param spinnerNumberModel the spinner model
   * @param linkedValue the value to link to the component
   * @return a builder for a Integer based JSpinner
   */
  public static NumberSpinnerBuilder<Integer> integerSpinner(SpinnerNumberModel spinnerNumberModel,
                                                             Value<Integer> linkedValue) {
    return new DefaultNumberSpinnerBuilder<>(spinnerNumberModel, Integer.class, requireNonNull(linkedValue));
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @return a builder for a JSpinner
   */
  public static <T> ListSpinnerBuilder<T> listSpinner(SpinnerListModel spinnerModel) {
    return new DefaultListSpinnerBuilder<>(spinnerModel, null);
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @param linkedValue the value to link to the component
   * @return a builder for a JSpinner
   */
  public static <T> ListSpinnerBuilder<T> listSpinner(SpinnerListModel spinnerModel, Value<T> linkedValue) {
    return new DefaultListSpinnerBuilder<>(spinnerModel, requireNonNull(linkedValue));
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @return a builder for a JSpinner
   */
  public static <T> ItemSpinnerBuilder<T> itemSpinner(SpinnerListModel spinnerModel) {
    return new DefaultItemSpinnerBuilder<>(spinnerModel, null);
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @param linkedValue the value to link to the component
   * @return a builder for a JSpinner
   */
  public static <T> ItemSpinnerBuilder<T> itemSpinner(SpinnerListModel spinnerModel, Value<T> linkedValue) {
    return new DefaultItemSpinnerBuilder<>(spinnerModel, requireNonNull(linkedValue));
  }

  /**
   * @param boundedRangeModel the slider model
   * @return a builder for a component
   */
  public static SliderBuilder slider(BoundedRangeModel boundedRangeModel) {
    return new DefaultSliderBuilder(boundedRangeModel, null);
  }

  /**
   * @param boundedRangeModel the slider model
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  public static SliderBuilder slider(BoundedRangeModel boundedRangeModel, Value<Integer> linkedValue) {
    return new DefaultSliderBuilder(boundedRangeModel, requireNonNull(linkedValue));
  }

  /**
   * A single selection JList builder.
   * @param <T> the value type
   * @param listModel the list model
   * @return a builder for a JList
   */
  public static <T> ListBuilder<T> list(ListModel<T> listModel) {
    return new DefaultListBuilder<>(listModel, null);
  }

  /**
   * A single selection JList builder.
   * @param <T> the value type
   * @param listModel the list model
   * @param linkedValue the value to link to the component
   * @return a builder for a JList
   */
  public static <T> ListBuilder<T> list(ListModel<T> listModel, Value<T> linkedValue) {
    return new DefaultListBuilder<>(listModel, requireNonNull(linkedValue));
  }

  /**
   * @param <T> the type to display in the label (using value.toString() or "" for null).
   * @return a label builder
   */
  public static <T> LabelBuilder<T> label() {
    return new DefaultLabelBuilder<>((String) null);
  }

  /**
   * @param <T> the type to display in the label (using value.toString() or "" for null).
   * @param linkedValueObserver the value observer to link to the label text
   * @return a label builder
   */
  public static <T> LabelBuilder<T> label(ValueObserver<T> linkedValueObserver) {
    return new DefaultLabelBuilder<>(linkedValueObserver);
  }

  /**
   * @param <T> the type to display in the label (using value.toString() or "" for null).
   * @param icon the label icon
   * @return a label builder
   */
  public static <T> LabelBuilder<T> label(Icon icon) {
    return new DefaultLabelBuilder<>(icon);
  }

  /**
   * @param text the label text
   * @return a label builder
   */
  public static LabelBuilder<String> label(String text) {
    return new DefaultLabelBuilder<>(text);
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
  public static PanelBuilder panel(LayoutManager layout) {
    return new DefaultPanelBuilder(requireNonNull(layout));
  }

  /**
   * @param panel the panel to configure
   * @return a panel builder
   */
  public static PanelBuilder panel(JPanel panel) {
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
   * @param view the view component
   * @return a scroll pane builder
   */
  public static ScrollPaneBuilder scrollPane(JComponent view) {
    return new DefaultScrollPaneBuilder(view);
  }

  /**
   * @param boundedRangeModel the model
   * @return a progress bar builder
   */
  public static ProgressBarBuilder progressBar(BoundedRangeModel boundedRangeModel) {
    return new DefaultProgressBarBuilder(boundedRangeModel);
  }

  /**
   * @return a FileInputPanel builder
   */
  public static FileInputPanelBuilder fileInputPanel() {
    return fileInputPanel(textField()
            .editable(false)
            .focusable(false)
            .build());
  }

  /**
   * @param filePathField the file path field
   * @return a FileInputPanel builder
   */
  public static FileInputPanelBuilder fileInputPanel(JTextField filePathField) {
    return new DefaultFileInputPanelBuilder(filePathField);
  }
}
