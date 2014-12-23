/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.ui.input.AbstractInputProvider;
import org.jminor.framework.client.model.EntityLookupModel;
import org.jminor.framework.domain.Entity;

import java.util.Collection;

/**
 * A InputProvider implementation for Entity values based on a EntityLookupField.
 * @see EntityLookupField
 */
public final class EntityLookupProvider extends AbstractInputProvider<Entity, EntityLookupField> {

  /**
   * Instantiates a new EntityLookupProvider
   * @param lookupModel the lookup model to base the lookup field on
   * @param initialValue the initial value
   */
  public EntityLookupProvider(final EntityLookupModel lookupModel, final Entity initialValue) {
    super(createEntityField(lookupModel, initialValue));
  }

  /** {@inheritDoc} */
  @Override
  public Entity getValue() {
    final Collection<Entity> selectedEntities = getInputComponent().getModel().getSelectedEntities();
    return selectedEntities.isEmpty() ? null : selectedEntities.iterator().next();
  }

  private static EntityLookupField createEntityField(final EntityLookupModel lookupModel, final Entity initialValue) {
    final EntityLookupField field = new EntityLookupField(lookupModel, false);
    if (initialValue != null) {
      lookupModel.setSelectedEntity(initialValue);
    }

    return field;
  }
}
