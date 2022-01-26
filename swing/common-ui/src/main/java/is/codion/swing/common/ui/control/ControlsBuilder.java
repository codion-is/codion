/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

final class ControlsBuilder implements Controls.Builder {

  private final List<Action> controls = new ArrayList<>();

  private String caption;
  private String description;
  private char mnemonic = 0;
  private StateObserver enabledState;
  private Icon smallIcon;
  private KeyStroke keyStroke;

  @Override
  public Controls.Builder caption(final String caption) {
    this.caption = caption;
    return this;
  }

  @Override
  public Controls.Builder description(final String description) {
    this.description = description;
    return this;
  }

  @Override
  public Controls.Builder mnemonic(final char mnenomic) {
    this.mnemonic = mnenomic;
    return this;
  }

  @Override
  public Controls.Builder keyStroke(final KeyStroke keyStroke) {
    this.keyStroke = keyStroke;
    return this;
  }

  @Override
  public Controls.Builder enabledState(final StateObserver enabledState) {
    this.enabledState = enabledState;
    return this;
  }

  @Override
  public Controls.Builder smallIcon(final Icon smallIcon) {
    this.smallIcon = smallIcon;
    return this;
  }

  @Override
  public Controls.Builder control(final Control control) {
    controls.add(requireNonNull(control));
    return this;
  }

  @Override
  public Controls.Builder control(final Control.Builder controlBuilder) {
    controls.add(requireNonNull(controlBuilder).build());
    return this;
  }

  @Override
  public Controls.Builder controls(final Control... controls) {
    this.controls.addAll(Arrays.asList(requireNonNull(controls)));
    return this;
  }

  @Override
  public Controls.Builder controls(final Control.Builder... controlBuilders) {
    this.controls.addAll(Arrays.stream(controlBuilders)
            .map(Control.Builder::build)
            .collect(Collectors.toList()));
    return this;
  }

  @Override
  public Controls.Builder action(final Action action) {
    this.controls.add(requireNonNull(action));
    return this;
  }

  @Override
  public Controls.Builder actions(final Action... actions) {
    this.controls.addAll(Arrays.asList(requireNonNull(actions)));
    return this;
  }

  @Override
  public Controls.Builder separator() {
    this.controls.add(null);
    return this;
  }

  @Override
  public Controls build() {
    return (Controls) new DefaultControls(caption, mnemonic, enabledState, smallIcon, controls)
            .setDescription(description)
            .setKeyStroke(keyStroke);
  }
}
