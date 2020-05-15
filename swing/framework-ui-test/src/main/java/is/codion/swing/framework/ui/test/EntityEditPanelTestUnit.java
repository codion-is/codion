/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.ui.test;

import dev.codion.swing.framework.model.SwingEntityEditModel;
import dev.codion.swing.framework.ui.EntityEditPanel;

import static java.util.Objects.requireNonNull;

/**
 * A base class for testing a {@link EntityEditPanel}
 */
public class EntityEditPanelTestUnit {

  private final SwingEntityEditModel editModel;
  private final Class<? extends EntityEditPanel> editPanelClass;

  /**
   * Instantiates a new edit panel test unit for the given edit panel class
   * @param editModel the edit model
   * @param editPanelClass the edit panel class
   */
  protected EntityEditPanelTestUnit(final SwingEntityEditModel editModel,
                                    final Class<? extends EntityEditPanel> editPanelClass) {
    this.editModel = requireNonNull(editModel, "editModel");
    this.editPanelClass = requireNonNull(editPanelClass, "editPanelClass");
  }

  /**
   * Initializes the edit panel and calls {@link EntityEditPanel#createHorizontalControlPanel()}
   * @throws Exception in case of an exception
   */
  protected final void testInitializePanel() throws Exception {
    createEditPanel().initializePanel().createHorizontalControlPanel();
  }

  /**
   * Creates the edit panel for testing
   * @return the edit panel to test
   * @throws Exception in case of an exception
   */
  protected EntityEditPanel createEditPanel() throws Exception {
    return editPanelClass.getConstructor(SwingEntityEditModel.class).newInstance(getEditModel());
  }

  /**
   * @return the edit model
   */
  protected SwingEntityEditModel getEditModel() {
    return editModel;
  }
}
