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
 * HTTP based database connection classes.<br>
 * <br>
 * {@link is.codion.framework.db.http.HttpEntityConnection}<br>
 * {@link is.codion.framework.db.http.HttpEntityConnectionProvider}<br>
 * @provides is.codion.framework.db.EntityConnectionProvider
 */
module is.codion.framework.db.http {
  requires org.slf4j;
  requires java.net.http;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;
  requires is.codion.framework.db.core;
  requires is.codion.framework.json.domain;
  requires is.codion.framework.json.db;

  exports is.codion.framework.db.http;

  provides is.codion.framework.db.EntityConnectionProvider.Builder
          with is.codion.framework.db.http.DefaultHttpEntityConnectionProviderBuilder;
}