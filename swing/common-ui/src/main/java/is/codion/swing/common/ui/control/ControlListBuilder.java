/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;

import javax.swing.Action;
import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class ControlListBuilder implements ControlList.Builder {

  private final List<Action> controls = new ArrayList<>();

  private String name;
  private char mnemonic = 0;
  private StateObserver enabledState;
  private Icon icon;

  @Override
  public ControlList.Builder name(final String name) {
    this.name = requireNonNull(name);
    return this;
  }

  @Override
  public ControlList.Builder mnemonic(final char mnenomic) {
    this.mnemonic = mnenomic;
    return this;
  }

  @Override
  public ControlList.Builder enabledState(final StateObserver enabledState) {
    this.enabledState = requireNonNull(enabledState);
    return this;
  }

  @Override
  public ControlList.Builder icon(final Icon icon) {
    this.icon = requireNonNull(icon);
    return this;
  }

  @Override
  public ControlList.Builder control(final Control control) {
    controls.add(requireNonNull(control));
    return this;
  }

  @Override
  public ControlList.Builder controls(final Control... controls) {
    this.controls.addAll(Arrays.asList(requireNonNull(controls)));
    return this;
  }

  @Override
  public ControlList.Builder action(final Action action) {
    this.controls.add(requireNonNull(action));
    return this;
  }

  @Override
  public ControlList.Builder actions(final Action... actions) {
    this.controls.addAll(Arrays.asList(requireNonNull(actions)));
    return this;
  }

  @Override
  public ControlList.Builder separator() {
    this.controls.add(null);
    return this;
  }

  @Override
  public ControlList build() {
    return new DefaultControlList(name, mnemonic, enabledState, icon, controls);
  }
}
