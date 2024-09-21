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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.control.CommandControl.CommandControlBuilder;
import is.codion.swing.common.ui.control.ToggleControl.ToggleControlBuilder;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A beefed up Action.
 * @see #command(Command)
 * @see #action(ActionCommand)
 * @see #toggle(Value)
 * @see #toggle(State)
 * @see #builder()
 */
public interface Control extends Action {

	/**
	 * @return the description or an empty Optional if none is available
	 */
	Optional<String> description();

	/**
	 * @return the name or an empty Optional if none is available
	 */
	Optional<String> name();

	/**
	 * @return a {@link StateObserver} indicating whether this Control is enabled
	 */
	StateObserver enabled();

	/**
	 * @return the mnemonic or an empty Optional if none is available
	 */
	OptionalInt mnemonic();

	/**
	 * @return the KeyStroke associated with this Control or an empty Optional if none is available
	 */
	Optional<KeyStroke> keyStroke();

	/**
	 * @return the small icon or an empty Optional if none is available
	 */
	Optional<Icon> smallIcon();

	/**
	 * @return the large icon or an empty Optional if none is available
	 */
	Optional<Icon> largeIcon();

	/**
	 * @return the background color or an empty Optional if none is available
	 */
	Optional<Color> background();

	/**
	 * @return the foreground color or an empty Optional if none is available
	 */
	Optional<Color> foreground();

	/**
	 * @return the font or an empty Optional if none is available
	 */
	Optional<Font> font();

	/**
	 * @return the keys for values that have been set for this control
	 * @see AbstractAction#getKeys()
	 */
	Collection<String> keys();

	/**
	 * Unsupported, the enabled state of Controls is based on their {@code enabled} state observer
	 * @param enabled the enabled status
	 * @throws UnsupportedOperationException always
	 * @see Builder#enabled(StateObserver)
	 */
	@Override
	void setEnabled(boolean enabled);

	/**
	 * Returns a new {@link Control.Builder} instance, based on a copy of this control.
	 * @param <B> the builder type
	 * @return a new builder
	 */
	<C extends Control, B extends Builder<C, B>> Builder<C, B> copy();

	/**
	 * A command interface, allowing Controls based on method references
	 */
	interface Command {

		/**
		 * Executes the command task.
		 * @throws Exception in case of an exception
		 */
		void execute() throws Exception;
	}

	/**
	 * A command interface, allowing Controls based on {@link ActionEvent}s.
	 */
	interface ActionCommand {

		/**
		 * Executes the command task.
		 * @param actionEvent the action event
		 * @throws Exception in case of an exception
		 */
		void execute(ActionEvent actionEvent) throws Exception;
	}

	/**
	 * Creates a control based on a {@link Control.Command}
	 * @param command the {@link Control.Command} on which to base the control
	 * @return a Control for calling the given {@link Control.Command}
	 */
	static CommandControl command(Command command) {
		return builder().command(command).build();
	}

	/**
	 * Creates a control based on a {@link Control.ActionCommand}
	 * @param actionCommand the {@link Control.ActionCommand} on which to base the control
	 * @return a Control for calling the given {@link Control.Command}
	 */
	static CommandControl action(ActionCommand actionCommand) {
		return builder().action(actionCommand).build();
	}

	/**
	 * Creates a new ToggleControl based on the given value
	 * @param value the value
	 * @return a new ToggleControl
	 */
	static ToggleControl toggle(Value<Boolean> value) {
		return builder().toggle(value).build();
	}

	/**
	 * Creates a new ToggleControl based on the given state
	 * @param state the state
	 * @return a new ToggleControl
	 */
	static ToggleControl toggle(State state) {
		return builder().toggle(state).build();
	}

	/**
	 * @return a new Control {@link BuilderFactory} instance
	 */
	static BuilderFactory builder() {
		return new DefaultControlBuilderFactory();
	}

	/**
	 * Provides control builders.
	 */
	interface BuilderFactory {

		/**
		 * @param command the command to execute
		 * @return a new {@link CommandControlBuilder} instance
		 */
		CommandControlBuilder command(Command command);

		/**
		 * @param actionCommand the action command to execute
		 * @return a new {@link CommandControlBuilder} instance
		 */
		CommandControlBuilder action(ActionCommand actionCommand);

		/**
		 * @param value the value to toggle
		 * @return a new {@link ToggleControlBuilder} instance
		 */
		ToggleControlBuilder toggle(Value<Boolean> value);

		/**
		 * @param state the state to toggle
		 * @return a new {@link ToggleControlBuilder} instance
		 */
		ToggleControlBuilder toggle(State state);
	}

	/**
	 * A builder for Control
	 * @param <C> the Control type
	 * @param <B> the builder type
	 */
	interface Builder<C extends Control, B extends Builder<C, B>> {

		/**
		 * @param name the name of the control
		 * @return this Builder instance
		 */
		B name(String name);

		/**
		 * @param enabled the state observer which controls the enabled state of the control
		 * @return this Builder instance
		 */
		B enabled(StateObserver enabled);

		/**
		 * @param mnemonic the control mnemonic
		 * @return this Builder instance
		 */
		B mnemonic(int mnemonic);

		/**
		 * @param smallIcon the small control icon
		 * @return this Builder instance
		 */
		B smallIcon(Icon smallIcon);

		/**
		 * @param largeIcon the large control icon
		 * @return this Builder instance
		 */
		B largeIcon(Icon largeIcon);

		/**
		 * @param description a string describing the control
		 * @return this Builder instance
		 */
		B description(String description);

		/**
		 * @param foreground the foreground color
		 * @return this Builder instance
		 */
		B foreground(Color foreground);

		/**
		 * @param background the background color
		 * @return this Builder instance
		 */
		B background(Color background);

		/**
		 * @param font the font
		 * @return this Builder instance
		 */
		B font(Font font);

		/**
		 * @param keyStroke the keystroke to associate with the control
		 * @return this Builder instance
		 */
		B keyStroke(KeyStroke keyStroke);

		/**
		 * Note that any values added will overwrite the property, if already present,
		 * i.e. setting the 'SmallIcon' value via this method will overwrite the one set
		 * via {@link #smallIcon(Icon)}.
		 * @param key the key
		 * @param value the value
		 * @return this builder
		 * @see Action#putValue(String, Object)
		 */
		B value(String key, Object value);

		/**
		 * @return a new Control instance
		 * @throws IllegalStateException in case no command has been set
		 */
		C build();
	}
}
