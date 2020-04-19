/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui.test;

import org.jminor.common.user.User;
import org.jminor.framework.model.EntityApplicationModel;
import org.jminor.swing.framework.ui.EntityApplicationPanel;

import static java.util.Objects.requireNonNull;

/**
 * A class for testing {@link EntityApplicationPanel} classes
 */
public class EntityApplicationPanelTestUnit {

  private final Class<? extends EntityApplicationPanel> panelClass;
  private final User user;

  /**
   * Instantiates a new entity application panel test unit
   * @param panelClass the panel class
   * @param user the user
   */
  protected EntityApplicationPanelTestUnit(final Class<? extends EntityApplicationPanel> panelClass, final User user) {
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
    final EntityApplicationPanel panel = createApplicationPanel();
    panel.startApplication(null, null, false, null, null, false, user);
    panel.getModel().getConnectionProvider().disconnect();
  }

  /**
   * Creates the panel to test
   * @return the panel to test
   * @throws Exception in case of an exception
   */
  protected EntityApplicationPanel createApplicationPanel() throws Exception {
    return panelClass.getConstructor().newInstance();
  }
}
