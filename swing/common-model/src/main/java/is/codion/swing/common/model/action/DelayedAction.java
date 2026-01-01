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

import static java.util.Objects.requireNonNull;

/**
 * Represents an action that executes after a specified delay, with the ability to cancel it before execution.
 * <p>
 * This interface is primarily used to prevent UI flicker when showing transient progress indicators.
 * For example, when a refresh operation might complete quickly, delaying the display of a progress bar
 * by a few hundred milliseconds prevents flickering when the operation finishes almost immediately.
 * <p>
 * The action is scheduled on the Event Dispatch Thread using a Swing Timer and executes exactly once
 * if not cancelled. If the operation completes before the delay expires, calling {@link #cancel()}
 * prevents the action from executing.
 * <p>
 * Example usage:
 * <pre>{@code
 * DelayedAction showProgress = delayedAction(300, () -> {
 *     progressBar.setVisible(true);
 * });
 *
 * // Later, if operation completes quickly:
 * showProgress.cancel();
 * }</pre>
 * @see #delayedAction(int, Runnable)
 */
public interface DelayedAction {

	/**
	 * Cancels the delayed action, preventing it from executing if the delay has not yet elapsed.
	 * If the action has already executed, this method has no effect.
	 * This method is safe to call multiple times.
	 */
	void cancel();

	/**
	 * Creates a new delayed action that executes the given action after the specified delay.
	 * The action is scheduled on the Event Dispatch Thread and executes exactly once if not cancelled.
	 * @param delay the delay in milliseconds before the action executes
	 * @param action the action to execute after the delay
	 * @return a new {@link DelayedAction} instance that can be cancelled
	 * @throws NullPointerException if action is null
	 */
	static DelayedAction delayedAction(int delay, Runnable action) {
		return new DefaultDelayedAction(delay, requireNonNull(action));
	}
}
