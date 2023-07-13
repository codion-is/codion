/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.test;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

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
  protected EntityEditPanelTestUnit(SwingEntityEditModel editModel,
                                    Class<? extends EntityEditPanel> editPanelClass) {
    this.editModel = requireNonNull(editModel, "editModel");
    this.editPanelClass = requireNonNull(editPanelClass, "editPanelClass");
  }

  /**
   * Initializes the edit panel.
   * @throws Exception in case of an exception
   */
  protected final void testInitialize() throws Exception {
    createEditPanel().initialize();
  }

  /**
   * Creates the edit panel for testing
   * @return the edit panel to test
   * @throws Exception in case of an exception
   */
  protected EntityEditPanel createEditPanel() throws Exception {
    return editPanelClass.getConstructor(SwingEntityEditModel.class).newInstance(editModel());
  }

  /**
   * @return the edit model
   */
  protected SwingEntityEditModel editModel() {
    return editModel;
  }
}
