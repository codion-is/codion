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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.swing.robot;

import is.codion.tools.swing.robot.DefaultAutomation.DefaultBuilder;

import java.awt.GraphicsDevice;
import java.awt.Window;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * An application automation harness.
 */
public interface Automation {

	/**
	 * @return the automation {@link Controller}
	 */
	Controller controller();

	/**
	 * @return the {@link Narrator} or an empty Optional if none is available
	 */
	Optional<Narrator> narrator();

	/**
	 * @param script the script to run
	 */
	void run(Consumer<Automation> script);

	/**
	 * Closes this {@link Automation} and release any resources
	 */
	void close();

	/**
	 * @return a new {@link Builder}
	 */
	static Builder builder() {
		return new DefaultBuilder();
	}

	/**
	 * Builds an {@link Automation} instance
	 */
	interface Builder {

		/**
		 * @param device the graphics device to use when running scripts
		 * @return this builder
		 */
		Builder device(GraphicsDevice device);

		/**
		 * Include a narrator
		 * @param applicationWindow the application window to attach the narrator to
		 * @return this builder
		 */
		Builder narrator(Window applicationWindow);

		/**
		 * Builds and runs the given script
		 * @param script the script
		 */
		void run(Consumer<Automation> script);

		/**
		 * @return a new {@link Automation}
		 */
		Automation build();
	}
}
