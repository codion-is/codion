/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import javax.swing.JFormattedTextField;
import java.time.temporal.Temporal;

import static java.util.Objects.requireNonNull;

abstract class AbstractTemporalFieldBuilder<V extends Temporal>
        extends AbstractComponentValueBuilder<V, JFormattedTextField> implements TemporalFieldValueBuilder<V> {

  protected String dateTimePattern;
  protected UpdateOn updateOn = UpdateOn.KEYSTROKE;

  @Override
  public final TemporalFieldValueBuilder<V> component(final JFormattedTextField component) {
    return (TemporalFieldValueBuilder<V>) super.component(component);
  }

  @Override
  public final TemporalFieldValueBuilder<V> initalValue(final V initialValue) {
    return (TemporalFieldValueBuilder<V>) super.initalValue(initialValue);
  }

  @Override
  public final TemporalFieldValueBuilder<V> dateTimePattern(final String dateTimePattern) {
    this.dateTimePattern = requireNonNull(dateTimePattern);
    return this;
  }

  @Override
  public final TemporalFieldValueBuilder<V> updateOn(final UpdateOn updateOn) {
    this.updateOn = requireNonNull(updateOn);
    return this;
  }

  protected final void validateForBuild() {
    if (component == null) {
      throw new IllegalStateException("Component must bet set before building");
    }
    if (dateTimePattern == null) {
      throw new IllegalStateException("DateTimePattern must bet set before building");
    }
  }
}
