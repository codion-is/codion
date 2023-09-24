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
 * Framework Swing model classes, such as:<br>
 * <br>
 * {@link is.codion.swing.framework.model.SwingEntityModel}<br>
 * {@link is.codion.swing.framework.model.SwingEntityEditModel}<br>
 * {@link is.codion.swing.framework.model.SwingEntityTableModel}<br>
 * {@link is.codion.swing.framework.model.SwingEntityApplicationModel}<br>
 * {@link is.codion.swing.framework.model.component.EntityComboBoxModel}<br>
 */
module is.codion.swing.framework.model {
  requires org.slf4j;
  requires org.json;
  requires transitive is.codion.framework.model;
  requires transitive is.codion.swing.common.model;

  exports is.codion.swing.framework.model;
  exports is.codion.swing.framework.model.component;
}