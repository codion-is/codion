/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.valuemap.ChangeValueMapEditModel;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.valuemap.AbstractValueMapLink;
import org.jminor.framework.client.model.EntityLookupModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A class for linking an EntityLookupModel to a EntityEditModel foreign key property value.
 */
public class LookupValueLink extends AbstractValueMapLink<String, Object> {

  private final EntityLookupModel lookupModel;

  /**
   * Instantiates a new LookupModelPropertyLink
   * @param lookupModel the lookup model to link
   * @param editModel the EntityEditModel instance
   * @param foreignKeyProperty the foreign key property to link
   */
  public LookupValueLink(final EntityLookupModel lookupModel, final ChangeValueMapEditModel<String, Object> editModel,
                         final Property.ForeignKeyProperty foreignKeyProperty) {
    super(editModel, foreignKeyProperty.getPropertyID(), LinkType.READ_WRITE);
    this.lookupModel = lookupModel;
    updateUI();
    lookupModel.eventSelectedEntitiesChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updateModel();
      }
    });
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIValue() {
    final List<Entity> selectedEntities = lookupModel.getSelectedEntities();
    return selectedEntities.size() == 0 ? null : selectedEntities.get(0);
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIValue(final Object propertyValue) {
    final List<Entity> value = new ArrayList<Entity>();
    if (getModelValue() != null)
      value.add((Entity) propertyValue);
    lookupModel.setSelectedEntities(value);
  }
}