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
 * Common Swing UI tools.<br>
 * <br>
 * {@link is.codion.swing.common.ui.tools.loadtest.LoadTestPanel}<br>
 */
module is.codion.swing.common.ui.tools {
  requires org.jfree.jfreechart;
  requires com.formdev.flatlaf.intellijthemes;
  requires transitive is.codion.swing.common.model.tools;
  requires transitive is.codion.swing.common.ui;

  exports is.codion.swing.common.ui.tools.loadtest;
  exports is.codion.swing.common.ui.tools.randomizer;
}