/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

class DefaultTransientAttributeDefinition<T> extends AbstractAttributeDefinition<T> implements TransientAttributeDefinition<T> {

  private static final long serialVersionUID = 1;

  private final boolean modifiesEntity;

  private DefaultTransientAttributeDefinition(DefaultTransientAttributeDefinitionBuilder<T, ?> builder) {
    super(builder);
    this.modifiesEntity = builder.modifiesEntity;
  }

  @Override
  public final boolean modifiesEntity() {
    return modifiesEntity;
  }

  static class DefaultTransientAttributeDefinitionBuilder<T, B extends TransientAttributeDefinition.Builder<T, B>>
          extends AbstractAttributeDefinitionBuilder<T, B> implements TransientAttributeDefinition.Builder<T, B> {

    private boolean modifiesEntity = true;

    DefaultTransientAttributeDefinitionBuilder(Attribute<T> attribute) {
      super(attribute);
    }

    @Override
    public AttributeDefinition<T> build() {
      return new DefaultTransientAttributeDefinition<>(this);
    }

    @Override
    public final TransientAttributeDefinition.Builder<T, B> modifiesEntity(boolean modifiesEntity) {
      this.modifiesEntity = modifiesEntity;
      return this;
    }
  }
}
