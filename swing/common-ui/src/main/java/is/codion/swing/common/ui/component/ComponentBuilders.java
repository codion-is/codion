/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.TemporalField;

import javax.swing.ComboBoxModel;
import javax.swing.JTextField;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.function.Supplier;

/**
 * A factory for {@link ComponentBuilder}.
 */
public final class ComponentBuilders {

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
   * @return a builder for a component linked to the given value
   */
  public static <T> ValueListComboBoxBuilder<T> valueListComboBoxBuilder(final Value<T> value,
                                                                         final List<Item<T>> values) {
    return new DefaultValueListComboBoxBuilder<>(values, value);
  }

  /**
   * @param value the value
   * @return a builder for a component linked to the given value
   */
  public static <T> ComboBoxBuilder<T> comboBoxBuilder(final Value<T> value, final Class<T> typeClass, final ComboBoxModel<T> comboBoxModel) {
    return new DefaultComboBoxBuilder<>(value, typeClass, comboBoxModel);
  }

  /**
   * @param value the value
   * @return a builder for a component linked to the given value
   */
  public static <T extends Temporal> TemporalInputPanelBuilder<T> temporalInputPanelBuiler(final Value<T> value, final Supplier<TemporalField<T>> supplier) {
    return new DefaultTemporalInputPanelBuiler<>(value, supplier);
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
   * @return a builder for a component linked to the given value
   */
  public static <T> TextFieldBuilder<T> textFieldBuilder(final Value<T> value, final Class<T> typeClass, final Supplier<JTextField> textFieldSupplier) {
    return new DefaultTextFieldBuilder<>(value, typeClass, textFieldSupplier);
  }

  /**
   * @param value the value
   * @return a builder for a component linked to the given value
   */
  public static FormattedTextFieldBuilder formattedTextFieldBuilder(final Value<String> value) {
    return new DefaultFormattedTextFieldBuilder(value);
  }
}
