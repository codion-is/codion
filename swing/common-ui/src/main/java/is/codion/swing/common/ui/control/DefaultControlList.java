/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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
  public ControlList add(final Action action) {
    actions.add(requireNonNull(action, "action"));
    return this;
  }

  @Override
  public ControlList addAt(final int index, final Action action) {
    actions.add(index, requireNonNull(action, "action"));
    return this;
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
  public boolean isEmpty() {
    return actions.isEmpty();
  }

  @Override
  public Action get(final int index) {
    return actions.get(index);
  }

  @Override
  public ControlList add(final ControlList controls) {
    actions.add(requireNonNull(controls, "controls"));
    return this;
  }

  @Override
  public ControlList addAt(final int index, final ControlList controls) {
    actions.add(index, requireNonNull(controls, "controls"));
    return this;
  }

  @Override
  public ControlList addSeparator() {
    actions.add(null);
    return this;
  }

  @Override
  public ControlList addSeparatorAt(final int index) {
    actions.add(index, null);
    return this;
  }

  @Override
  public ControlList addAll(final ControlList controls) {
    actions.addAll(requireNonNull(controls, "controls").getActions());
    return this;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {/*Not required*/}
}
