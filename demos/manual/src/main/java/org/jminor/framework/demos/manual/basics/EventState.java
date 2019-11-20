/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.basics;

import org.jminor.common.event.Event;
import org.jminor.common.event.EventObserver;
import org.jminor.common.event.Events;
import org.jminor.common.state.State;
import org.jminor.common.state.StateObserver;
import org.jminor.common.state.States;
import org.jminor.swing.common.ui.UiUtil;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

public final class EventState {

  public static void main(final String[] args) {
    // tag::event[]
    Event<String> event = Events.event();

    // an observer handles the listeners for an Event but can not trigger it
    EventObserver<String> eventObserver = event.getObserver();

    // add a listener notified each time the event occurs
    eventObserver.addListener(() -> System.out.println("Event occurred"));

    event.fire();//output: 'Event occurred'

    // data can be propagated by adding a EventDataListener
    eventObserver.addDataListener(data -> System.out.println("Event: " + data));

    event.fire("info");//output: 'Event: info'

    // Event extends EventObserver so listeneres can be added
    // directly without referring to the EventObserver
    event.addListener(() -> System.out.println("Event"));
    // end::event[]

    // tag::state[]
    // a boolean state, false by default
    State state = States.state();

    // an observer handles the listeners for a State but can not change it
    StateObserver stateObserver = state.getObserver();
    // a reversed observer is always available
    StateObserver reversedObserver = state.getReversedObserver();

    // add a listener notified each time the state changes
    stateObserver.addListener(() -> System.out.println("State changed"));

    state.set(true);//output: 'State changed'

    stateObserver.addDataListener(value -> System.out.println("State: " + value));

    state.set(false);//output: 'State: false'

    // State extends StateObserver so listeners can be added
    // directly without referring to the StateObserver
    state.addListener(() -> System.out.println("State changed"));
    // end::state[]

    // tag::action[]
    Action action = new AbstractAction("action") {
      public void actionPerformed(ActionEvent e) {}
    };

    UiUtil.linkToEnabledState(state, action);

    System.out.println(action.isEnabled());// output: false

    state.set(true);

    System.out.println(action.isEnabled());// output: true
    // end::action[]
  }
}
