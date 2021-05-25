/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.TemporalField;

import java.time.LocalDate;

final class DefaultLocalDateFieldBuilder extends DefaultTemporalFieldBuilder<LocalDate, TemporalField<LocalDate>, LocalDateFieldBuilder>
        implements LocalDateFieldBuilder {

  DefaultLocalDateFieldBuilder(final String dateTimePattern) {
    super(LocalDate.class, dateTimePattern);
  }
}
