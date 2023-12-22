/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import javax.swing.Action;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

final class ControlsBuilder extends AbstractControlBuilder<Controls, Controls.Builder> implements Controls.Builder {

  private final List<Action> controls = new ArrayList<>();

  @Override
  public Controls.Builder control(Control control) {
    controls.add(requireNonNull(control));
    return this;
  }

  @Override
  public Controls.Builder control(Control.Builder<?, ?> controlBuilder) {
    controls.add(requireNonNull(controlBuilder).build());
    return this;
  }

  @Override
  public Controls.Builder controls(Control... controls) {
    this.controls.addAll(Arrays.asList(requireNonNull(controls)));
    return this;
  }

  @Override
  public Controls.Builder controls(Control.Builder<?, ?>... controlBuilders) {
    this.controls.addAll(Arrays.stream(controlBuilders)
            .map(new BuildControl())
            .collect(Collectors.toList()));
    return this;
  }

  @Override
  public Controls.Builder action(Action action) {
    this.controls.add(requireNonNull(action));
    return this;
  }

  @Override
  public Controls.Builder actions(Action... actions) {
    this.controls.addAll(Arrays.asList(requireNonNull(actions)));
    return this;
  }

  @Override
  public Controls.Builder separator() {
    this.controls.add(Controls.SEPARATOR);
    return this;
  }

  @Override
  protected Controls createControl() {
    return new DefaultControls(name, enabled, controls);
  }

  private static final class BuildControl implements Function<Control.Builder<?, ?>, Control> {

    @Override
    public Control apply(Control.Builder<?, ?> builder) {
      return builder.build();
    }
  }
}
