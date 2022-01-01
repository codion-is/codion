/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.TemporalField;

import java.time.LocalDateTime;

final class DefaultLocalDateTimeFieldBuilder extends DefaultTemporalFieldBuilder<LocalDateTime, TemporalField<LocalDateTime>, LocalDateTimeFieldBuilder>
        implements LocalDateTimeFieldBuilder {

  DefaultLocalDateTimeFieldBuilder(final String dateTimePattern, final Value<LocalDateTime> linkedValue) {
    super(LocalDateTime.class, dateTimePattern, linkedValue);
  }
}
