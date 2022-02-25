/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.FileInputPanel;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.textfield.TemporalInputPanel;
import is.codion.swing.common.ui.textfield.TextInputPanel;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.text.JTextComponent;
import java.math.BigDecimal;
import java.text.Format;
import java.time.temporal.Temporal;

import static is.codion.swing.common.ui.textfield.TextComponents.formattedTextComponentValue;

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
  public static <C extends JTextComponent> ComponentValue<String, C> textComponent(C textComponent) {
    return textComponent(textComponent, null, UpdateOn.KEYSTROKE);
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @param <C> the text component type
   * @return a Value bound to the given component
   */
  public static <C extends JTextComponent> ComponentValue<String, C> textComponent(C textComponent,
                                                                                   Format format) {
    return textComponent(textComponent, format, UpdateOn.KEYSTROKE);
  }

  /**
   * @param textComponent the component
   * @param updateOn specifies when the underlying value should be updated
   * @param <C> the text component type
   * @return a Value bound to the given component
   */
  public static <C extends JTextComponent> ComponentValue<String, C> textComponent(C textComponent,
                                                                                   UpdateOn updateOn) {
    return textComponent(textComponent, null, updateOn);
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @param updateOn specifies when the underlying value should be updated
   * @param <C> the text component type
   * @return a Value bound to the given component
   */
  public static <C extends JTextComponent> ComponentValue<String, C> textComponent(C textComponent,
                                                                                   Format format,
                                                                                   UpdateOn updateOn) {
    return formattedTextComponentValue(textComponent, format, updateOn);
  }

  /**
   * @param textField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Character, JTextField> characterTextField(JTextField textField) {
    return characterTextField(textField, UpdateOn.KEYSTROKE);
  }

  /**
   * @param textField the component
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<Character, JTextField> characterTextField(JTextField textField, UpdateOn updateOn) {
    return new CharacterFieldValue(textField, updateOn);
  }

  /**
   * Instantiates a new String based ComponentValue.
   * @param textInputPanel the text input panel to base this component value on
   * @return a String based ComponentValue
   */
  public static ComponentValue<String, TextInputPanel> textInputPanel(TextInputPanel textInputPanel) {
    return textInputPanel.componentValue();
  }

  /**
   * Instantiates a new {@link ComponentValue} for {@link Temporal} values.
   * @param inputPanel the input panel to use
   * @param <T> the temporal value type
   * @return a Value bound to the given component
   */
  public static <T extends Temporal> ComponentValue<T, TemporalInputPanel<T>> temporalInputPanel(TemporalInputPanel<T> inputPanel) {
    return inputPanel.componentValue();
  }

  /**
   * Instantiates a new {@link ComponentValue} for {@link Temporal} values.
   * @param temporalField the temporal field to use
   * @param <T> the temporal value type
   * @return a Value bound to the given component
   */
  public static <T extends Temporal> ComponentValue<T, TemporalField<T>> temporalField(TemporalField<T> temporalField) {
    return temporalField(temporalField, UpdateOn.KEYSTROKE);
  }

  /**
   * Instantiates a new {@link ComponentValue} for {@link Temporal} values.
   * @param temporalField the temporal field to use
   * @param updateOn specifies when the underlying value should be updated
   * @param <T> the temporal value type
   * @return a Value bound to the given component
   */
  public static <T extends Temporal> ComponentValue<T, TemporalField<T>> temporalField(TemporalField<T> temporalField,
                                                                                       UpdateOn updateOn) {
    return temporalField.componentValue(updateOn);
  }

  /**
   * Instantiates an Item based ComponentValue.
   * @param <T> the value type
   * @param <C> the combo box type
   * @param comboBox the combo box
   * @return a Value bound to the given component
   */
  public static <T, C extends JComboBox<Item<T>>> ComponentValue<T, C> itemComboBox(C comboBox) {
    return new SelectedItemValue<>(comboBox);
  }

  /**
   * @param <T> the value type
   * @param <C> the combo box type
   * @param comboBox the combo box
   * @return a Value bound to the given component
   */
  public static <T, C extends JComboBox<T>> ComponentValue<T, C> comboBox(C comboBox) {
    return new SelectedValue<>(comboBox);
  }

  /**
   * @return a file based ComponentValue
   */
  public static ComponentValue<byte[], FileInputPanel> fileInputPanel() {
    return new FileInputPanelValue();
  }

  /**
   * Creates a boolean value based on the given toggle button.
   * If the button is a {@link NullableCheckBox} the value will be nullable otherwise not
   * @param toggleButton the toggle button
   * @param <T> the attribute type
   * @return a Value bound to the given button
   */
  public static <T extends JToggleButton> ComponentValue<Boolean, T> toggleButton(JToggleButton toggleButton) {
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
  public static ComponentValue<Boolean, JComboBox<Item<Boolean>>> booleanComboBox(JComboBox<Item<Boolean>> comboBox) {
    return new BooleanComboBoxValue(comboBox);
  }

  /**
   * @param bigDecimalField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<BigDecimal, BigDecimalField> bigDecimalField(BigDecimalField bigDecimalField) {
    return bigDecimalField(bigDecimalField, true);
  }

  /**
   * @param bigDecimalField the component
   * @param nullable true if the value should be nullable
   * @return a Value bound to the given component
   */
  public static ComponentValue<BigDecimal, BigDecimalField> bigDecimalField(BigDecimalField bigDecimalField,
                                                                            boolean nullable) {
    return bigDecimalField(bigDecimalField, nullable, UpdateOn.KEYSTROKE);
  }

  /**
   * @param bigDecimalField the component
   * @param nullable true if the value should be nullable
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<BigDecimal, BigDecimalField> bigDecimalField(BigDecimalField bigDecimalField,
                                                                            boolean nullable, UpdateOn updateOn) {
    return new BigDecimalFieldValue(bigDecimalField, nullable, updateOn);
  }

  /**
   * @param spinner the spinner
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, JSpinner> doubleSpinner(JSpinner spinner) {
    return new SpinnerNumberValue<>(spinner);
  }

  /**
   * @param doubleField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, DoubleField> doubleField(DoubleField doubleField) {
    return doubleField(doubleField, true);
  }

  /**
   * @param doubleField the component
   * @param nullable true if the value should be nullable
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, DoubleField> doubleField(DoubleField doubleField, boolean nullable) {
    return doubleField(doubleField, nullable, UpdateOn.KEYSTROKE);
  }

  /**
   * @param doubleField the component
   * @param nullable true if the value should be nullable
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, DoubleField> doubleField(DoubleField doubleField, boolean nullable, UpdateOn updateOn) {
    return new DoubleFieldValue(doubleField, nullable, updateOn);
  }

  /**
   * @param spinner the spinner
   * @return a Value bound to the given spinner
   */
  public static ComponentValue<Integer, JSpinner> integerSpinner(JSpinner spinner) {
    return new SpinnerNumberValue<>(spinner);
  }

  /**
   * @param integerField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Integer, IntegerField> integerField(IntegerField integerField) {
    return integerField(integerField, true);
  }

  /**
   * @param integerField the component
   * @param nullable true if the value should be nullable
   * @return a Value bound to the given component
   */
  public static ComponentValue<Integer, IntegerField> integerField(IntegerField integerField, boolean nullable) {
    return integerField(integerField, nullable, UpdateOn.KEYSTROKE);
  }

  /**
   * @param integerField the component
   * @param nullable true if the value should be nullable
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<Integer, IntegerField> integerField(IntegerField integerField, boolean nullable, UpdateOn updateOn) {
    return new IntegerFieldValue(integerField, nullable, updateOn);
  }

  /**
   * @param longField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Long, LongField> longField(LongField longField) {
    return longField(longField, true);
  }

  /**
   * @param longField the component
   * @param nullable true if the value should be nullable
   * @return a Value bound to the given component
   */
  public static ComponentValue<Long, LongField> longField(LongField longField, boolean nullable) {
    return longField(longField, nullable, UpdateOn.KEYSTROKE);
  }

  /**
   * @param longField the component
   * @param nullable true if the value should be nullable
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<Long, LongField> longField(LongField longField, boolean nullable, UpdateOn updateOn) {
    return new LongFieldValue(longField, nullable, updateOn);
  }

  /**
   * A single selection JList component value.
   * @param <T> the value type
   * @param list the component
   * @return a Value bound to the given component
   */
  public static <T> ComponentValue<T, JList<T>> list(JList<T> list) {
    return new ListValue<>(list);
  }

  /**
   * @param <T> the value type
   * @param spinner the spinner
   * @return a Value bound to the given spinner
   * @throws IllegalArgumentException in case the spinner model is not a SpinnerListModel
   */
  public static <T> ComponentValue<T, JSpinner> listSpinner(JSpinner spinner) {
    return new SpinnerListValue<>(spinner);
  }

  /**
   * @param <T> the value type
   * @param spinner the spinner
   * @return a Value bound to the given spinner
   * @throws IllegalArgumentException in case the spinner model is not a SpinnerListModel
   */
  public static <T> ComponentValue<T, JSpinner> itemSpinner(JSpinner spinner) {
    return new SpinnerItemValue<>(spinner);
  }

  /**
   * @param progressBar the progress bar
   * @return a Value bound to the given progress bar
   */
  public static ComponentValue<Integer, JProgressBar> progressBar(JProgressBar progressBar) {
    return new IntegerProgressBarValue(progressBar);
  }

  /**
   * @param slider the slider
   * @return a Value bound to the given slider
   */
  public static ComponentValue<Integer, JSlider> slider(JSlider slider) {
    return new IntegerSliderValue(slider);
  }
}
