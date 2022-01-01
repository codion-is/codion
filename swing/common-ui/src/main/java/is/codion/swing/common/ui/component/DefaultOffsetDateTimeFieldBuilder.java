/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.TemporalField;

import java.time.OffsetDateTime;

final class DefaultOffsetDateTimeFieldBuilder extends DefaultTemporalFieldBuilder<OffsetDateTime, TemporalField<OffsetDateTime>, OffsetDateTimeFieldBuilder>
        implements OffsetDateTimeFieldBuilder {

  DefaultOffsetDateTimeFieldBuilder(final String dateTimePattern, final Value<OffsetDateTime> linkedValue) {
    super(OffsetDateTime.class, dateTimePattern, linkedValue);
  }
}
