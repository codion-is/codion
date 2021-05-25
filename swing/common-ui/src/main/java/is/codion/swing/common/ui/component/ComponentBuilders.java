/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.ui.textfield.TemporalField;

import javax.swing.ComboBoxModel;
import javax.swing.JTextField;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
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
    if (valueClass.equals(LocalTime.class)) {
      return (TextFieldBuilder<T, C, B>) localTimeFieldBuilder();
    }
    if (valueClass.equals(LocalDate.class)) {
      return (TextFieldBuilder<T, C, B>) localDateFieldBuilder();
    }
    if (valueClass.equals(LocalDateTime.class)) {
      return (TextFieldBuilder<T, C, B>) localDateTimeFieldBuilder();
    }
    if (valueClass.equals(OffsetDateTime.class)) {
      return (TextFieldBuilder<T, C, B>) offsetDateTimeFieldBuilder();
    }
    if (valueClass.equals(Integer.class)) {
      return (TextFieldBuilder<T, C, B>) integerFieldBuilder();
    }
    else if (valueClass.equals(Long.class)) {
      return (TextFieldBuilder<T, C, B>) longFieldBuilder();
    }
    else if (valueClass.equals(Double.class)) {
      return (TextFieldBuilder<T, C, B>) doubleFieldBuilder();
    }
    else if (valueClass.equals(BigDecimal.class)) {
      return (TextFieldBuilder<T, C, B>) bigDecimalFieldBuilder();
    }

    return new DefaultTextFieldBuilder<>(valueClass);
  }

  /**
   * @param valueClass the temporal value type
   * @param <T> the value type
   * @param <C> the component type
   * @param <B> the builder type
   * @return a builder for temporal fields
   */
  public static <T extends Temporal, C extends TemporalField<T>, B extends TemporalFieldBuilder<T, C, B>> TemporalFieldBuilder<T, C, B> temporalFieldBuilder(final Class<T> valueClass) {
    return new DefaultTemporalFieldBuilder<>(valueClass);
  }

  /**
   * @return a builder for a component
   */
  public static LocalTimeFieldBuilder localTimeFieldBuilder() {
    return new DefaultLocalTimeFieldBuilder();
  }

  /**
   * @return a builder for a component
   */
  public static LocalDateFieldBuilder localDateFieldBuilder() {
    return new DefaultLocalDateFieldBuilder();
  }

  /**
   * @return a builder for a component
   */
  public static LocalDateTimeFieldBuilder localDateTimeFieldBuilder() {
    return new DefaultLocalDateTimeFieldBuilder();
  }

  /**
   * @return a builder for a component
   */
  public static OffsetDateTimeFieldBuilder offsetDateTimeFieldBuilder() {
    return new DefaultOffsetDateTimeFieldBuilder();
  }

  /**
   * @return a builder for a component
   */
  public static IntegerFieldBuilder integerFieldBuilder() {
    return new DefaultIntegerFieldBuilder();
  }

  /**
   * @return a builder for a component
   */
  public static LongFieldBuilder longFieldBuilder() {
    return new DefaultLongFieldBuilder();
  }

  /**
   * @return a builder for a component
   */
  public static DoubleFieldBuilder doubleFieldBuilder() {
    return new DefaultDoubleFieldBuilder();
  }

  /**
   * @return a builder for a component
   */
  public static BigDecimalFieldBuilder bigDecimalFieldBuilder() {
    return new DefaultBigDecimalFieldBuilder();
  }

  /**
   * @return a builder for a component
   */
  public static FormattedTextFieldBuilder formattedTextFieldBuilder() {
    return new DefaultFormattedTextFieldBuilder();
  }
}
