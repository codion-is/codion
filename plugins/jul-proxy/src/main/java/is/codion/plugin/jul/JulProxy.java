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
 * Copyright (c) 2017 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.plugin.jul;

import is.codion.common.utilities.logging.LoggerProxy;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * A java.util.logging LoggerProxy implementation
 */
public final class JulProxy implements LoggerProxy {

	// JUL holds loggers weakly, a level set on a logger nothing else references lost along with it,
	// and LoggerProxy.instance() returns a new proxy each time, so the references outlive any one proxy
	private static final Map<String, Logger> CONFIGURED = new ConcurrentHashMap<>();

	@Override
	public @Nullable Object level(String logger) {
		Logger julLogger = LogManager.getLogManager().getLogger(requireNonNull(logger));

		return julLogger == null ? null : julLogger.getLevel();
	}

	@Override
	public void level(String logger, Object level) {
		if (!(level instanceof Level)) {
			throw new IllegalArgumentException("level should be of type " + Level.class.getName());
		}
		Logger julLogger = Logger.getLogger(requireNonNull(logger));
		julLogger.setLevel((Level) level);
		CONFIGURED.put(logger, julLogger);
	}

	@Override
	public List<Object> levels() {
		return unmodifiableList(asList(Level.ALL, Level.FINEST, Level.FINER, Level.FINE, Level.CONFIG, Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF));
	}

	@Override
	public String root() {
		// the root logger, named by the empty string, the global logger being one of its children
		return "";
	}

	@Override
	public Collection<String> loggers() {
		return unmodifiableList(Collections.list(LogManager.getLogManager().getLoggerNames()));
	}
}
