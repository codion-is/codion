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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.manual.common;

import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

public final class EventStateValue {

	private static void event() {
		// tag::event[]
		// specify an event propagating
		// a String as the event data
		Event<String> event = Event.event();

		// an observer manages the listeners
		// for an Event but can not trigger it
		EventObserver<String> observer = event.observer();

		// add a listener if you're not
		// interested in the event data
		observer.addListener(() -> System.out.println("Event occurred"));

		event.run();//output: 'Event occurred'

		// or a consumer if you're
		// interested in the event data
		observer.addConsumer(data -> System.out.println("Event: " + data));

		event.accept("info");//output: 'Event: info'

		// Event implements EventObserver so
		// listeneres can be added directly without
		// referring to the EventObserver
		event.addConsumer(System.out::println);
		// end::event[]
	}

	private static void state() {
		// tag::state[]
		// a boolean state, false by default
		State state = State.state();

		// an observer manages the listeners for a State but can not modify it
		StateObserver observer = state.observer();
		// a not observer is always available
		StateObserver not = state.not();

		// add a listener notified each time the state changes
		observer.addListener(() -> System.out.println("State changed"));

		state.set(true);//output: 'State changed'

		observer.addConsumer(value -> System.out.println("State: " + value));

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

		CommandControl control = Control.builder()
						.command(() -> System.out.println("Hello Control"))
						.enabled(state)
						.build();

		System.out.println(control.isEnabled());// output: false

		state.set(true);

		System.out.println(control.isEnabled());// output: true
		// end::control[]
	}

	private static void value() {
		// tag::value[]
		// a nullable value with 2 as the initial value
		Value<Integer> value =
						Value.value(2);

		value.set(4);

		// a non-null value using 0 as null
		Value<Integer> otherValue =
						Value.builder()
										.nonNull(0)
										// linked to the value above
										.link(value)
										.build();

		System.out.println(otherValue.get());// output: 4

		otherValue.set(3);

		System.out.println(value.get());// output: 3

		value.addConsumer(System.out::println);

		otherValue.addListener(() ->
						System.out.println("Value changed: " + otherValue.get()));
		// end::value[]
	}

	private static void nullValue() {
		// tag::nullValue[]
		Integer initialValue = 42;
		Integer nullValue = 0;

		Value<Integer> value =
						Value.builder()
										.nonNull(nullValue)
										.initialValue(initialValue)
										.build();

		System.out.println(value.nullable());//output: false

		System.out.println(value.get());// output: 42

		value.clear();

		System.out.println(value.get());//output: 0
		// end::nullValue[]
	}

	// tag::observers[]
	private static final class Counter {

		private final State valueNull = State.state(true);
		private final Value<Integer> value = Value.builder()
						.<Integer>nullable()
						.consumer(integer -> valueNull.set(integer == null))
						.build();

		/**
		 * @return an event observer notified each time the value changes
		 */
		public EventObserver<Integer> valueChangeEvent() {
			return value.observer();
		}

		/**
		 * @return a state observer indicating whether the value is null
		 */
		public StateObserver valueNullState() {
			return valueNull.observer();
		}
	}
	// end::observers[]

	public static void main(String[] args) {
		event();
		state();
		action();
		control();
		value();
		nullValue();
	}
}
