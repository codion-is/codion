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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.tools.swing.robot;

import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.tools.swing.robot.Controller.FocusLostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.GraphicsDevice;
import java.awt.Window;
import java.util.Optional;
import java.util.function.Consumer;

import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import static java.util.Objects.requireNonNull;

final class DefaultAutomation implements Automation {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultAutomation.class);

	private final Controller controller;
	private final Narrator narrator;

	private DefaultAutomation(DefaultBuilder builder) {
		this.controller = Controller.controller(builder.device);
		if (builder.applicationWindow != null) {
			this.narrator = new Narrator(controller, builder.applicationWindow);
		}
		else {
			this.narrator = null;
		}
	}

	@Override
	public Controller controller() {
		return controller;
	}

	@Override
	public Optional<Narrator> narrator() {
		return Optional.ofNullable(narrator);
	}

	@Override
	public void run(Consumer<Automation> script) {
		requireNonNull(script);
		ProgressWorker.builder()
						.task(() -> script.accept(this))
						.onDone(this::close)
						.onException(DefaultAutomation::handleException)
						.execute();
	}

	@Override
	public void close() {
		if (narrator != null) {
			narrator.close();
		}
	}

	private static void handleException(Exception exception) {
		if (exception instanceof FocusLostException) {
			LOG.debug("Automation terminated due to losing input focus");
		}
		else {
			LOG.error(exception.getMessage(), exception);
		}
	}

	static final class DefaultBuilder implements Builder {

		private GraphicsDevice device = getLocalGraphicsEnvironment().getDefaultScreenDevice();
		private Window applicationWindow;

		@Override
		public Builder device(GraphicsDevice device) {
			this.device = requireNonNull(device);
			return this;
		}

		@Override
		public Builder narrator(Window applicationWindow) {
			this.applicationWindow = requireNonNull(applicationWindow);
			return this;
		}

		@Override
		public void run(Consumer<Automation> script) {
			build().run(script);
		}

		@Override
		public Automation build() {
			return new DefaultAutomation(this);
		}
	}
}
