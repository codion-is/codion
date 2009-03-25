/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.AbstractPropertyLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.model.Property;

public abstract class AbstractEntityPropertyLink extends AbstractPropertyLink {

  private final Property property;

  public AbstractEntityPropertyLink(final EntityModel entityModel, final Property property, final LinkType linkType) {
    super(entityModel, property.propertyID, entityModel.getPropertyChangeEvent(property), linkType);
    this.property = property;
  }

  /** {@inheritDoc} */
  public Object getModelPropertyValue() {
    if (getEntityModel().isValueNull(property.propertyID))
      return null;

    return getEntityModel().getValue(property);
  }

  /** {@inheritDoc} */
  public void setModelPropertyValue(final Object value) {
    getEntityModel().uiSetValue(property, value);
  }

  /**
   * @return true if the underlying model value of this property is null
   */
  protected boolean isValueNull() {
    return getEntityModel().isValueNull(property.propertyID);
  }

  /**
   * @return the linked property
   */
  protected Property getProperty() {
    return property;
  }

  /**
   * @return the property owner, in this case a EntityModel
   */
  private EntityModel getEntityModel() {
    return (EntityModel) super.getPropertyOwner();
  }
}
