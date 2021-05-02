/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.ui.textfield.NumberField;

import java.text.NumberFormat;

import static java.util.Objects.requireNonNull;

abstract class AbstractNumberFieldValueBuilder<F extends NumberFormat, V extends Number, C extends NumberField<V>>
        extends AbstractComponentValueBuilder<V, C> implements NumberFieldValueBuilder<V, C, F> {

  protected boolean nullable = true;
  protected UpdateOn updateOn = UpdateOn.KEYSTROKE;
  protected F format;
  protected int columns;

  @Override
  public NumberFieldValueBuilder<V, C, F> component(final C component) {
    return (NumberFieldValueBuilder<V, C, F>) super.component(component);
  }

  @Override
  public NumberFieldValueBuilder<V, C, F> initalValue(final V initialValue) {
    return (NumberFieldValueBuilder<V, C, F>) super.initalValue(initialValue);
  }

  @Override
  public NumberFieldValueBuilder<V, C, F> nullable(final boolean nullable) {
    this.nullable = nullable;
    return this;
  }

  @Override
  public NumberFieldValueBuilder<V, C, F> updateOn(final UpdateOn updateOn) {
    this.updateOn = requireNonNull(updateOn);
    return this;
  }

  @Override
  public NumberFieldValueBuilder<V, C, F> format(final F format) {
    if (component != null) {
      throw new IllegalStateException("Component has already been set");
    }
    this.format = requireNonNull(format);
    return this;
  }

  @Override
  public NumberFieldValueBuilder<V, C, F> columns(final int columns) {
    if (component != null) {
      throw new IllegalStateException("Component has already been set");
    }
    this.columns = columns;
    return null;
  }
}
