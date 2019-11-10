/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

class DefaultTransientProperty extends DefaultProperty implements TransientProperty {

  private static final long serialVersionUID = 1;

  private boolean modifiesEntity = true;

  /**
   * @param propertyId the property ID, since TransientProperties do not map to underlying table columns,
   * the property ID should not be column name, only be unique for this entity
   * @param type the data type of this property
   * @param caption the caption of this property
   */
  DefaultTransientProperty(final String propertyId, final int type, final String caption) {
    super(propertyId, type, caption, getTypeClass(type));
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isModifiesEntity() {
    return modifiesEntity;
  }

  /**
   * @return a builder for this property instance
   */
  TransientProperty.Builder builder() {
    return new Builder(this);
  }

  void setModifiesEntity(final boolean modifiesEntity) {
    this.modifiesEntity = modifiesEntity;
  }

  private static final class Builder extends DefaultProperty.Builder<DefaultTransientProperty>
          implements TransientProperty.Builder<DefaultTransientProperty> {

    private Builder(final DefaultTransientProperty property) {
      super(property);
    }

    /** {@inheritDoc} */
    @Override
    public Builder setModifiesEntity(final boolean modifiesEntity) {
      property.setModifiesEntity(modifiesEntity);
      return this;
    }
  }
}
