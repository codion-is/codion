/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.AbstractPropertyLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.model.Property;

/**
 * An abstract class for linking a UI component to a EntityModel property value
 */
public abstract class AbstractEntityPropertyLink extends AbstractPropertyLink {

  /**
   * The linked property
   */
  private final Property property;

  /**
   * Instantiate a new AbstractEntityPropertyLink
   * @param entityModel the EntityModel instance
   * @param property the property to link
   * @param linkType the link type
   */
  public AbstractEntityPropertyLink(final EntityModel entityModel, final Property property, final LinkType linkType) {
    super(entityModel, property.propertyID, entityModel.getPropertyChangeEvent(property), linkType);
    this.property = property;
  }

  /** {@inheritDoc} */
  public Object getModelPropertyValue() {
    return isModelPropertyValueNull() ? null : getEntityModel().getValue(property.propertyID);
  }

  /** {@inheritDoc} */
  public void setModelPropertyValue(final Object value) {
    getEntityModel().uiSetValue(property, value);
  }

  /**
   * @return true if the underlying model value of this property is null
   */
  protected boolean isModelPropertyValueNull() {
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
