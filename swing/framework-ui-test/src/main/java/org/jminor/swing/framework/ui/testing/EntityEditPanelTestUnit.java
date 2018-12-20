/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui.testing;

import org.jminor.common.User;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import java.util.Objects;

/**
 * A base class for testing a {@link EntityEditPanel}
 */
public abstract class EntityEditPanelTestUnit {

  private final EntityConnectionProvider connectionProvider;
  private final Class<? extends EntityEditPanel> editPanelClass;
  private final String entityId;

  /**
   * Instantiates a new edit panel test unit for the given edit panel class
   * @param domainclass the domain model class name
   * @param editPanelClass the edit panel class
   * @param entityId the entityId
   * @param user the user
   */
  protected EntityEditPanelTestUnit(final String domainclass, final Class<? extends EntityEditPanel> editPanelClass,
                                    final String entityId, final User user) {
    Objects.requireNonNull(editPanelClass, "editPanelClass");
    Objects.requireNonNull(entityId, "entityId");
    Objects.requireNonNull(user, "user");
    this.connectionProvider = EntityConnectionProviders.connectionProvider(domainclass, getClass().getName()).setUser(user);
    this.editPanelClass = editPanelClass;
    this.entityId = entityId;
  }

  /**
   * @return the entityId of the panel being tested
   */
  public final String getEntityId() {
    return entityId;
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
  protected SwingEntityEditModel createEditModel() {
    return new SwingEntityEditModel(getEntityId(), getConnectionProvider());
  }

  /**
   * @return a connection provider
   */
  protected final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }
}
