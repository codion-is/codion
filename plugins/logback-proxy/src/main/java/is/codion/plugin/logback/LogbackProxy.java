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
package is.codion.plugin.logback;

import is.codion.common.logging.LoggerProxy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static java.util.Spliterators.spliteratorUnknownSize;

/**
 * A Logback LoggerProxy implementation
 */
public final class LogbackProxy implements LoggerProxy {

  @Override
  public Object getLogLevel() {
    return ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).getLevel();
  }

  @Override
  public void setLogLevel(Object logLevel) {
    if (!(logLevel instanceof Level)) {
      throw new IllegalArgumentException("logLevel should be of type " + Level.class.getName());
    }
    ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel((Level) logLevel);
  }

  @Override
  public List<Object> levels() {
    return asList(Level.OFF, Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR);
  }

  @Override
  public Collection<String> files() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    return context.getLoggerList().stream()
            .flatMap(LogbackProxy::appenders)
            .filter(FileAppender.class::isInstance)
            .map(FileAppender.class::cast)
            .map(FileAppender::getFile)
            .collect(Collectors.toList());
  }

  private static Stream<Appender<ILoggingEvent>> appenders(Logger logger) {
    return StreamSupport.stream(spliteratorUnknownSize(logger.iteratorForAppenders(), 0), false);
  }
}
