/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.state;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

final class DefaultGroup implements State.Group {

  private final List<WeakReference<State>> members = new ArrayList<>();

  public DefaultGroup(final State... states) {
    if (states != null) {
      for (final State state : states) {
        addState(state);
      }
    }
  }

  @Override
  public void addState(final State state) {
    synchronized (members) {
      for (final WeakReference<State> reference : members) {
        if (reference.get() == state) {
          return;
        }
      }//no duplicate states

      members.add(new WeakReference<>(state));
    }
    updateAccordingToState(state);
    state.addListener(() -> updateAccordingToState(state));
  }

  private void updateAccordingToState(final State state) {
    synchronized (members) {
      final ListIterator<WeakReference<State>> iterator = members.listIterator();
      while (iterator.hasNext()) {
        final State referredState = iterator.next().get();
        if (referredState == null) {//remove this dead weak reference
          iterator.remove();
        }
        else if (state.get() && !state.equals(referredState)) {
          referredState.set(false);
        }
      }
    }
  }
}
