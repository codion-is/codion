/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.common;

import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.control.Control;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

public final class EventStateValue {

  private static void event() {
    // tag::event[]
    Event<String> event = Event.event();

    // an observer handles the listeners for an Event but can not trigger it
    EventObserver<String> observer = event.observer();

    // add a listener notified each time the event occurs
    observer.addListener(() -> System.out.println("Event occurred"));

    event.run();//output: 'Event occurred'

    // data can be propagated by adding a Consumer as listener
    observer.addDataListener(data -> System.out.println("Event: " + data));

    event.accept("info");//output: 'Event: info'

    // Event implements EventObserver so listeneres can be added
    // directly without referring to the EventObserver
    event.addListener(() -> System.out.println("Event"));
    // end::event[]
  }

  private static void state() {
    // tag::state[]
    // a boolean state, false by default
    State state = State.state();

    // an observer handles the listeners for a State but can not modify it
    StateObserver observer = state.observer();
    // a not observer is always available
    StateObserver not = state.not();

    // add a listener notified each time the state changes
    observer.addListener(() -> System.out.println("State changed"));

    state.set(true);//output: 'State changed'

    observer.addDataListener(value -> System.out.println("State: " + value));

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
      @Override
      public void actionPerformed(ActionEvent e) {
        System.out.println("Hello Action");
      }
    };

    Utilities.linkToEnabledState(state, action);

    System.out.println(action.isEnabled());// output: false

    state.set(true);

    System.out.println(action.isEnabled());// output: true
    // end::action[]
  }

  private static void control() {
    // tag::control[]
    State state = State.state();

    Control control = Control.builder(() ->
                    System.out.println("Hello Control"))
            .enabled(state)
            .build();

    System.out.println(control.isEnabled());// output: false

    state.set(true);

    System.out.println(control.isEnabled());// output: true
    // end::control[]
  }

  private static void value() {
    // tag::value[]
    Value<Integer> value = Value.value();

    value.set(2);

    Value<Integer> otherValue = Value.value();

    otherValue.link(value);

    System.out.println(otherValue.get());// output: 2

    otherValue.set(3);

    System.out.println(value.get());// output: 3

    value.addDataListener(System.out::println);
    // end::value[]
  }

  private static void nullValue() {
    // tag::nullValue[]
    Integer initialValue = 42;
    Integer nullValue = 0;

    Value<Integer> value = Value.value(initialValue, nullValue);

    System.out.println(value.nullable());//output: false

    System.out.println(value.get());// output: 42

    value.set(null);

    System.out.println(value.get());//output: 0
    // end::nullValue[]
  }

  public static void main(String[] args) {
    event();
    state();
    action();
    control();
    value();
    nullValue();
  }
}
