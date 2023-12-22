/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.value.Value;

import static java.util.Objects.requireNonNull;

final class ToggleControlBuilder<B extends Control.Builder<ToggleControl, B>> extends AbstractControlBuilder<ToggleControl, B> {

  private final Value<Boolean> value;

  ToggleControlBuilder(Value<Boolean> value) {
    this.value = requireNonNull(value);
  }

  @Override
  protected ToggleControl createControl() {
    return new DefaultToggleControl(value, name, enabled);
  }
}
