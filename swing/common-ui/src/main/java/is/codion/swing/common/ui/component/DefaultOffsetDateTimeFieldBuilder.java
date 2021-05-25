/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.TemporalField;

import java.time.OffsetDateTime;

final class DefaultOffsetDateTimeFieldBuilder extends DefaultTemporalFieldBuilder<OffsetDateTime, TemporalField<OffsetDateTime>, OffsetDateTimeFieldBuilder>
        implements OffsetDateTimeFieldBuilder{

  DefaultOffsetDateTimeFieldBuilder() {
    super(OffsetDateTime.class);
  }
}
