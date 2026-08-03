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
package is.codion.common.utilities.dispatch;

import java.util.ServiceLoader;
import java.util.concurrent.Executor;

/**
 * <p>Provides access to a <i>dispatch context</i>: a confined execution context which results must be
 * handed back to, and which must not be blocked. On UI platforms this is the UI thread, the Swing event
 * dispatch thread or the Android main looper, but nothing here requires a user interface.
 * <p>A context is not necessarily a fixed thread. Implementations may bind one to the calling thread for
 * the duration of a request, as web frameworks providing a per-session context do, which is why
 * {@link #bound()} asks whether a context is bound here rather than which thread is running.
 * <p>The implementation is provided via {@link ServiceLoader}, defaulting to {@link #SYNCHRONOUS} when
 * none is available.
 * @see #instance()
 */
public interface Dispatcher {

	/**
	 * A {@link Dispatcher} without a dispatch context, running tasks on the calling thread.
	 * <p>Used when no implementation is available, and useful for testing.
	 */
	Dispatcher SYNCHRONOUS = new Dispatcher() {
		@Override
		public Executor executor() {
			return Runnable::run;
		}

		@Override
		public boolean bound() {
			return false;
		}
	};

	/**
	 * <p>Returns an {@link Executor} running tasks on the dispatch context, resolved for the caller.
	 * <p>Must be called where a context is bound, see {@link #bound()}, so that implementations binding
	 * a context per request or session resolve the right one.
	 * @return an {@link Executor} running tasks on the dispatch context
	 */
	Executor executor();

	/**
	 * <p>Returns true if a dispatch context is bound to the caller, that is, if this is the context
	 * results must be handed back to and which must not be blocked.
	 * <p>This reports a fact, not a recommendation. Whether to hand work off on the strength of it is
	 * left to the caller, some always do so regardless.
	 * @return true if a dispatch context is bound to the caller
	 */
	boolean bound();

	/**
	 * Returns the {@link Dispatcher} implementation, as provided via {@link ServiceLoader},
	 * or {@link #SYNCHRONOUS} in case no implementation is available.
	 * @return the {@link Dispatcher} implementation to use
	 */
	static Dispatcher instance() {
		return DispatcherHolder.INSTANCE;
	}
}
