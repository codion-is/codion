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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
/**
 * Logback implementation of {@link is.codion.common.logging.LoggerProxy}.
 */
module is.codion.plugin.logback.proxy {
  requires org.slf4j;
  requires ch.qos.logback.core;
  requires ch.qos.logback.classic;
  requires is.codion.common.core;

  exports is.codion.plugin.logback;

  provides is.codion.common.logging.LoggerProxy
          with is.codion.plugin.logback.LogbackProxy;
}