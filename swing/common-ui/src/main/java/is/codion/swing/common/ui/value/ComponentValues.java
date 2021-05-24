/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.item.Item;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.textfield.TextInputPanel;
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

import static java.util.Objects.requireNonNull;

/**
 * A factory for {@link ComponentValue}.
 */
public final class ComponentValues {

  private ComponentValues() {}

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
   * @param textInputPanel the text input panel to base this component value on
   * @return a String based ComponentValue
   */
  public static ComponentValue<String, TextInputPanel> textInputPanel(final TextInputPanel textInputPanel) {
    return new TextInputPanelValue(textInputPanel);
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
   * @param <C> the combo box type
   * @param comboBox the combo box
   * @return a Value bound to the given component
   */
  public static <V, C extends JComboBox<Item<V>>> ComponentValue<V, C> itemComboBox(final C comboBox) {
    return new SelectedItemValue<>(comboBox);
  }

  /**
   * @param <V> the value type
   * @param <C> the combo box type
   * @param comboBox the combo box
   * @return a Value bound to the given component
   */
  public static <V, C extends JComboBox<V>> ComponentValue<V, C> comboBox(final C comboBox) {
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
   * @param toggleButton the toggle button
   * @param <T> the attribute type
   * @return a Value bound to the given button
   */
  public static <T extends JToggleButton> ComponentValue<Boolean, T> toggleButton(final JToggleButton toggleButton) {
    if (toggleButton instanceof NullableCheckBox) {
      return (ComponentValue<Boolean, T>) new BooleanNullableCheckBoxValue((NullableCheckBox) toggleButton);
    }

    return (ComponentValue<Boolean, T>) new BooleanToggleButtonValue(toggleButton);
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

  /**
   * Links the given text field with the given value.
   * @param textField the text field
   * @param valueClass the value class
   * @param updateOn the update on
   * @param format the format, if any
   * @param <C> the component type
   * @param <T> the value type
   * @return the text field
   * @throws IllegalArgumentException in case the value class is not supported
   */
  public static <C extends JTextField, T> ComponentValue<T, C> textFieldValue(final C textField, final Class<T> valueClass,
                                                                              final UpdateOn updateOn, final Format format) {
    requireNonNull(textField);
    requireNonNull(valueClass);
    requireNonNull(updateOn);
    if (valueClass.equals(String.class)) {
      final ComponentValue<String, C> componentValue = textComponent(textField, format, updateOn);

      return (ComponentValue<T, C>) componentValue;
    }
    else if (valueClass.equals(Character.class)) {
      final ComponentValue<Character, JTextField> componentValue = characterTextField(textField, updateOn);

      return (ComponentValue<T, C>) componentValue;
    }
    else if (valueClass.equals(Integer.class)) {
      final ComponentValue<Integer, IntegerField> componentValue = integerFieldBuilder()
              .component((IntegerField) textField)
              .updateOn(updateOn)
              .build();

      return (ComponentValue<T, C>) componentValue;
    }
    else if (valueClass.equals(Double.class)) {
      final ComponentValue<Double, DoubleField> componentValue = doubleFieldBuilder()
              .component((DoubleField) textField)
              .updateOn(updateOn)
              .build();

      return (ComponentValue<T, C>) componentValue;
    }
    else if (valueClass.equals(BigDecimal.class)) {
      final ComponentValue<BigDecimal, BigDecimalField> componentValue = bigDecimalFieldBuilder()
              .component((BigDecimalField) textField)
              .updateOn(updateOn)
              .build();

      return (ComponentValue<T, C>) componentValue;
    }
    else if (valueClass.equals(Long.class)) {
      final ComponentValue<Long, LongField> componentValue = longFieldBuilder()
              .component((LongField) textField)
              .updateOn(updateOn)
              .build();

      return (ComponentValue<T, C>) componentValue;
    }
    else if (Temporal.class.isAssignableFrom(valueClass)) {
      final ComponentValue<Temporal, TemporalField<Temporal>> componentValue =
              temporalField((TemporalField<Temporal>) textField, updateOn);

      return (ComponentValue<T, C>) componentValue;
    }
    else {
      throw new IllegalArgumentException("Text fields not implemented for type: " + valueClass);
    }
  }
}
