/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.item.Item;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.TextInputPanel;
import is.codion.swing.common.ui.time.TemporalField;
import is.codion.swing.common.ui.time.TemporalInputPanel;

import javax.swing.JComboBox;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.text.JTextComponent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.time.temporal.Temporal;
import java.util.List;

/**
 * A factory for {@link ComponentValue}.
 */
public final class ComponentValues {

  private ComponentValues() {}

  /**
   * @return a Value bound to the given component
   */
  public static ComponentValue<String, JTextField> textField() {
    return textComponent(new JTextField(), null, UpdateOn.KEYSTROKE);
  }

  /**
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<String, JTextField> textField(final UpdateOn updateOn) {
    return textComponent(new JTextField(), null, updateOn);
  }

  /**
   * @param textComponent the component
   * @param <C> the text component type
   * @return a Value bound to the given component
   */
  public static <C extends JTextComponent> ComponentValue<String, C> textComponent(final C textComponent) {
    return textComponent(textComponent, null, UpdateOn.KEYSTROKE);
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @param <C> the text component type
   * @return a Value bound to the given component
   */
  public static <C extends JTextComponent> ComponentValue<String, C> textComponent(final C textComponent,
                                                                                   final Format format) {
    return textComponent(textComponent, format, UpdateOn.KEYSTROKE);
  }

  /**
   * @param textComponent the component
   * @param updateOn specifies when the underlying value should be updated
   * @param <C> the text component type
   * @return a Value bound to the given component
   */
  public static <C extends JTextComponent> ComponentValue<String, C> textComponent(final C textComponent,
                                                                                   final UpdateOn updateOn) {
    return textComponent(textComponent, null, updateOn);
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @param updateOn specifies when the underlying value should be updated
   * @param <C> the text component type
   * @return a Value bound to the given component
   */
  public static <C extends JTextComponent> ComponentValue<String, C> textComponent(final C textComponent,
                                                                                   final Format format,
                                                                                   final UpdateOn updateOn) {
    return new FormattedTextComponentValue<>(textComponent, format, updateOn);
  }

  /**
   * @param textField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Character, JTextField> characterTextField(final JTextField textField) {
    return characterTextField(textField, UpdateOn.KEYSTROKE);
  }

  /**
   * @param textField the component
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<Character, JTextField> characterTextField(final JTextField textField, final UpdateOn updateOn) {
    return new CharacterFieldValue(textField, updateOn);
  }

  /**
   * Instantiates a new String based ComponentValue.
   * @param inputDialogTitle the title to use for the lookup input dialog
   * @param initialValue the initial value
   * @param maximumLength the maximum input length, -1 for no limit
   * @return a String based ComponentValue
   */
  public static ComponentValue<String, TextInputPanel> textInputPanel(final String inputDialogTitle,
                                                                      final String initialValue,
                                                                      final int maximumLength) {
    return new TextInputPanelValue(inputDialogTitle, initialValue, maximumLength);
  }

  /**
   * Instantiates a new {@link ComponentValue} for {@link Temporal} values.
   * @param inputPanel the input panel to use
   * @param <V> the temporal value type
   * @return a Value bound to the given component
   */
  public static <V extends Temporal> ComponentValue<V, TemporalInputPanel<V>> temporalInputPanel(final TemporalInputPanel<V> inputPanel) {
    return new TemporalInputPanelValue<>(inputPanel);
  }

  /**
   * Instantiates a new {@link ComponentValue} for {@link Temporal} values.
   * @param temporalField the temporal field to use
   * @param <V> the temporal value type
   * @return a Value bound to the given component
   */
  public static <V extends Temporal> ComponentValue<V, TemporalField<V>> temporalField(final TemporalField<V> temporalField) {
    return temporalField(temporalField, UpdateOn.KEYSTROKE);
  }

  /**
   * Instantiates a new {@link ComponentValue} for {@link Temporal} values.
   * @param temporalField the temporal field to use
   * @param updateOn specifies when the underlying value should be updated
   * @param <V> the temporal value type
   * @return a Value bound to the given component
   */
  public static <V extends Temporal> ComponentValue<V, TemporalField<V>> temporalField(final TemporalField<V> temporalField,
                                                                                       final UpdateOn updateOn) {
    return new TemporalFieldValue<>(temporalField, updateOn);
  }

  /**
   * Instantiates a Item based ComponentValue.
   * @param <V> the value type
   * @param values the available values
   * @param initialValue the initial value
   * @return a ComponentValue based on a combo box
   */
  public static <V> ComponentValue<V, JComboBox<Item<V>>> itemComboBox(final List<Item<V>> values, final V initialValue) {
    return new SelectedItemValue<>(initialValue, values);
  }

  /**
   * Instantiates a Item based ComponentValue.
   * @param <V> the value type
   * @param comboBox the combo box
   * @return a Value bound to the given component
   */
  public static <V> ComponentValue<V, JComboBox<Item<V>>> itemComboBox(final JComboBox<Item<V>> comboBox) {
    return new SelectedItemValue<>(comboBox);
  }

  /**
   * @param <V> the value type
   * @param comboBox the combo box
   * @return a Value bound to the given component
   */
  public static <V> ComponentValue<V, JComboBox<V>> comboBox(final JComboBox<V> comboBox) {
    return new SelectedValue<>(comboBox);
  }

  /**
   * @return a file based ComponentValue
   */
  public static ComponentValue<byte[], FileInputPanelValue.FileInputPanel> fileInputPanel() {
    return new FileInputPanelValue();
  }

  /**
   * Creates a boolean value based on the given toggle button.
   * If the button is a {@link NullableCheckBox} the value will be nullable otherwise not
   * @param button the button
   * @param <T> the attribute type
   * @return a Value bound to the given button
   */
  public static <T extends JToggleButton> ComponentValue<Boolean, T> toggleButton(final JToggleButton button) {
    if (button instanceof NullableCheckBox) {
      return (ComponentValue<Boolean, T>) new BooleanNullableCheckBoxValue((NullableCheckBox) button);
    }

    return (ComponentValue<Boolean, T>) new BooleanToggleButtonValue(button);
  }

  /**
   * Instantiates a new Boolean based ComponentValue with a null initial value.
   * @return a Boolean based ComponentValue
   */
  public static ComponentValue<Boolean, JComboBox<Item<Boolean>>> booleanComboBox() {
    return booleanComboBox((Boolean) null);
  }

  /**
   * Instantiates a new Boolean based ComponentValue.
   * @param initialValue the initial value
   * @return a Boolean based ComponentValue
   */
  public static ComponentValue<Boolean, JComboBox<Item<Boolean>>> booleanComboBox(final Boolean initialValue) {
    final BooleanComboBoxModel model = new BooleanComboBoxModel();
    model.setSelectedItem(initialValue);

    return booleanComboBox(new JComboBox<>(model));
  }

  /**
   * Instantiates a new Boolean based ComponentValue.
   * @param comboBox the combo box
   * @return a Boolean based ComponentValue
   */
  public static ComponentValue<Boolean, JComboBox<Item<Boolean>>> booleanComboBox(final JComboBox<Item<Boolean>> comboBox) {
    return new BooleanComboBoxValue(comboBox);
  }

  /**
   * @return a BigDecimal based ComponentValue
   */
  public static ComponentValue<BigDecimal, BigDecimalField> bigDecimalField() {
    return bigDecimalField((BigDecimal) null);
  }

  /**
   * @param initialValue the initial value
   * @return a BigDecimal based ComponentValue
   */
  public static ComponentValue<BigDecimal, BigDecimalField> bigDecimalField(final BigDecimal initialValue) {
    return bigDecimalFieldBuilder()
            .initalValue(initialValue)
            .build();
  }

  /**
   * @param bigDecimalField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<BigDecimal, BigDecimalField> bigDecimalField(final BigDecimalField bigDecimalField) {
    return bigDecimalFieldBuilder()
            .component(bigDecimalField)
            .build();
  }

  /**
   * @return a BigDecimal based NumberFieldValueBuilder
   */
  public static NumberFieldValueBuilder<BigDecimal, BigDecimalField, DecimalFormat> bigDecimalFieldBuilder() {
    return new BigDecimalFieldValueBuilder();
  }

  /**
   * @param spinner the spinner
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, JSpinner> doubleSpinner(final JSpinner spinner) {
    return new SpinnerNumberValue<>(spinner);
  }

  /**
   * Instantiates a new Double based ComponentValue.
   * @return a Double based ComponentValue
   */
  public static ComponentValue<Double, DoubleField> doubleField() {
    return doubleField((Double) null);
  }

  /**
   * Instantiates a new Double based ComponentValue.
   * @param initialValue the initial value
   * @return a Double based ComponentValue
   */
  public static ComponentValue<Double, DoubleField> doubleField(final Double initialValue) {
    return doubleFieldBuilder()
            .initalValue(initialValue)
            .build();
  }

  /**
   * @param doubleField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, DoubleField> doubleField(final DoubleField doubleField) {
    return doubleFieldBuilder()
            .component(doubleField)
            .build();
  }

  /**
   * @return a Double based NumberFieldValueBuilder
   */
  public static NumberFieldValueBuilder<Double, DoubleField, DecimalFormat> doubleFieldBuilder() {
    return new DoubleFieldValueBuilder();
  }

  /**
   * Instantiates a new Integer based ComponentValue.
   * @return a Integer based ComponentValue
   */
  public static ComponentValue<Integer, IntegerField> integerField() {
    return integerField((Integer) null);
  }

  /**
   * Instantiates a new Integer based ComponentValue.
   * @param initialValue the initial value
   * @return a Integer based ComponentValue
   */
  public static ComponentValue<Integer, IntegerField> integerField(final Integer initialValue) {
    return integerFieldBuilder()
            .initalValue(initialValue)
            .build();
  }

  /**
   * @param integerField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Integer, IntegerField> integerField(final IntegerField integerField) {
    return integerFieldBuilder()
            .component(integerField)
            .build();
  }

  /**
   * @return a Integer based NumberFieldValueBuilder
   */
  public static NumberFieldValueBuilder<Integer, IntegerField, NumberFormat> integerFieldBuilder() {
    return new IntegerFieldValueBuilder();
  }

  /**
   * @param spinner the spinner
   * @return a Value bound to the given spinner
   */
  public static ComponentValue<Integer, JSpinner> integerSpinner(final JSpinner spinner) {
    return new SpinnerNumberValue<>(spinner);
  }

  /**
   * @param progressBar the progress bar
   * @return a Value bound to the given progress bar
   */
  public static ComponentValue<Integer, JProgressBar> progressBar(final JProgressBar progressBar) {
    return new IntegerProgressBarValue(progressBar);
  }

  /**
   * @param slider the slider
   * @return a Value bound to the given slider
   */
  public static ComponentValue<Integer, JSlider> slider(final JSlider slider) {
    return new IntegerSliderValue(slider);
  }

  /**
   * Instantiates a new Long based ComponentValue.
   * @return a Long based ComponentValue
   */
  public static ComponentValue<Long, LongField> longField() {
    return longField((Long) null);
  }

  /**
   * Instantiates a new Long based ComponentValue.
   * @param initialValue the initial value
   * @return a Long based ComponentValue
   */
  public static ComponentValue<Long, LongField> longField(final Long initialValue) {
    return longFieldBuilder()
            .initalValue(initialValue)
            .build();
  }

  /**
   * @param longField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Long, LongField> longField(final LongField longField) {
    return longFieldBuilder()
            .component(longField)
            .build();
  }

  /**
   * @return a Long based NumberFieldValueBuilder
   */
  public static NumberFieldValueBuilder<Long, LongField, NumberFormat> longFieldBuilder() {
    return new LongFieldValueBuilder();
  }
}
