/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.loadtest;

import is.codion.swing.common.tools.loadtest.AbstractUsageScenario;
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
