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
package is.codion.framework.db.local.logger;

import is.codion.common.logging.MethodTrace;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * This is an internal class not for general usage.
 */
public interface MethodLogger {

	void enter(String method);

	void enter(String method, @Nullable Object argument);

	@Nullable MethodTrace exit(String method);

	@Nullable MethodTrace exit(String method, @Nullable Exception exception);

	@Nullable MethodTrace exit(String method, @Nullable Exception exception, @Nullable String exitMessage);

	boolean isEnabled();

	void setEnabled(boolean enabled);

	List<MethodTrace> entries();

	static MethodLogger methodLogger(int maxSize, ArgumentFormatter formatter) {
		return new DefaultMethodLogger(maxSize, formatter);
	}

	static MethodLogger noOpLogger() {
		return DefaultMethodLogger.NO_OP;
	}

	interface Loggable {

		void methodLogger(@Nullable MethodLogger methodLogger);
	}

	interface ArgumentFormatter {

		String format(String methodName, @Nullable Object argument);
	}
}
