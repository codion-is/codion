/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

class DefaultTransientProperty<T> extends AbstractProperty<T> implements TransientProperty<T> {

  private static final long serialVersionUID = 1;

  private final boolean modifiesEntity;

  protected DefaultTransientProperty(DefaultTransientPropertyBuilder<T, ?> builder) {
    super(builder);
    this.modifiesEntity = builder.modifiesEntity;
  }

  @Override
  public final boolean isModifiesEntity() {
    return modifiesEntity;
  }

  static class DefaultTransientPropertyBuilder<T, B extends TransientProperty.Builder<T, B>>
          extends AbstractPropertyBuilder<T, B> implements TransientProperty.Builder<T, B> {

    private boolean modifiesEntity = true;

    DefaultTransientPropertyBuilder(Attribute<T> attribute, String caption) {
      super(attribute, caption);
    }

    @Override
    public Property<T> build() {
      return new DefaultTransientProperty<>(this);
    }

    @Override
    public final TransientProperty.Builder<T, B> modifiesEntity(boolean modifiesEntity) {
      this.modifiesEntity = modifiesEntity;
      return this;
    }
  }
}
