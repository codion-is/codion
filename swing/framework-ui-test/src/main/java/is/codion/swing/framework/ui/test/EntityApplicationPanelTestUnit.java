/*
 * Copyright (c) 2013 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.test;

import is.codion.common.user.User;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;

import static java.util.Objects.requireNonNull;

/**
 * A class for testing {@link EntityApplicationPanel} classes
 */
public class EntityApplicationPanelTestUnit {

  private final Class<? extends EntityApplicationPanel<?>> panelClass;
  private final User user;

  /**
   * Instantiates a new entity application panel test unit
   * @param panelClass the panel class
   * @param user the user
   */
  protected EntityApplicationPanelTestUnit(Class<? extends EntityApplicationPanel<?>> panelClass, User user) {
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
    EntityApplicationPanel<?> panel = createApplicationPanel();
    panel.addApplicationStartedListener(frame -> panel.model().connectionProvider().close());
    panel.starter().silentLoginUser(user).displayFrame(false).displayProgressDialog(false).start();
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
