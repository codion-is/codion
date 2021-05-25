/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.TemporalField;

import java.time.LocalDateTime;

final class DefaultLocalDateTimeFieldBuilder extends DefaultTemporalFieldBuilder<LocalDateTime, TemporalField<LocalDateTime>, LocalDateTimeFieldBuilder>
        implements LocalDateTimeFieldBuilder {

  DefaultLocalDateTimeFieldBuilder(final String dateTimePattern) {
    super(LocalDateTime.class, dateTimePattern);
  }
}
