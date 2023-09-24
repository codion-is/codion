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
 * Manual demo.
 */
module is.codion.framework.demos.manual {
  requires java.desktop;
  requires jasperreports;
  requires is.codion.common.core;
  requires is.codion.dbms.h2database;
  requires is.codion.framework.db.local;
  requires is.codion.framework.db.rmi;
  requires is.codion.framework.db.http;
  requires is.codion.framework.server;
  requires is.codion.framework.servlet;
  requires is.codion.swing.common.ui.tools;
  requires is.codion.swing.common.ui;
  requires is.codion.swing.framework.model;
  requires is.codion.swing.framework.model.tools;
  requires is.codion.swing.framework.ui;
  requires is.codion.plugin.jasperreports;
  requires is.codion.framework.domain.test;
  requires org.junit.jupiter.api;
  requires com.formdev.flatlaf.intellijthemes;

  exports is.codion.framework.demos.manual.store.domain;
  exports is.codion.framework.demos.manual.store.minimal.domain;
  exports is.codion.framework.demos.manual.store.model;
  exports is.codion.framework.demos.manual.store.ui;
}