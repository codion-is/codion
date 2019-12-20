/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.framework.domain.Entity;
import org.jminor.framework.model.EntityLookupModel;
import org.jminor.swing.common.ui.input.AbstractInputProvider;

import java.util.Collection;

/**
 * A InputProvider implementation for Entity values based on a EntityLookupField.
 * @see EntityLookupField
 */
public final class EntityLookupFieldInputProvider extends AbstractInputProvider<Entity, EntityLookupField> {

  /**
   * Instantiates a new EntityLookupProvider
   * @param lookupModel the lookup model to base the lookup field on
   * @param initialValue the initial value
   */
  public EntityLookupFieldInputProvider(final EntityLookupModel lookupModel, final Entity initialValue) {
    super(createEntityLookupField(lookupModel, initialValue));
  }

  /** {@inheritDoc} */
  @Override
  public Entity getValue() {
    final Collection<Entity> selectedEntities = getInputComponent().getModel().getSelectedEntities();

    return selectedEntities.isEmpty() ? null : selectedEntities.iterator().next();
  }

  /** {@inheritDoc} */
  @Override
  public void setValue(final Entity value) {
    getInputComponent().getModel().setSelectedEntity(value);
  }

  private static EntityLookupField createEntityLookupField(final EntityLookupModel lookupModel, final Entity initialValue) {
    final EntityLookupField field = new EntityLookupField(lookupModel, false);
    lookupModel.setSelectedEntity(initialValue);

    return field;
  }
}
