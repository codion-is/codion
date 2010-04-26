/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.ui.input.AbstractInputProvider;
import org.jminor.framework.client.model.EntityLookupModel;
import org.jminor.framework.domain.Entity;

/**
 * A InputProvider implementation for Entity values based on a EntityLookupField.
 * @see EntityLookupField
 */
public class EntityLookupProvider extends AbstractInputProvider<Entity> {

  public EntityLookupProvider(final EntityLookupModel lookupModel, final Entity currentValue) {
    super(createEntityField(lookupModel, currentValue));
  }

  @Override
  public Entity getValue() {
    final EntityLookupField lookupField = (EntityLookupField) getInputComponent();
    if (lookupField.getModel().getSelectedEntities().size() == 0)
      return null;

    return lookupField.getModel().getSelectedEntities().get(0);
  }

  private static EntityLookupField createEntityField(final EntityLookupModel lookupModel, final Object currentValue) {
    final EntityLookupField field = new EntityLookupField(lookupModel);
    if (currentValue != null)
      lookupModel.setSelectedEntity((Entity) currentValue);

    return field;
  }
}
