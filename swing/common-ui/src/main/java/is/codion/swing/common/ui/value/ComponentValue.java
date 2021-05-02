/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.TextInputPanel;
import is.codion.swing.common.ui.time.TemporalInputPanel;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.text.JTextComponent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.List;

import static is.codion.common.Util.nullOrEmpty;

/**
 * A {@link Value} represented by an input component of some sort.
 * A factory for {@link ComponentValue} implementations.
 * @param <V> the value type
 * @param <C> the component type
 */
public interface ComponentValue<V, C extends JComponent> extends Value<V> {

  /**
   * @return the input component representing the value
   */
  C getComponent();

  /**
   * @param textComponent the component
   * @param <C> the text component type
   * @return a Value bound to the given component
   */
  static <C extends JTextComponent> ComponentValue<String, C> stringTextComponent(final C textComponent) {
    return new AbstractTextComponentValue<String, C>(textComponent, null, UpdateOn.KEYSTROKE) {
      @Override
      protected String getComponentValue(final C component) {
        final String text = component.getText();

        return nullOrEmpty(text) ? null : text;
      }
      @Override
      protected void setComponentValue(final C component, final String value) {
        component.setText(value);
      }
    };
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @param <C> the text component type
   * @return a Value bound to the given component
   */
  static <C extends JTextComponent> ComponentValue<String, C> stringTextComponent(final C textComponent,
                                                                                  final Format format) {
    return stringTextComponent(textComponent, format, UpdateOn.KEYSTROKE);
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @param updateOn specifies when the underlying value should be updated
   * @param <C> the text component type
   * @return a Value bound to the given component
   */
  static <C extends JTextComponent> ComponentValue<String, C> stringTextComponent(final C textComponent,
                                                                                  final Format format,
                                                                                  final UpdateOn updateOn) {
    return new FormattedTextComponentValue<>(textComponent, format, updateOn);
  }

  /**
   * @param textField the component
   * @return a Value bound to the given component
   */
  static ComponentValue<Character, JTextField> characterTextField(final JTextField textField) {
    return characterTextField(textField, UpdateOn.KEYSTROKE);
  }

  /**
   * @param textField the component
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  static ComponentValue<Character, JTextField> characterTextField(final JTextField textField, final UpdateOn updateOn) {
    return new CharacterFieldValue(textField, updateOn);
  }

  /**
   * Instantiates a new String based ComponentValue.
   * @param inputDialogTitle the title to use for the lookup input dialog
   * @param initialValue the initial value
   * @param maximumLength the maximum input length, -1 for no limit
   * @return a String based ComponentValue
   */
  static ComponentValue<String, TextInputPanel> stringTextInputPanel(final String inputDialogTitle, final String initialValue,
                                                                     final int maximumLength) {
    return new TextInputPanelValue(inputDialogTitle, initialValue, maximumLength);
  }

  /**
   * Instantiates a new {@link ComponentValue} for {@link Temporal} values.
   * @param inputPanel the input panel to use
   * @param <V> the temporal value type
   * @return a Value bound to the given component
   */
  static <V extends Temporal> ComponentValue<V, TemporalInputPanel<V>> temporalValue(final TemporalInputPanel<V> inputPanel) {
    return new TemporalInputPanelValue<>(inputPanel);
  }

  /**
   * @return a LocalTime based TemporalFieldValueBuilder
   */
  static TemporalFieldValueBuilder<LocalTime> localTimeFieldBuilder() {
    return new LocalTimeFieldValueBuilder();
  }

  /**
   * @return a LocalDate based TemporalFieldValueBuilder
   */
  static TemporalFieldValueBuilder<LocalDate> localDateFieldBuilder() {
    return new LocalDateFieldValueBuilder();
  }

  /**
   * @return a LocalDateTime based TemporalFieldValueBuilder
   */
  static TemporalFieldValueBuilder<LocalDateTime> localDateFieldTimeBuilder() {
    return new LocalDateTimeFieldValueBuilder();
  }

  /**
   * @return a OffsetDateTime based TemporalFieldValueBuilder
   */
  static TemporalFieldValueBuilder<OffsetDateTime> offsetDateFieldTimeBuilder() {
    return new OffsetDateTimeFieldValueBuilder();
  }

  /**
   * Instantiates a Item based ComponentValue.
   * @param initialValue the initial value
   * @param values the available values
   * @param <V> the value type
   * @return a ComponentValue based on a combo box
   */
  static <V> ComponentValue<V, JComboBox<Item<V>>> selectedItemComboBox(final V initialValue, final List<Item<V>> values) {
    return new SelectedItemValue<>(initialValue, values);
  }

  /**
   * Instantiates a Item based ComponentValue.
   * @param <V> the value type
   * @param comboBox the combo box
   * @return a Value bound to the given component
   */
  static <V> ComponentValue<V, JComboBox<Item<V>>> selectedItemComboBox(final JComboBox<Item<V>> comboBox) {
    return new SelectedItemValue<>(comboBox);
  }

  /**
   * @param <V> the value type
   * @param comboBox the combo box
   * @return a Value bound to the given component
   */
  static <V> ComponentValue<V, JComboBox<V>> selectedComboBox(final JComboBox<V> comboBox) {
    return new SelectedValue<>(comboBox);
  }

  /**
   * @return a file based ComponentValue
   */
  static ComponentValue<byte[], FileInputPanelValue.FileInputPanel> fileInputPanel() {
    return new FileInputPanelValue();
  }

  /**
   * Creates a boolean value based on the given toggle button.
   * If the button is a {@link NullableCheckBox} the value will be nullable otherwise not
   * @param button the button
   * @param <T> the attribute type
   * @return a Value bound to the given button
   */
  static <T extends JToggleButton> ComponentValue<Boolean, T> booleanToggleButton(final JToggleButton button) {
    if (button instanceof NullableCheckBox) {
      return (ComponentValue<Boolean, T>) new BooleanNullableCheckBoxValue((NullableCheckBox) button);
    }

    return (ComponentValue<Boolean, T>) new BooleanToggleButtonValue(button);
  }

  /**
   * Instantiates a new Boolean based ComponentValue with a null initial value.
   * @return a Boolean based ComponentValue
   */
  static ComponentValue<Boolean, JComboBox<Item<Boolean>>> booleanComboBox() {
    return booleanComboBox((Boolean) null);
  }

  /**
   * Instantiates a new Boolean based ComponentValue.
   * @param initialValue the initial value
   * @return a Boolean based ComponentValue
   */
  static ComponentValue<Boolean, JComboBox<Item<Boolean>>> booleanComboBox(final Boolean initialValue) {
    final BooleanComboBoxModel model = new BooleanComboBoxModel();
    model.setSelectedItem(initialValue);

    return booleanComboBox(new JComboBox<>(model));
  }

  /**
   * Instantiates a new Boolean based ComponentValue.
   * @param comboBox the combo box
   * @return a Boolean based ComponentValue
   */
  static ComponentValue<Boolean, JComboBox<Item<Boolean>>> booleanComboBox(final JComboBox<Item<Boolean>> comboBox) {
    return new BooleanComboBoxValue(comboBox);
  }

  /**
   * @return a BigDecimal based ComponentValue
   */
  static ComponentValue<BigDecimal, BigDecimalField> bigDecimalField() {
    return bigDecimalField((BigDecimal) null);
  }

  /**
   * @param initialValue the initial value
   * @return a BigDecimal based ComponentValue
   */
  static ComponentValue<BigDecimal, BigDecimalField> bigDecimalField(final BigDecimal initialValue) {
    return bigDecimalFieldBuilder()
            .initalValue(initialValue)
            .build();
  }

  /**
   * @param bigDecimalField the component
   * @return a Value bound to the given component
   */
  static ComponentValue<BigDecimal, BigDecimalField> bigDecimalField(final BigDecimalField bigDecimalField) {
    return bigDecimalFieldBuilder()
            .component(bigDecimalField)
            .build();
  }

  /**
   * @return a BigDecimal based NumberFieldValueBuilder
   */
  static NumberFieldValueBuilder<BigDecimal, BigDecimalField, DecimalFormat> bigDecimalFieldBuilder() {
    return new BigDecimalFieldValueBuilder();
  }

  /**
   * @param spinner the spinner
   * @return a Value bound to the given component
   */
  static ComponentValue<Double, JSpinner> doubleSpinner(final JSpinner spinner) {
    return new SpinnerNumberValue<>(spinner);
  }

  /**
   * Instantiates a new Double based ComponentValue.
   * @return a Double based ComponentValue
   */
  static ComponentValue<Double, DoubleField> doubleField() {
    return doubleField((Double) null);
  }

  /**
   * Instantiates a new Double based ComponentValue.
   * @param initialValue the initial value
   * @return a Double based ComponentValue
   */
  static ComponentValue<Double, DoubleField> doubleField(final Double initialValue) {
    return doubleFieldBuilder()
            .initalValue(initialValue)
            .build();
  }

  /**
   * @param doubleField the component
   * @return a Value bound to the given component
   */
  static ComponentValue<Double, DoubleField> doubleField(final DoubleField doubleField) {
    return doubleFieldBuilder()
            .component(doubleField)
            .build();
  }

  /**
   * @return a Double based NumberFieldValueBuilder
   */
  static NumberFieldValueBuilder<Double, DoubleField, DecimalFormat> doubleFieldBuilder() {
    return new DoubleFieldValueBuilder();
  }

  /**
   * Instantiates a new Integer based ComponentValue.
   * @return a Integer based ComponentValue
   */
  static ComponentValue<Integer, IntegerField> integerField() {
    return integerField((Integer) null);
  }

  /**
   * Instantiates a new Integer based ComponentValue.
   * @param initialValue the initial value
   * @return a Integer based ComponentValue
   */
  static ComponentValue<Integer, IntegerField> integerField(final Integer initialValue) {
    return integerFieldBuilder()
            .initalValue(initialValue)
            .build();
  }

  /**
   * @param integerField the component
   * @return a Value bound to the given component
   */
  static ComponentValue<Integer, IntegerField> integerField(final IntegerField integerField) {
    return integerFieldBuilder()
            .component(integerField)
            .build();
  }

  /**
   * @return a Integer based NumberFieldValueBuilder
   */
  static NumberFieldValueBuilder<Integer, IntegerField, NumberFormat> integerFieldBuilder() {
    return new IntegerFieldValueBuilder();
  }

  /**
   * @param spinner the spinner
   * @return a Value bound to the given spinner
   */
  static ComponentValue<Integer, JSpinner> integerSpinner(final JSpinner spinner) {
    return new SpinnerNumberValue<>(spinner);
  }

  /**
   * @param progressBar the progress bar
   * @return a Value bound to the given progress bar
   */
  static ComponentValue<Integer, JProgressBar> integerProgressBar(final JProgressBar progressBar) {
    return new IntegerProgressBarValue(progressBar);
  }

  /**
   * Instantiates a new Long based ComponentValue.
   * @return a Long based ComponentValue
   */
  static ComponentValue<Long, LongField> longField() {
    return longField((Long) null);
  }

  /**
   * Instantiates a new Long based ComponentValue.
   * @param initialValue the initial value
   * @return a Long based ComponentValue
   */
  static ComponentValue<Long, LongField> longField(final Long initialValue) {
    return longFieldBuilder()
            .initalValue(initialValue)
            .build();
  }

  /**
   * @param longField the component
   * @return a Value bound to the given component
   */
  static ComponentValue<Long, LongField> longField(final LongField longField) {
    return longFieldBuilder()
            .component(longField)
            .build();
  }

  /**
   * @return a Long based NumberFieldValueBuilder
   */
  static NumberFieldValueBuilder<Long, LongField, NumberFormat> longFieldBuilder() {
    return new LongFieldValueBuilder();
  }
}
