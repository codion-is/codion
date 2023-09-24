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
 * Common Swing model tools, such as:<br>
 * <br>
 * {@link is.codion.swing.common.model.tools.loadtest.LoadTestModel}<br>
 * {@link is.codion.swing.common.model.tools.loadtest.UsageScenario}<br>
 */
module is.codion.swing.common.model.tools {
  requires org.slf4j;
  requires org.jfree.jfreechart;
  requires jdk.management;
  requires transitive java.desktop;
  requires transitive is.codion.common.db;
  requires transitive is.codion.common.model;
  requires transitive is.codion.swing.common.model;

  exports is.codion.swing.common.model.tools.loadtest;
  exports is.codion.swing.common.model.tools.randomizer;
}