/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;

import javax.swing.ComboBoxModel;
import javax.swing.JTextField;
import java.math.BigDecimal;
import java.time.temporal.Temporal;
import java.util.List;

/**
 * A factory for {@link ComponentBuilder}.
 */
public final class ComponentBuilders {
  
  private ComponentBuilders() {}

  /**
   * @return a builder for a component linked to the given value
   */
  public static CheckBoxBuilder checkBoxBuilder() {
    return new DefaultCheckBoxBuilder();
  }

  /**
   * @param comboBoxModel the combo box model
   * @return a builder for a component linked to the given value
   */
  public static BooleanComboBoxBuilder booleanComboBoxBuilder(final BooleanComboBoxModel comboBoxModel) {
    return new DefaultBooleanComboBoxBuilder(comboBoxModel);
  }

  /**
   * @param values the values
   * @param <T> the value type
   * @return a builder for a component linked to the given value
   */
  public static <T> ValueListComboBoxBuilder<T> valueListComboBoxBuilder(final List<Item<T>> values) {
    return new DefaultValueListComboBoxBuilder<>(values);
  }

  /**
   * @param valueClass the value class
   * @param comboBoxModel the combo box model
   * @param <T> the value type
   * @return a builder for a component linked to the given value
   */
  public static <T> ComboBoxBuilder<T> comboBoxBuilder(final Class<T> valueClass, final ComboBoxModel<T> comboBoxModel) {
    return new DefaultComboBoxBuilder<>(valueClass, comboBoxModel);
  }

  /**
   * @param <T> the value type
   * @param valueClass the value class
   * @return a builder for a component linked to the given value
   */
  public static <T extends Temporal> TemporalInputPanelBuilder<T> temporalInputPanelBuiler(final Class<T> valueClass) {
    return new DefaultTemporalInputPanelBuiler<>(valueClass);
  }

  /**
   * @return a builder for a component linked to the given value
   */
  public static TextInputPanelBuilder textInputPanelBuilder() {
    return new DefaultTextInputPanelBuilder();
  }

  /**
   * @return a builder for a component linked to the given value
   */
  public static TextAreaBuilder textAreaBuilder() {
    return new DefaultTextAreaBuilder();
  }

  /**
   * @param <T> the value type
   * @param <C> the text field type
   * @param <B> the builder type
   * @param valueClass the value class
   * @return a builder for a component linked to the given value
   */
  public static <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> TextFieldBuilder<T, C, B> textFieldBuilder(final Class<T> valueClass) {
    return new DefaultTextFieldBuilder<>(valueClass);
  }

  /**
   * @return a builder for a component linked to the given value
   */
  public static TextFieldBuilder<Integer, IntegerField, IntegerFieldBuilder> integerFieldBuilder() {
    return new DefaultTextFieldBuilder<>(Integer.class);
  }

  /**
   * @return a builder for a component linked to the given value
   */
  public static TextFieldBuilder<Long, LongField, LongFieldBuilder> longFieldBuilder() {
    return new DefaultTextFieldBuilder<>(Long.class);
  }

  /**
   * @return a builder for a component linked to the given value
   */
  public static TextFieldBuilder<Double, DoubleField, DoubleFieldBuilder> doubleFieldBuilder() {
    return new DefaultTextFieldBuilder<>(Double.class);
  }

  /**
   * @return a builder for a component linked to the given value
   */
  public static TextFieldBuilder<BigDecimal, BigDecimalField, BigDecimalFieldBuilder> bigDecimalFieldBuilder() {
    return new DefaultTextFieldBuilder<>(BigDecimal.class);
  }

  /**
   * @return a builder for a component linked to the given value
   */
  public static FormattedTextFieldBuilder formattedTextFieldBuilder() {
    return new DefaultFormattedTextFieldBuilder();
  }
}
