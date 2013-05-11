package org.jminor.framework.client.ui;

import org.jminor.common.model.User;

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
    this.panelClass = panelClass;
    this.user = user;
  }

  /**
   * Instantiates the panel and initializes it
   * @throws Exception in case of an exception
   */
  protected final void testInitializePanel() throws Exception {
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
