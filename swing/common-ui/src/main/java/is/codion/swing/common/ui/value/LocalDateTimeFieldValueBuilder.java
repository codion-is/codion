/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import javax.swing.JFormattedTextField;
import java.time.LocalDateTime;

final class LocalDateTimeFieldValueBuilder extends AbstractTemporalFieldBuilder<LocalDateTime> {

  @Override
  public ComponentValue<LocalDateTime, JFormattedTextField> build() {
    validateForBuild();

    final TemporalFieldValue<LocalDateTime> fieldValue = new TemporalFieldValue<>(component,
            dateTimePattern, updateOn, LocalDateTime::parse);
    fieldValue.set(initialValue);

    return fieldValue;
  }
}
