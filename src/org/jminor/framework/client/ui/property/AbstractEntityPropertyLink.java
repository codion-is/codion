/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.AbstractPropertyLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.domain.Property;

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
   * @param editModel the EntityModel instance
   * @param property the property to link
   * @param linkType the link type
   */
  public AbstractEntityPropertyLink(final EntityEditModel editModel, final Property property, final LinkType linkType) {
    super(editModel, editModel.getPropertyChangeEvent(property), linkType);
    this.property = property;
  }

  /** {@inheritDoc} */
  @Override
  public Object getModelPropertyValue() {
    return isModelPropertyValueNull() ? null : getEditModel().getValue(property.getPropertyID());
  }

  /** {@inheritDoc} */
  @Override
  public void setModelPropertyValue(final Object value) {
    getEditModel().setValue(property, value);
  }

  /**
   * @return true if the underlying model value of this property is null
   */
  protected boolean isModelPropertyValueNull() {
    return getEditModel().isValueNull(property.getPropertyID());
  }

  /**
   * @return the linked property
   */
  protected Property getProperty() {
    return property;
  }

  /**
   * @return the property owner, in this case a EntityEditorModel
   */
  private EntityEditModel getEditModel() {
    return (EntityEditModel) super.getPropertyOwner();
  }
}
