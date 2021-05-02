/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import javax.swing.JFormattedTextField;
import java.time.temporal.Temporal;

/**
 * A builder for Values based on a numerical field
 * @param <V> the value type
 */
public interface TemporalFieldValueBuilder<V extends Temporal> extends ComponentValue.Builder<V, JFormattedTextField> {

  @Override
  TemporalFieldValueBuilder<V> component(JFormattedTextField component);

  @Override
  TemporalFieldValueBuilder<V> initalValue(V initialValue);

  /**
   * @param dateTimePattern the date time pattern
   * @return this builder instace
   */
  TemporalFieldValueBuilder<V> dateTimePattern(String dateTimePattern);

  /**
   * @param updateOn specifies when the underlying value should be updated
   * @return this builder instace
   */
  TemporalFieldValueBuilder<V> updateOn(UpdateOn updateOn);
}
