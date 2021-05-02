/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.ui.textfield.NumberField;

import java.text.NumberFormat;

/**
 * A builder for Values based on a numerical field
 * @param <V> the value type
 * @param <C> the component type
 * @param <F> the format type
 */
public interface NumberFieldValueBuilder<V extends Number, C extends NumberField<V>, F extends NumberFormat> extends ComponentValueBuilder<V, C> {

  /**
   * @param component the component
   * @return this builder instace
   */
  @Override
  NumberFieldValueBuilder<V, C, F> component(C component);

  /**
   * @param initialValue the initial value
   * @return this builder instace
   */
  @Override
  NumberFieldValueBuilder<V, C, F> initalValue(V initialValue);

  /**
   * @param nullable if false then the resulting Value translates null to 0
   * @return this builder instace
   */
  NumberFieldValueBuilder<V, C, F> nullable(boolean nullable);

  /**
   * @param updateOn specifies when the underlying value should be updated
   * @return this builder instace
   */
  NumberFieldValueBuilder<V, C, F> updateOn(UpdateOn updateOn);

  /**
   * @param format the number format to use
   * @return this builder instace
   */
  NumberFieldValueBuilder<V, C, F> format(F format);

  /**
   * @param columns the number of text field columns
   * @return this builder instace
   */
  NumberFieldValueBuilder<V, C, F> columns(int columns);
}
