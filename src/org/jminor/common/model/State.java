/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import javax.swing.Action;
import java.awt.event.ActionListener;

/**
 * A class encapsulating a simple boolean state, providing a change event.
 */
public interface State {

  void addListeningAction(final Action action);

  /**
   * @param value the new active state of this State instance
   */
  void setActive(final boolean value);

  /**
   * @return true if this state is active, false otherwise
   */
  boolean isActive();

  /**
   * @return a State that is always the same as the parent state but can not be directly modified
   */
  State getLinkedState();

  /**
   * @return A State object that is always the reverse of the parent state
   */
  State getReversedState();

  /**
   * @return an EventObserver notified each time the state changes
   */
  EventObserver stateObserver();

  void addStateListener(final ActionListener listener);

  void removeStateListener(final ActionListener listener);

  void notifyStateObserver();

  interface AggregateState extends State {

      /**
       * The conjunction types used in AggregateState.
       */
      enum Type {AND, OR}

      /**
       * @return the type of this aggregate state
       */
      public Type getType();

      void addState(final State state);

      void removeState(final State state);
    }

    /**
     * A StateGroup deactivates all other states when a state in the group is activated.
     * StateGroup works with WeakReference so adding states does not prevent
     * them from being garbage collected.
     */
    interface StateGroup {

      /**
       * Adds a state to this state group via a WeakReference,
       * so it does not prevent it from being garbage collected.
       * Adding an active state deactivates all other states in the group.
       * @param state the State to add
       */
      void addState(final State state);
    }
  }