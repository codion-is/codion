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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.tools.loadtest;

import is.codion.swing.common.model.tools.loadtest.AbstractUsageScenario;
import is.codion.swing.framework.model.SwingEntityApplicationModel;

/**
 * An abstract base class for usage scenarios based on SwingEntityApplicationModel instances
 * @param <M> the application model type used by this usage scenario
 */
public abstract class AbstractEntityUsageScenario<M extends SwingEntityApplicationModel> extends AbstractUsageScenario<M> {

  /**
   * Instantiates a new AbstractEntityUsageScenario
   */
  protected AbstractEntityUsageScenario() {
    super();
  }

  /**
   * Instantiates a new AbstractEntityUsageScenario
   * @param name the scenario name
   */
  protected AbstractEntityUsageScenario(String name) {
    super(name);
  }
}
