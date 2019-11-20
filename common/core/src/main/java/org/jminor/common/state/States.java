/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.state;

import org.jminor.common.Conjunction;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.EventObserver;
import org.jminor.common.event.Events;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static java.util.Objects.requireNonNull;

/**
 * A factory class for {@link State} objects.
 * @see State
 */
public final class States {

  private States() {}

  /**
   * Instantiates a new 'false' State object.
   * @return a new State
   */
  public static State state() {
    return state(false);
  }

  /**
   * Instantiates a new State object.
   * @param value the initial state value
   * @return a new State
   */
  public static State state(final boolean value) {
    return new DefaultState(value);
  }

  /**
   * Instantiates a new State.AggregateState object.
   * @param conjunction the conjunction to use
   * @param stateObservers the state observers to base this aggregate state on
   * @return a new State.AggregateState
   */
  public static State.AggregateState aggregateState(final Conjunction conjunction, final StateObserver... stateObservers) {
    return new DefaultAggregateState(conjunction, stateObservers);
  }

  /**
   * Instantiates a new State.Group object, which guarantees that only a single
   * state within the group is active at a time
   * @param states the states to add to the group initially, not required
   * @return a new State.Group
   * @see State.Group
   */
  public static State.Group group(final State... states) {
    return new DefaultGroup(states);
  }

  private static class DefaultState implements State {

    private final Object lock = new Object();
    private StateObserver observer;
    private boolean value;

    DefaultState() {
      this(false);
    }

    DefaultState(final boolean value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return Boolean.toString(value);
    }

    @Override
    public void set(final boolean value) {
      synchronized (lock) {
        if (this.value != value) {
          final boolean previousValue = this.value;
          this.value = value;
          ((DefaultStateObserver) getObserver()).notifyObservers(previousValue, value);
        }
      }
    }

    @Override
    public boolean get() {
      synchronized (lock) {
        return value;
      }
    }

    @Override
    public final StateObserver getObserver() {
      synchronized (lock) {
        if (observer == null) {
          observer = new DefaultStateObserver(this, false);
        }

        return observer;
      }
    }

    @Override
    public final void addListener(final EventListener listener) {
      getObserver().addListener(listener);
    }

    @Override
    public final void removeListener(final EventListener listener) {
      getObserver().removeListener(listener);
    }

    @Override
    public void addDataListener(final EventDataListener<Boolean> listener) {
      getObserver().addDataListener(listener);
    }

    @Override
    public void removeDataListener(final EventDataListener listener) {
      getObserver().removeDataListener(listener);
    }

    @Override
    public final StateObserver getReversedObserver() {
      return getObserver().getReversedObserver();
    }
  }

  private static final class DefaultAggregateState extends DefaultState implements State.AggregateState {

    private final Object lock = new Object();
    private final List<AggregateStateListener> stateListeners = new ArrayList<>();
    private final Conjunction conjunction;

    private DefaultAggregateState(final Conjunction conjunction) {
      this.conjunction = requireNonNull(conjunction, "conjunction");
    }

    private DefaultAggregateState(final Conjunction conjunction, final StateObserver... states) {
      this(conjunction);
      if (states != null) {
        for (final StateObserver state : states) {
          addState(state);
        }
      }
    }

    @Override
    public String toString() {
      synchronized (lock) {
        final StringBuilder stringBuilder = new StringBuilder("Aggregate");
        stringBuilder.append(toString(conjunction)).append(super.toString());
        for (final AggregateStateListener listener : stateListeners) {
          stringBuilder.append(", ").append(listener.getState());
        }

        return stringBuilder.toString();
      }
    }

    @Override
    public Conjunction getConjunction() {
      return conjunction;
    }

    @Override
    public void addState(final StateObserver state) {
      requireNonNull(state, "state");
      synchronized (lock) {
        if (findListener(state) == null) {
          final boolean value = get();
          stateListeners.add(new AggregateStateListener(state));
          ((DefaultStateObserver) getObserver()).notifyObservers(value, get());
        }
      }
    }

