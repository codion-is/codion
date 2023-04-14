/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.test;

import is.codion.common.user.User;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;

import static is.codion.swing.framework.ui.EntityApplicationBuilder.entityApplicationBuilder;
import static java.util.Objects.requireNonNull;

/**
 * A class for testing {@link EntityApplicationPanel} classes
 */
public class EntityApplicationPanelTestUnit<M extends SwingEntityApplicationModel> {

  private final Class<M> modelClass;
  private final Class<? extends EntityApplicationPanel<M>> panelClass;
  private final User user;

  /**
   * Instantiates a new entity application panel test unit
   * @param modelClass the application model class
   * @param panelClass the application panel class
   * @param user the user
   */
  protected EntityApplicationPanelTestUnit(Class<M> modelClass, Class<? extends EntityApplicationPanel<M>> panelClass, User user) {
    this.modelClass = requireNonNull(modelClass, "modelClass");
    this.panelClass = requireNonNull(panelClass, "panelClass");
    this.user = requireNonNull(user, "user");
  }

  /**
   * Instantiates the panel and initializes it
   */
  protected final void testInitializePanel() {
    entityApplicationBuilder(modelClass, panelClass)
            .automaticLoginUser(user)
            .saveDefaultUsername(false)
            .setUncaughtExceptionHandler(false)
            .displayStartupDialog(false)
            .displayFrame(false)
            .onApplicationStarted(applicationPanel -> applicationPanel.applicationModel().connectionProvider().close())
            .start();
  }
}
