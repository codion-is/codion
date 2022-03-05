/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

class DefaultTransientProperty<T> extends AbstractProperty<T> implements TransientProperty<T> {

  private static final long serialVersionUID = 1;

  private boolean modifiesEntity = true;

  /**
   * @param attribute the attribute, since TransientProperties do not map to underlying table columns,
   * the property id should not be column name, only be unique for this entity
   * @param type the data type of this property
   * @param caption the caption of this property
   */
  DefaultTransientProperty(Attribute<T> attribute, String caption) {
    super(attribute, caption);
  }

  @Override
  public final boolean isModifiesEntity() {
    return modifiesEntity;
  }

  <P extends TransientProperty<T>, B extends TransientProperty.Builder<T, P, B>> TransientProperty.Builder<T, P, B> builder() {
    return new DefaultTransientPropertyBuilder<>(this);
  }

  static class DefaultTransientPropertyBuilder<T, P extends TransientProperty<T>, B extends TransientProperty.Builder<T, P, B>>
          extends AbstractPropertyBuilder<T, P, B> implements TransientProperty.Builder<T, P, B> {

    private final DefaultTransientProperty<T> transientProperty;

    DefaultTransientPropertyBuilder(DefaultTransientProperty<T> transientProperty) {
      super(transientProperty);
      this.transientProperty = transientProperty;
    }

    @Override
    public TransientProperty.Builder<T, P, B> modifiesEntity(boolean modifiesEntity) {
      transientProperty.modifiesEntity = modifiesEntity;
      return this;
    }
  }
}
