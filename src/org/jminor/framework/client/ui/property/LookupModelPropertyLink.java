/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityLookupModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.Property;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A class for linking an EntityLookupModel to a EntityModel entity property value
 */
public class LookupModelPropertyLink extends AbstractEntityPropertyLink {

  private final EntityLookupModel lookupModel;

  /**
   * Instantiates a new LookupModelPropertyLink
   * @param entityModel the EntityModel instance
   * @param propertyID the ID of the property to link
   * @param lookupModel the lookup model to link
   */
  public LookupModelPropertyLink(final EntityModel entityModel, final String propertyID,
                                 final EntityLookupModel lookupModel) {
    this(entityModel, EntityRepository.get().getEntityProperty(entityModel.getEntityID(), propertyID), lookupModel);
  }

  /**
   * Instantiates a new LookupModelPropertyLink
   * @param entityModel the EntityModel instance
   * @param property the property to link
   * @param lookupModel the lookup model to link
   */
  public LookupModelPropertyLink(final EntityModel entityModel, final Property.EntityProperty property,
                                 final EntityLookupModel lookupModel) {
    super(entityModel, property, LinkType.READ_WRITE);
    this.lookupModel = lookupModel;
    updateUI();
    lookupModel.evtSelectedEntitiesChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updateModel();
      }
    });
  }

  /** {@inheritDoc} */
  protected Object getUIPropertyValue() {
    final List<Entity> selectedEntities = lookupModel.getSelectedEntities();
    return selectedEntities.size() == 0 ? null : selectedEntities.get(0);
  }

  /** {@inheritDoc} */
  protected void setUIPropertyValue(final Object propertyValue) {
    final List<Entity> value = new ArrayList<Entity>();
    if (getModelPropertyValue() != null)
      value.add((Entity) propertyValue);
    lookupModel.setSelectedEntities(value);
  }
}