/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

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
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.text.JTextComponent;
import java.math.BigDecimal;
import java.text.Format;
import java.time.temporal.Temporal;

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
    return bigDecimalField(bigDecimalField, true);
  }

  /**
   * @param bigDecimalField the component
   * @param nullable true if the value should be nullable
   * @return a Value bound to the given component
   */
  public static ComponentValue<BigDecimal, BigDecimalField> bigDecimalField(final BigDecimalField bigDecimalField,
                                                                            final boolean nullable) {
    return bigDecimalField(bigDecimalField, nullable, UpdateOn.KEYSTROKE);
  }

  /**
   * @param bigDecimalField the component
   * @param nullable true if the value should be nullable
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<BigDecimal, BigDecimalField> bigDecimalField(final BigDecimalField bigDecimalField,
                                                                            final boolean nullable, final UpdateOn updateOn) {
    return new BigDecimalFieldValue(bigDecimalField, nullable, updateOn);
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
    return doubleField(doubleField, true);
  }

  /**
   * @param doubleField the component
   * @param nullable true if the value should be nullable
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, DoubleField> doubleField(final DoubleField doubleField, final boolean nullable) {
    return doubleField(doubleField, nullable, UpdateOn.KEYSTROKE);
  }

  /**
   * @param doubleField the component
   * @param nullable true if the value should be nullable
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, DoubleField> doubleField(final DoubleField doubleField, final boolean nullable, final UpdateOn updateOn) {
    return new DoubleFieldValue(doubleField, nullable, updateOn);
  }

  /**
   * @param spinner the spinner
   * @return a Value bound to the given spinner
   */
  public static ComponentValue<Integer, JSpinner> integerSpinner(final JSpinner spinner) {
    return new SpinnerNumberValue<>(spinner);
  }

  /**
   * @param integerField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Integer, IntegerField> integerField(final IntegerField integerField) {
    return integerField(integerField, true);
  }

  /**
   * @param integerField the component
   * @param nullable true if the value should be nullable
   * @return a Value bound to the given component
   */
  public static ComponentValue<Integer, IntegerField> integerField(final IntegerField integerField, final boolean nullable) {
    return integerField(integerField, nullable, UpdateOn.KEYSTROKE);
  }

  /**
   * @param integerField the component
   * @param nullable true if the value should be nullable
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<Integer, IntegerField> integerField(final IntegerField integerField, final boolean nullable, final UpdateOn updateOn) {
    return new IntegerFieldValue(integerField, nullable, updateOn);
  }

  /**
   * @param longField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Long, LongField> longField(final LongField longField) {
    return longField(longField, true);
  }

  /**
   * @param longField the component
   * @param nullable true if the value should be nullable
   * @return a Value bound to the given component
   */
  public static ComponentValue<Long, LongField> longField(final LongField longField, final boolean nullable) {
    return longField(longField, nullable, UpdateOn.KEYSTROKE);
  }

  /**
   * @param longField the component
   * @param nullable true if the value should be nullable
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<Long, LongField> longField(final LongField longField, final boolean nullable, final UpdateOn updateOn) {
    return new LongFieldValue(longField, nullable, updateOn);
  }

  /**
   * @param spinner the spinner
   * @return a Value bound to the given spinner
   * @throws IllegalArgumentException in case the spinner model is not a SpinnerListModel
   */
  public static <T> ComponentValue<T, JSpinner> listSpinner(final JSpinner spinner) {
    return new SpinnerListValue<>(spinner);
  }

  /**
   * @param spinner the spinner
   * @return a Value bound to the given spinner
   * @throws IllegalArgumentException in case the spinner model is not a SpinnerListModel
   */
  public static <T> ComponentValue<T, JSpinner> itemSpinner(final JSpinner spinner) {
    return new SpinnerItemValue<>(spinner);
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
}
