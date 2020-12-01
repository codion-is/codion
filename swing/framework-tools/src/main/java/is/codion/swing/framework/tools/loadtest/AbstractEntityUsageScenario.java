/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.loadtest;

import is.codion.framework.model.EntityApplicationModel;
import is.codion.swing.common.tools.loadtest.AbstractUsageScenario;

/**
 * An abstract base class for usage scenarios based on EntityApplicationModel instances
 * @param <M> the application model type used by this usage scenario
 */
public abstract class AbstractEntityUsageScenario<M extends EntityApplicationModel> extends AbstractUsageScenario<M> {

  /**
   * Instantiates a new AbstractEntityUsageScenario
   */
  public AbstractEntityUsageScenario() {
    super();
  }

  /**
   * Instantiates a new AbstractEntityUsageScenario
   * @param name the scenario name
   */
  public AbstractEntityUsageScenario(final String name) {
    super(name);
  }
}
