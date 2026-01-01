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
package is.codion.common.utilities.logging;

import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;

final class NoOpLoggerProxy implements LoggerProxy {

	private static final String NO_LOG_LEVEL = "NULL";

	@Override
	public Object getLogLevel(String logger) {
		return NO_LOG_LEVEL;
	}

	@Override
	public void setLogLevel(String logger, Object logLevel) {/*no op*/}

	@Override
	public List<Object> levels() {
		return emptyList();
	}

	@Override
	public String rootLogger() {
		return "";
	}

	@Override
	public Collection<String> loggers() {
		return emptyList();
	}
}
