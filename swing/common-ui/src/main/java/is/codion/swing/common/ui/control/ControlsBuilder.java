/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

final class ControlsBuilder implements Controls.Builder {

  private final List<Action> controls = new ArrayList<>();

  private String name;
  private String description;
  private char mnemonic = 0;
  private StateObserver enabled;
  private Icon smallIcon;
  private Icon largeIcon;
  private KeyStroke keyStroke;

  @Override
  public Controls.Builder name(String name) {
    this.name = name;
    return this;
  }

  @Override
  public Controls.Builder description(String description) {
    this.description = description;
    return this;
  }

  @Override
  public Controls.Builder mnemonic(char mnenomic) {
    this.mnemonic = mnenomic;
    return this;
  }

  @Override
  public Controls.Builder keyStroke(KeyStroke keyStroke) {
    this.keyStroke = keyStroke;
    return this;
  }

  @Override
  public Controls.Builder enabled(StateObserver enabled) {
    this.enabled = enabled;
    return this;
  }

  @Override
  public Controls.Builder smallIcon(Icon smallIcon) {
    this.smallIcon = smallIcon;
    return this;
  }

  @Override
  public Control.Builder largeIcon(Icon largeIcon) {
    this.largeIcon = largeIcon;
    return this;
  }

  @Override
  public Controls.Builder control(Control control) {
    controls.add(requireNonNull(control));
    return this;
  }

  @Override
  public Controls.Builder control(Control.Builder controlBuilder) {
    controls.add(requireNonNull(controlBuilder).build());
    return this;
  }

  @Override
  public Controls.Builder controls(Control... controls) {
    this.controls.addAll(Arrays.asList(requireNonNull(controls)));
    return this;
  }

  @Override
  public Controls.Builder controls(Control.Builder... controlBuilders) {
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
  public Controls build() {
    Controls defaultControls = new DefaultControls(name, enabled, controls);
    defaultControls.setMnemonic(mnemonic);
    defaultControls.setSmallIcon(smallIcon);
    defaultControls.setLargeIcon(largeIcon);
    defaultControls.setDescription(description);
    defaultControls.setKeyStroke(keyStroke);

    return defaultControls;
  }

  private static final class BuildControl implements Function<Control.Builder, Control> {

    @Override
    public Control apply(Control.Builder builder) {
      return builder.build();
    }
  }
}
