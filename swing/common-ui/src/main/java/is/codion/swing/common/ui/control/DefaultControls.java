/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultControls extends AbstractControl implements Controls {

  private static final String CONTROLS_PARAMETER = "controls";

  private final List<Action> actions = new ArrayList<>();

  DefaultControls(String name, StateObserver enabledState, List<Action> controls) {
    super(name, enabledState);
    for (Action control : controls) {
      if (control != null) {
        add(control);
      }
      else {
        addSeparator();
      }
    }
  }

  @Override
  public List<Action> actions() {
    return unmodifiableList(actions);
  }

  @Override
  public Controls add(Action action) {
    actions.add(requireNonNull(action, "action"));
    return this;
  }

  @Override
  public Controls addAt(int index, Action action) {
    actions.add(index, requireNonNull(action, "action"));
    return this;
  }

  @Override
  public Controls remove(Action action) {
    if (action != null) {
      actions.remove(action);
    }
    return this;
  }

  @Override
  public Controls removeAll() {
    actions.clear();
    return this;
  }

  @Override
  public int size() {
    return actions.size();
  }

  @Override
  public boolean isEmpty() {
    return actions.isEmpty();
  }

  @Override
  public Action get(int index) {
    return actions.get(index);
  }

  @Override
  public Controls add(Controls controls) {
    actions.add(requireNonNull(controls, CONTROLS_PARAMETER));
    return this;
  }

  @Override
  public Controls addAt(int index, Controls controls) {
    actions.add(index, requireNonNull(controls, CONTROLS_PARAMETER));
    return this;
  }

  @Override
  public Controls addSeparator() {
    actions.add(null);
    return this;
  }

  @Override
  public Controls addSeparatorAt(int index) {
    actions.add(index, null);
    return this;
  }

  @Override
  public Controls addAll(Controls controls) {
    actions.addAll(requireNonNull(controls, CONTROLS_PARAMETER).actions());
    return this;
  }

  @Override
  public void actionPerformed(ActionEvent e) {/*Not required*/}
}
