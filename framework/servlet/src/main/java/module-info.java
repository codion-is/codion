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
 * Servlet AuxiliaryServer implementation for EntityServer, based on <a href="https://javalin.io">Javalin</a>.<br>
 * <br>
 * {@link is.codion.framework.servlet.EntityService}<br>
 * {@link is.codion.framework.servlet.EntityServiceFactory}<br>
 * @provides is.codion.common.rmi.server.AuxiliaryServerFactory
 */
module is.codion.framework.servlet {
  requires org.slf4j;
  requires io.javalin;
  requires io.javalin.community.ssl;
  requires is.codion.framework.db.rmi;
  requires is.codion.framework.json.domain;
  requires is.codion.framework.json.db;

  exports is.codion.framework.servlet;

  provides is.codion.common.rmi.server.AuxiliaryServerFactory
          with is.codion.framework.servlet.EntityServiceFactory;
}