    @Override
    public void removeState(final StateObserver state) {
      requireNonNull(state, "state");
      synchronized (lock) {
        final boolean value = get();
        final AggregateStateListener listener = findListener(state);
        if (listener != null) {
          state.removeDataListener(listener);
          stateListeners.remove(listener);
          ((DefaultStateObserver) getObserver()).notifyObservers(value, get());
        }
      }
    }

    @Override
    public boolean get() {
      synchronized (lock) {
        return get(conjunction, null, false);
      }
    }

    @Override
    public void set(final boolean value) {
      throw new UnsupportedOperationException("The state of aggregate states can't be set");
    }

    private boolean get(final Conjunction conjunction, final StateObserver exclude, final boolean excludeReplacement) {
      for (final AggregateStateListener listener : stateListeners) {
        final StateObserver state = listener.getState();
        final boolean value = state.equals(exclude) ? excludeReplacement : state.get();
        if (conjunction == Conjunction.AND) {
          if (!value) {
            return false;
          }
        }
        else if (value) {
          return true;
        }
      }

      return conjunction == Conjunction.AND;
    }

    private AggregateStateListener findListener(final StateObserver state) {
      return stateListeners.stream().filter(listener -> listener.getState().equals(state)).findFirst().orElse(null);
    }

    private final class AggregateStateListener implements EventDataListener<Boolean> {
      private final StateObserver state;

      private AggregateStateListener(final StateObserver state) {
        this.state = state;
        this.state.addDataListener(this);
      }

      @Override
      public void eventOccurred(final Boolean newValue) {
        ((DefaultStateObserver) getObserver()).notifyObservers(getPreviousState(state, !newValue), get());
      }

      private boolean getPreviousState(final StateObserver excludeState, final boolean previousValue) {
        synchronized (lock) {
          return get(conjunction, excludeState, previousValue);
        }
      }

      private StateObserver getState() {
        return state;
      }
    }

    private static String toString(final Conjunction conjunction) {
      switch (conjunction) {
        case AND: return " and ";
        case OR: return " or ";
        default: throw new IllegalArgumentException("Unknown conjunction: " + conjunction);
      }
    }
  }

  private static final class DefaultStateObserver implements StateObserver {

    private final Object lock = new Object();
    private final StateObserver stateObserver;
    private final boolean reversed;

    private Event<Boolean> stateChangedEvent;
    private DefaultStateObserver reversedStateObserver;

    private DefaultStateObserver(final StateObserver stateObserver, final boolean reversed) {
      this.stateObserver = stateObserver;
      this.reversed = reversed;
    }

    @Override
    public boolean get() {
      synchronized (lock) {
        return reversed ? !stateObserver.get() : stateObserver.get();
      }
    }

    @Override
    public StateObserver getReversedObserver() {
      synchronized (lock) {
        if (reversedStateObserver == null) {
          reversedStateObserver = new DefaultStateObserver(this, true);
        }

        return reversedStateObserver;
      }
    }

    @Override
    public void addListener(final EventListener listener) {
      getEventObserver().addListener(listener);
    }

    @Override
    public void removeListener(final EventListener listener) {
      getEventObserver().removeListener(listener);
    }

    @Override
    public void addDataListener(final EventDataListener<Boolean> listener) {
      getEventObserver().addDataListener(listener);
    }

    @Override
    public void removeDataListener(final EventDataListener listener) {
      getEventObserver().removeDataListener(listener);
    }

    private EventObserver<Boolean> getEventObserver() {
      synchronized (lock) {
        if (stateChangedEvent == null) {
          stateChangedEvent = Events.event();
        }

        return stateChangedEvent.getObserver();
      }
    }

    private void notifyObservers(final boolean previousValue, final boolean newValue) {
      synchronized (lock) {
        if (previousValue != newValue) {
          if (stateChangedEvent != null) {
            stateChangedEvent.fire(newValue);
          }
          if (reversedStateObserver != null) {
            reversedStateObserver.notifyObservers(newValue, previousValue);
          }
        }
      }
    }
  }

  private static final class DefaultGroup implements State.Group {

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
}
