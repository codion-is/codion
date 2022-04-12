/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.common;

import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.textfield.IntegerField;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

public final class EventStateValue {

  private static void event() {
    // tag::event[]
    Event<String> event = Event.event();

    // an observer handles the listeners for an Event but can not trigger it
    EventObserver<String> eventObserver = event.getObserver();

    // add a listener notified each time the event occurs
    eventObserver.addListener(() -> System.out.println("Event occurred"));

    event.onEvent();//output: 'Event occurred'

    // data can be propagated by adding a EventDataListener
    eventObserver.addDataListener(data -> System.out.println("Event: " + data));

    event.onEvent("info");//output: 'Event: info'

    // Event extends EventObserver so listeneres can be added
    // directly without referring to the EventObserver
    event.addListener(() -> System.out.println("Event"));
    // end::event[]
  }

  private static void state() {
    // tag::state[]
    // a boolean state, false by default
    State state = State.state();

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
  }

  private static void action() {
    // tag::action[]
    State state = State.state();

    Action action = new AbstractAction("action") {
      public void actionPerformed(ActionEvent e) {}
    };

    Utilities.linkToEnabledState(state, action);

    System.out.println(action.isEnabled());// output: false

    state.set(true);

    System.out.println(action.isEnabled());// output: true
    // end::action[]
  }

  private static void value() {
    // tag::value[]
    Value<Integer> value = Value.value();

    value.addDataListener(System.out::println);

    value.set(2);

    IntegerField integerField =
            Components.integerField(value)
                    .build();

    integerField.setInteger(3);//linked value is now 3
    // end::value[]
  }

  public static void main(String[] args) {
    event();
    state();
    action();
  }
}
