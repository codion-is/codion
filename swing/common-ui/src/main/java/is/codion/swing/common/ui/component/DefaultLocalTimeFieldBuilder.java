/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.TemporalField;

import java.time.LocalTime;

final class DefaultLocalTimeFieldBuilder extends DefaultTemporalFieldBuilder<LocalTime, TemporalField<LocalTime>, LocalTimeFieldBuilder>
        implements LocalTimeFieldBuilder {

  DefaultLocalTimeFieldBuilder() {
    super(LocalTime.class);
  }
}
