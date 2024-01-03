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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
/**
 * RMI application server.<br>
 * <br>
 * {@link is.codion.framework.server.EntityServer}<br>
 * {@link is.codion.framework.server.EntityServerAdmin}<br>
 * {@link is.codion.framework.server.EntityServerConfiguration}<br>
 */
module is.codion.framework.server {
  requires org.slf4j;
  requires transitive is.codion.framework.db.local;
  requires transitive is.codion.framework.db.rmi;

  exports is.codion.framework.server;
}