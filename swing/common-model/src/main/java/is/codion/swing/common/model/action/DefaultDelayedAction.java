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
package is.codion.swing.common.model.action;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

final class DefaultDelayedAction implements DelayedAction {

	private final Timer timer;

	DefaultDelayedAction(int delay, Runnable action) {
		this.timer = new Timer(delay, new Performer(action));
		this.timer.setRepeats(false);
		this.timer.start();
	}

	@Override
	public void cancel() {
		timer.stop();
	}

	private static final class Performer implements ActionListener {

		private final Runnable action;

		private Performer(Runnable action) {
			this.action = action;
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			action.run();
		}
	}
}
