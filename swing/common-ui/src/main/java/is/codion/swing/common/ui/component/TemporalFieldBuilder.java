/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.DateTimeParser;
import is.codion.swing.common.ui.textfield.TemporalField;

import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

/**
 * A builder for {@link TemporalField}.
 */
public interface TemporalFieldBuilder<T extends Temporal, C extends TemporalField<T>>
        extends TextFieldBuilder<T, C, TemporalFieldBuilder<T, C>>, TemporalField.Builder<T> {

  @Override
  TemporalFieldBuilder<T, C> dateTimeFormatter(DateTimeFormatter dateTimeFormatter);

  @Override
  TemporalFieldBuilder<T, C> dateTimeParser(DateTimeParser<T> dateTimeParser);

  @Override
  TemporalFieldBuilder<T, C> focusLostBehaviour(int focusLostBehaviour);
}
