/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import javax.swing.JFormattedTextField;
import java.time.LocalDate;

final class LocalDateFieldValueBuilder extends AbstractTemporalFieldBuilder<LocalDate> {

  @Override
  public ComponentValue<LocalDate, JFormattedTextField> build() {
    validateForBuild();

    final TemporalFieldValue<LocalDate> fieldValue = new TemporalFieldValue<>(component,
            dateTimePattern, updateOn, LocalDate::parse);
    fieldValue.set(initialValue);

    return fieldValue;
  }
}
