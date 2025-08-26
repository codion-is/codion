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
package is.codion.framework.db.local.tracer;

import is.codion.common.logging.MethodTrace;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;

final class NoOpMethodTracer implements MethodTracer {

	@Override
	public void enter(String method) {}

	@Override
	public void enter(String method, @Nullable Object argument) {}

	@Override
	public void enter(String method, @Nullable Object... arguments) {}

	@Override
	public @Nullable MethodTrace exit(String method) {
		return null;
	}

	@Override
	public @Nullable MethodTrace exit(String method, @Nullable Exception exception) {
		return null;
	}

	@Override
	public @Nullable MethodTrace exit(String method, @Nullable Exception exception, @Nullable String exitMessage) {
		return null;
	}

	@Override
	public void onTrace(Consumer<MethodTrace> consumer) {}

	@Override
	public List<MethodTrace> entries() {
		return emptyList();
	}
}
