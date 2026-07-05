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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.worker;

import java.util.ServiceLoader;
import java.util.concurrent.Executor;

/**
 * <p>Resolves the platform's UI thread, for running {@link ProgressWorker} handlers and for
 * determining whether a model refresh should run asynchronously.
 * <p>The implementation is provided via {@link ServiceLoader}, defaulting to {@link #SYNCHRONOUS}
 * same-thread dispatch when no implementation is available.
 * @see #instance()
 */
public interface Dispatcher {

	/**
	 * A synchronous {@link Dispatcher}, running tasks on the calling thread and reporting no UI thread.
	 * <p>Used when no implementation is available, and useful for testing.
	 */
	Dispatcher SYNCHRONOUS = new Dispatcher() {
		@Override
		public Executor executor() {
			return Runnable::run;
		}

		@Override
		public boolean isUserInterfaceThread() {
			return false;
		}
	};

	/**
	 * <p>Returns an {@link Executor} running tasks on the UI thread, resolved for the current context.
	 * <p>Called when a {@link ProgressWorker} is executed, on the UI thread, so that context sensitive
	 * implementations, such as web frameworks providing a per-session UI thread, can bind to the current context.
	 * @return an {@link Executor} running tasks on the UI thread
	 */
	Executor executor();

	/**
	 * @return true if the calling thread is the UI thread
	 */
	boolean isUserInterfaceThread();

	/**
	 * Returns the {@link Dispatcher} implementation, as provided via {@link ServiceLoader},
	 * or {@link #SYNCHRONOUS} in case no implementation is available.
	 * @return the {@link Dispatcher} implementation to use
	 */
	static Dispatcher instance() {
		return DispatcherHolder.INSTANCE;
	}
}
