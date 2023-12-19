/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
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
      if (control == SEPARATOR) {
        addSeparator();
      }
      else {
        add(control);
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
    actions.remove(action);
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
  public boolean empty() {
    return actions.isEmpty();
  }

  @Override
  public boolean notEmpty() {
    return !empty();
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
    actions.add(SEPARATOR);
    return this;
  }

  @Override
  public Controls addSeparatorAt(int index) {
    actions.add(index, SEPARATOR);
    return this;
  }

  @Override
  public Controls addAll(Controls controls) {
    actions.addAll(requireNonNull(controls, CONTROLS_PARAMETER).actions());
    return this;
  }

  @Override
  public void actionPerformed(ActionEvent e) {/*Not required*/}

  static final class DefaultSeparator implements Action {

    private static final String CONTROLS_SEPARATOR = "Separator";

    DefaultSeparator() {}

    @Override
    public Object getValue(String key) {
      throw new UnsupportedOperationException(CONTROLS_SEPARATOR);
    }

    @Override
    public void putValue(String key, Object value) {
      throw new UnsupportedOperationException(CONTROLS_SEPARATOR);
    }

    @Override
    public void setEnabled(boolean b) {
      throw new UnsupportedOperationException(CONTROLS_SEPARATOR);
    }

    @Override
    public boolean isEnabled() {
      throw new UnsupportedOperationException(CONTROLS_SEPARATOR);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
      throw new UnsupportedOperationException(CONTROLS_SEPARATOR);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
      throw new UnsupportedOperationException(CONTROLS_SEPARATOR);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      throw new UnsupportedOperationException(CONTROLS_SEPARATOR);
    }
  }
}
