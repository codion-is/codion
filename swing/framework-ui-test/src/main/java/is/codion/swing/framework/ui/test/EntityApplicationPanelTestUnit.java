/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.test;

import is.codion.common.user.User;
import is.codion.framework.model.EntityApplicationModel;
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
   * @throws Exception in case of an exception
   */
  protected final void testInitializePanel() throws Exception {
    EntityApplicationModel.SAVE_DEFAULT_USERNAME.set(false);
    EntityApplicationPanel.SHOW_STARTUP_DIALOG.set(false);
    entityApplicationBuilder(modelClass, panelClass)
            .panelFactory(applicationModel -> {
              try {
                EntityApplicationPanel<M> applicationPanel = panelClass.getConstructor(modelClass).newInstance(applicationModel);
                applicationPanel.addApplicationStartedListener(frame -> applicationModel.connectionProvider().close());

                return applicationPanel;
              }
              catch (Exception e) {
                throw new RuntimeException(e);
              }
            })
            .automaticLoginUser(user)
            .displayStartupDialog(false)
            .displayFrame(false)
            .start();
  }

  /**
   * Creates the panel to test
   * @return the panel to test
   * @throws Exception in case of an exception
   */
  protected EntityApplicationPanel<?> createApplicationPanel() throws Exception {
    return panelClass.getConstructor().newInstance();
  }
}
