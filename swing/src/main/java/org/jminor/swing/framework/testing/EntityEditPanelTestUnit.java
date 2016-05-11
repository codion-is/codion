/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.testing;

import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

/**
 * A base class for testing a {@link EntityEditPanel}
 */
public abstract class EntityEditPanelTestUnit {

  private final EntityConnectionProvider connectionProvider;
  private final Class<? extends EntityEditPanel> editPanelClass;
  private final String entityID;

  /**
   * Instantiates a new edit panel test unit for the given edit panel class
   * @param editPanelClass the edit panel class
   * @param entityID the entityId
   * @param user the user
   */
  protected EntityEditPanelTestUnit(final Class<? extends EntityEditPanel> editPanelClass,
                                    final String entityID, final User user) {
    Util.rejectNullValue(editPanelClass, "editPanelClass");
    Util.rejectNullValue(entityID, "entityID");
    Util.rejectNullValue(user, "user");
    this.connectionProvider = EntityConnectionProviders.connectionProvider(user, getClass().getName());
    this.editPanelClass = editPanelClass;
    this.entityID = entityID;
  }

  /**
   * @return the entityID of the panel being tested
   */
  public final String getEntityID() {
    return entityID;
  }

  /**
   * Initializes the edit panel using the model returned by {@link #createEditModel()}
   * @throws Exception in case of an exception
   */
  protected final void testInitializePanel() throws Exception {
    createEditPanel().initializePanel();
  }

  /**
   * Creates the edit panel for testing
   * @return the edit panel to test
   * @throws Exception in case of an exception
   */
  protected EntityEditPanel createEditPanel() throws Exception {
    return editPanelClass.getConstructor(SwingEntityEditModel.class).newInstance(createEditModel());
  }

  /**
   * @return the edit model to base the edit panel on, by default a default edit model implementation
   */
  protected EntityEditModel createEditModel() {
    return new SwingEntityEditModel(getEntityID(), getConnectionProvider());
  }

  /**
   * @return a connection provider
   */
  protected final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }
}
