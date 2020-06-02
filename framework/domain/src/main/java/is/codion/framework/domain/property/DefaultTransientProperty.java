/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.attribute.Attribute;

class DefaultTransientProperty<T> extends DefaultProperty<T> implements TransientProperty<T> {

  private static final long serialVersionUID = 1;

  private boolean modifiesEntity = true;

  /**
   * @param attribute the attribute, since TransientProperties do not map to underlying table columns,
   * the property id should not be column name, only be unique for this entity
   * @param type the data type of this property
   * @param caption the caption of this property
   */
  DefaultTransientProperty(final Attribute<T> attribute, final String caption) {
    super(attribute, caption);
  }

  @Override
  public final boolean isModifiesEntity() {
    return modifiesEntity;
  }

  /**
   * @return a builder for this property instance
   */
  TransientProperty.Builder<T> builder() {
    return new DefaultTransientPropertyBuilder<>(this);
  }

  static class DefaultTransientPropertyBuilder<T>
          extends DefaultPropertyBuilder<T> implements TransientProperty.Builder<T> {

    private final DefaultTransientProperty<T> transientProperty;

    DefaultTransientPropertyBuilder(final DefaultTransientProperty<T> transientProperty) {
      super(transientProperty);
      this.transientProperty = transientProperty;
    }

    @Override
    public TransientProperty<T> get() {
      return transientProperty;
    }

    @Override
    public TransientProperty.Builder<T> modifiesEntity(final boolean modifiesEntity) {
      transientProperty.modifiesEntity = modifiesEntity;
      return this;
    }
  }
}
