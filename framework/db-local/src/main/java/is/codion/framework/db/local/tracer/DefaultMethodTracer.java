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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local.tracer;

import is.codion.common.logging.MethodTrace;

import org.jspecify.annotations.Nullable;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static is.codion.common.logging.MethodTrace.methodTrace;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultMethodTracer implements MethodTracer {

	private final Deque<MethodTrace> callStack = new LinkedList<>();
	private final LinkedList<MethodTrace> entries = new LinkedList<>();
	private final ArgumentFormatter formatter;
	private final int maxSize;

	private boolean enabled = false;

	DefaultMethodTracer(int maxSize, ArgumentFormatter formatter) {
		this.maxSize = maxSize;
		this.formatter = requireNonNull(formatter);
	}

	@Override
	public synchronized void enter(String method) {
		if (enabled) {
			callStack.push(methodTrace(method, null));
		}
	}

	@Override
	public synchronized void enter(String method, @Nullable Object argument) {
		if (enabled) {
			callStack.push(methodTrace(method, formatter.format(method, argument)));
		}
	}

	@Override
	public synchronized void enter(String method, @Nullable Object... arguments) {
		if (enabled) {
			callStack.push(methodTrace(method, formatter.format(method, arguments)));
		}
	}

	@Override
	public @Nullable MethodTrace exit(String method) {
		return exit(method, null);
	}

	@Override
	public @Nullable MethodTrace exit(String method, @Nullable Exception exception) {
		return exit(method, exception, null);
	}

	@Override
	public synchronized @Nullable MethodTrace exit(String method, @Nullable Exception exception, @Nullable String exitMessage) {
		if (!enabled) {
			return null;
		}
		if (callStack.isEmpty()) {
			throw new IllegalStateException("Call stack is empty when trying to log method exit: " + method);
		}
		MethodTrace entry = callStack.pop();
		if (!entry.method().equals(method)) {
			throw new IllegalStateException("Expecting method " + entry.method() + " but got " + method + " when trying to log method exit");
		}
		entry.complete(exception, exitMessage);
		if (callStack.isEmpty()) {
			if (entries.size() == maxSize) {
				entries.removeFirst();
			}
			entries.addLast(entry);
		}
		else {
			callStack.peek().addChild(entry);
		}

		return entry;
	}

	@Override
	public synchronized boolean isEnabled() {
		return enabled;
	}

	@Override
	public synchronized void setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			if (!enabled) {
				entries.clear();
				callStack.clear();
			}
		}
	}

	@Override
	public synchronized List<MethodTrace> entries() {
		return unmodifiableList(entries);
	}
}
