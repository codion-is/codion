/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;

import javax.swing.ComboBoxModel;
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
   * @param valueClass the value class
   * @return a builder for a component linked to the given value
   */
  public static <T> TextFieldBuilder<T> textFieldBuilder(final Class<T> valueClass) {
    return new DefaultTextFieldBuilder<>(valueClass);
  }

  /**
   * @return a builder for a component linked to the given value
   */
  public static FormattedTextFieldBuilder formattedTextFieldBuilder() {
    return new DefaultFormattedTextFieldBuilder();
  }
}
