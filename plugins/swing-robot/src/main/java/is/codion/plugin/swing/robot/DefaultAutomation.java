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
package is.codion.plugin.swing.robot;

import is.codion.swing.common.model.worker.ProgressWorker;

import java.awt.GraphicsDevice;
import java.awt.Window;
import java.util.Optional;
import java.util.function.Consumer;

import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import static java.util.Objects.requireNonNull;

final class DefaultAutomation implements Automation {

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
						.execute();
	}

	@Override
	public void close() {
		if (narrator != null) {
			narrator.close();
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
