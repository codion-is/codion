/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static java.util.Objects.requireNonNull;

final class DefaultStateGroup implements State.Group {

  private final List<WeakReference<State>> members = new ArrayList<>();

  DefaultStateGroup(State... states) {
    for (State state : requireNonNull(states, "states")) {
      addState(state);
    }
  }

  @Override
  public void addState(State state) {
    requireNonNull(state, "state");
    synchronized (members) {
      for (WeakReference<State> reference : members) {
        if (reference.get() == state) {
          return;
        }
      }//no duplicate states

      members.add(new WeakReference<>(state));
    }
    synchronized (members) {
      if (state.get()) {
        disableOthers(state);
      }
    }
    state.addDataListener(value -> {
      synchronized (members) {
        if (value) {
          disableOthers(state);
        }
      }
    });
  }

  private void disableOthers(State state) {
    ListIterator<WeakReference<State>> iterator = members.listIterator();
    while (iterator.hasNext()) {
      State memberState = iterator.next().get();
      if (memberState == null) {//remove this dead weak reference
        iterator.remove();
      }
      else if (memberState != state) {
        memberState.set(false);
      }
    }
  }
}
