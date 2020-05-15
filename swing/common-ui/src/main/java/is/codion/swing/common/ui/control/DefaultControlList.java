/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;

import javax.swing.Action;
import javax.swing.Icon;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultControlList extends AbstractControl implements ControlList {

  private final List<Action> actions = new ArrayList<>();

  DefaultControlList(final String name, final char mnemonic, final StateObserver enabledState, final Icon icon,
                     final Control... controls) {
    super(name, enabledState, icon);
    setMnemonic(mnemonic);
    for (final Control control : controls) {
      add(control);
    }
  }

  @Override
  public List<ControlList> getControlLists() {
    return actions.stream().filter(control -> control instanceof DefaultControlList)
            .map(control -> (DefaultControlList) control).collect(toList());
  }

  @Override
  public List<Action> getActions() {
    return unmodifiableList(actions);
  }

  @Override
  public void add(final Action action) {
    actions.add(action);
  }

  @Override
  public void addAt(final int index, final Action action) {
    actions.add(index, action);
  }

  @Override
  public boolean remove(final Action action) {
    return action != null && actions.remove(action);
  }

  @Override
  public void removeAll() {
    actions.clear();
  }

  @Override
  public int size() {
    return actions.size();
  }

  @Override
  public Action get(final int index) {
    return actions.get(index);
  }

  @Override
  public void add(final ControlList controls) {
    requireNonNull(controls, "controls");
    actions.add(controls);
  }

  @Override
  public void addAt(final int index, final ControlList controls) {
    requireNonNull(controls, "controls");
    actions.add(index, controls);
  }

  @Override
  public void addSeparator() {
    actions.add(null);
  }

  @Override
  public void addSeparatorAt(final int index) {
    actions.add(index, null);
  }

  @Override
  public void addAll(final ControlList controls) {
    actions.addAll(requireNonNull(controls, "controls").getActions());
  }

  @Override
  public void actionPerformed(final ActionEvent e) {/*Not required*/}
}
