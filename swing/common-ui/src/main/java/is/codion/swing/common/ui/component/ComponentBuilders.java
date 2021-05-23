/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.TemporalField;

import javax.swing.ComboBoxModel;
import java.time.temporal.Temporal;
import java.util.List;

/**
 * A factory for {@link ComponentBuilder}.
 */
public final class ComponentBuilders {
  
  private ComponentBuilders() {}

  /**
   * @param value the value
   * @return a builder for a component linked to the given value
   */
  public static CheckBoxBuilder checkBoxBuilder(final Value<Boolean> value) {
    return new DefaultCheckBoxBuilder(value);
  }

  /**
   * @param value the value
   * @return a builder for a component linked to the given value
   */
  public static BooleanComboBoxBuilder booleanComboBoxBuilder(final Value<Boolean> value) {
    return new DefaultBooleanComboBoxBuilder(value);
  }

  /**
   * @param value the value
   * @param values the values
   * @param <T> the value type
   * @return a builder for a component linked to the given value
   */
  public static <T> ValueListComboBoxBuilder<T> valueListComboBoxBuilder(final Value<T> value,
                                                                         final List<Item<T>> values) {
    return new DefaultValueListComboBoxBuilder<>(values, value);
  }

  /**
   * @param value the value
   * @param valueClass the value class
   * @param comboBoxModel the combo box model
   * @param <T> the value type
   * @return a builder for a component linked to the given value
   */
  public static <T> ComboBoxBuilder<T> comboBoxBuilder(final Value<T> value, final Class<T> valueClass, final ComboBoxModel<T> comboBoxModel) {
    return new DefaultComboBoxBuilder<>(value, valueClass, comboBoxModel);
  }

  /**
   * @param value the value
   * @param temporalField the temporal field
   * @param <T> the value type
   * @return a builder for a component linked to the given value
   */
  public static <T extends Temporal> TemporalInputPanelBuilder<T> temporalInputPanelBuiler(final Value<T> value, final TemporalField<T> temporalField) {
    return new DefaultTemporalInputPanelBuiler<>(value, temporalField);
  }

  /**
   * @param value the value
   * @return a builder for a component linked to the given value
   */
  public static TextInputPanelBuilder textInputPanelBuilder(final Value<String> value) {
    return new DefaultTextInputPanelBuilder(value);
  }

  /**
   * @param value the value
   * @return a builder for a component linked to the given value
   */
  public static TextAreaBuilder textAreaBuilder(final Value<String> value) {
    return new DefaultTextAreaBuilder(value);
  }

  /**
   * @param value the value
   * @param <T> the value type
   * @param valueClass the value class
   * @return a builder for a component linked to the given value
   */
  public static <T> TextFieldBuilder<T> textFieldBuilder(final Value<T> value, final Class<T> valueClass) {
    return new DefaultTextFieldBuilder<>(value, valueClass);
  }

  /**
   * @param value the value
   * @return a builder for a component linked to the given value
   */
  public static FormattedTextFieldBuilder formattedTextFieldBuilder(final Value<String> value) {
    return new DefaultFormattedTextFieldBuilder(value);
  }
}
