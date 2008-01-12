/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.State;
import org.jminor.common.ui.control.AbstractPropertyLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.model.Property;

public abstract class AbstractEntityPropertyLink extends AbstractPropertyLink {

  private final Property property;

  public AbstractEntityPropertyLink(final EntityModel entityModel, final Property property, final String caption,
                            final LinkType linkType, final State enabledState) {
    super(entityModel, caption, entityModel.getPropertyChangeEvent(property), linkType, enabledState);
    if (property == null)
      throw new IllegalArgumentException("Property cannot be null");

    this.property = property;
  }

  /** {@inheritDoc} */
  public Object getPropertyValue() {
    if (getEntityModel().isValueNull(property.propertyID))
      return null;

    return getEntityModel().getValue(property);
  }

  /** {@inheritDoc} */
  public void setPropertyValue(final Object value) {
    getEntityModel().uiSetValue(property, value);
  }

  /**
   * @return Value for property 'valueNull'.
   */
  protected boolean isValueNull() {
    return getEntityModel().isValueNull(property.propertyID);
  }

  /**
   * @return Value for property 'property'.
   */
  protected Property getProperty() {
    return property;
  }

  /**
   * @return Value for property 'entityModel'.
   */
  private EntityModel getEntityModel() {
    return (EntityModel) super.getPropertyOwner();
  }
}
