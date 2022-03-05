/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

class DefaultTransientProperty<T> extends AbstractProperty<T> implements TransientProperty<T> {

  private static final long serialVersionUID = 1;

  private final boolean modifiesEntity;

  protected DefaultTransientProperty(DefaultTransientPropertyBuilder<T, ?, ?> builder) {
    super(builder);
    this.modifiesEntity = builder.modifiesEntity;
  }

  @Override
  public final boolean isModifiesEntity() {
    return modifiesEntity;
  }

  static class DefaultTransientPropertyBuilder<T, P extends TransientProperty<T>, B extends TransientProperty.Builder<T, P, B>>
          extends AbstractPropertyBuilder<T, P, B> implements TransientProperty.Builder<T, P, B> {

    private boolean modifiesEntity = true;

    DefaultTransientPropertyBuilder(Attribute<T> attribute, String caption) {
      super(attribute, caption);
    }

    @Override
    public P build() {
      return (P) new DefaultTransientProperty<T>(this);
    }

    @Override
    public final TransientProperty.Builder<T, P, B> modifiesEntity(boolean modifiesEntity) {
      this.modifiesEntity = modifiesEntity;
      return this;
    }
  }
}
