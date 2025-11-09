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
 * Copyright (c) 2017 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.plugin.jul;

import is.codion.common.utilities.logging.LoggerProxy;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A java.util.logging LoggerProxy implementation
 */
public final class JulProxy implements LoggerProxy {

	@Override
	public Object getLogLevel(String name) {
		return LogManager.getLogManager().getLogger(requireNonNull(name)).getLevel();
	}

	@Override
	public void setLogLevel(String name, Object logLevel) {
		if (!(logLevel instanceof Level)) {
			throw new IllegalArgumentException("logLevel should be of type " + Level.class.getName());
		}
		LogManager.getLogManager().getLogger(requireNonNull(name)).setLevel((Level) logLevel);
	}

	@Override
	public List<Object> levels() {
		return asList(Level.ALL, Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST, Level.OFF);
	}

	@Override
	public String rootLogger() {
		return Logger.GLOBAL_LOGGER_NAME;
	}

	@Override
	public Collection<String> loggers() {
		return Collections.list(LogManager.getLogManager().getLoggerNames());
	}
}
