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
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common.logging;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * A logging proxy facilitating the setting of log levels
 */
public interface LoggerProxy {

  /**
   * The null logger proxy instance, zero functionality.
   */
  LoggerProxy NULL_PROXY = new LoggerProxy() {

    private static final String NO_LOG_LEVEL = "NULL";

    @Override
    public Object getLogLevel() {
      return NO_LOG_LEVEL;
    }

    @Override
    public void setLogLevel(Object logLevel) {/*no op*/}

    @Override
    public List<Object> logLevels() {
      return Collections.emptyList();
    }
  };

  /**
   * @return the current log level
   */
  Object getLogLevel();

  /**
   * Sets the log level
   * @param logLevel the log level
   */
  void setLogLevel(Object logLevel);

  /**
   * @return the available log levels
   */
  List<Object> logLevels();

  /**
   * @return the first available LoggerProxy implementation found, {@link #NULL_PROXY} if none is available.
   */
  static LoggerProxy instance() {
    ServiceLoader<LoggerProxy> loader = ServiceLoader.load(LoggerProxy.class);
    Iterator<LoggerProxy> proxyIterator = loader.iterator();
    if (proxyIterator.hasNext()) {
      return proxyIterator.next();
    }

    System.err.println("No LoggerProxy service implementation found");
    return NULL_PROXY;
  }
}
