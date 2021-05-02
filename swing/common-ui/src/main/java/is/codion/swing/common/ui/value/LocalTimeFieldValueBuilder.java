/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import javax.swing.JFormattedTextField;
import java.time.LocalTime;

final class LocalTimeFieldValueBuilder extends AbstractTemporalFieldBuilder<LocalTime> {

  @Override
  public ComponentValue<LocalTime, JFormattedTextField> build() {
    validateForBuild();

    final TemporalFieldValue<LocalTime> fieldValue = new TemporalFieldValue<>(component,
            dateTimePattern, updateOn, LocalTime::parse);
    fieldValue.set(initialValue);

    return fieldValue;
  }
}
