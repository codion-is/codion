/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.ui.input.AbstractInputProvider;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.domain.Entity;

import javax.swing.JComboBox;

/**
 * A InputManager implementation for Entity values based on a JComboBox.
 * @see EntityComboBoxModel
 */
public class EntityComboProvider extends AbstractInputProvider<Entity> {

  public EntityComboProvider(final EntityComboBoxModel model, final Entity currentValue) {
    super(createEntityField(model, currentValue));
  }

  public Entity getValue() {
    final EntityComboBoxModel model = (EntityComboBoxModel) ((JComboBox) getInputComponent()).getModel();
    return model.getSelectedEntity();
  }

  private static JComboBox createEntityField(final EntityComboBoxModel model, final Object currentValue) {
    if (!model.isDataInitialized())
      model.refresh();
    if (currentValue != null)
      model.setSelectedItem(currentValue);

    return new JComboBox(model);
  }
}
