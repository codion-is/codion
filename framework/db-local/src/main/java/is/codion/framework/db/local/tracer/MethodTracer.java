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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local.tracer;

import is.codion.common.logging.MethodTrace;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * This is an internal class not for general usage.
 */
public interface MethodTracer {

	MethodTracer NO_OP = new NoOpMethodTracer();

	void enter(String method);

	void enter(String method, @Nullable Object argument);

	void enter(String method, @Nullable Object... arguments);

	@Nullable MethodTrace exit(String method);

	@Nullable MethodTrace exit(String method, @Nullable Exception exception);

	@Nullable MethodTrace exit(String method, @Nullable Exception exception, @Nullable String exitMessage);

	List<MethodTrace> entries();

	void onTrace(Consumer<MethodTrace> consumer);

	static MethodTracer methodTracer(int maxSize) {
		return new DefaultMethodTracer(maxSize);
	}

	interface Traceable {

		void tracer(MethodTracer tracer);
	}
}
