/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import javax.swing.JFormattedTextField;
import java.time.OffsetDateTime;

final class OffsetDateTimeFieldValueBuilder extends AbstractTemporalFieldBuilder<OffsetDateTime> {

  @Override
  public ComponentValue<OffsetDateTime, JFormattedTextField> build() {
    validateForBuild();

    final TemporalFieldValue<OffsetDateTime> fieldValue = new TemporalFieldValue<>(component,
            dateTimePattern, updateOn, OffsetDateTime::parse);
    fieldValue.set(initialValue);

    return fieldValue;
  }
}
