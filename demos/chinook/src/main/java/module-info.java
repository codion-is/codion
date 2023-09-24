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
 * Chinook demo.
 */
module is.codion.framework.demos.chinook {
  requires is.codion.common.rmi;
  requires is.codion.framework.db.local;
  requires is.codion.framework.db.http;
  requires is.codion.framework.db.rmi;
  requires is.codion.swing.common.ui.tools;
  requires is.codion.swing.framework.model.tools;
  requires is.codion.swing.framework.ui;
  requires is.codion.plugin.jasperreports;
  requires is.codion.plugin.imagepanel;
  requires jasperreports;
  requires com.formdev.flatlaf.intellijthemes;

  exports is.codion.framework.demos.chinook.model
          to is.codion.swing.framework.model, is.codion.swing.framework.ui;
  exports is.codion.framework.demos.chinook.ui
          to is.codion.swing.framework.ui;
  exports is.codion.framework.demos.chinook.tutorial
          to is.codion.framework.db.local;
  //for loading of reports from classpath, accessing default methods in EntityType interfaces and resource bundles
  opens is.codion.framework.demos.chinook.domain;

  provides is.codion.framework.domain.Domain
          with is.codion.framework.demos.chinook.domain.impl.ChinookImpl;
  provides is.codion.common.rmi.server.LoginProxy
          with is.codion.framework.demos.chinook.server.ChinookLoginProxy;
}