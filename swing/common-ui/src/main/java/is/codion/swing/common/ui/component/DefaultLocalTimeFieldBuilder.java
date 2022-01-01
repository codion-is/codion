/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.TemporalField;

import java.time.LocalTime;

final class DefaultLocalTimeFieldBuilder extends DefaultTemporalFieldBuilder<LocalTime, TemporalField<LocalTime>, LocalTimeFieldBuilder>
        implements LocalTimeFieldBuilder {

  DefaultLocalTimeFieldBuilder(final String dateTimePattern, final Value<LocalTime> linkedValue) {
    super(LocalTime.class, dateTimePattern, linkedValue);
  }
}
