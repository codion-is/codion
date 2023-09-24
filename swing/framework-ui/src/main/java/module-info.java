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
 * Framework Swing UI classes, such as:<br>
 * <br>
 * {@link is.codion.swing.framework.ui.EntityPanel}<br>
 * {@link is.codion.swing.framework.ui.EntityEditPanel}<br>
 * {@link is.codion.swing.framework.ui.EntityTablePanel}<br>
 * {@link is.codion.swing.framework.ui.EntityApplicationPanel}<br>
 * {@link is.codion.swing.framework.ui.component.EntityComboBox}<br>
 * {@link is.codion.swing.framework.ui.component.EntityComponents}<br>
 * {@link is.codion.swing.framework.ui.icon.FrameworkIcons}<br>
 */
module is.codion.swing.framework.ui {
  requires org.slf4j;
  requires transitive org.kordamp.ikonli.core;
  requires org.kordamp.ikonli.swing;
  requires transitive is.codion.framework.i18n;
  requires transitive is.codion.swing.framework.model;
  requires transitive is.codion.swing.common.ui;

  exports is.codion.swing.framework.ui;
  exports is.codion.swing.framework.ui.component;
  exports is.codion.swing.framework.ui.icon;

  opens is.codion.swing.framework.ui.icon;

  uses is.codion.swing.framework.ui.icon.FrameworkIcons;

  provides is.codion.swing.framework.ui.icon.FrameworkIcons
          with is.codion.swing.framework.ui.icon.DefaultFrameworkIcons;
  provides org.kordamp.ikonli.IkonHandler
          with is.codion.swing.framework.ui.icon.FrameworkIkonHandler;
}