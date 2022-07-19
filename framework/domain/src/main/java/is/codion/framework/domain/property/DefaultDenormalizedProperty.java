/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;

/**
 * A property representing a column that should get its value automatically from a column in a referenced table
 */
final class DefaultDenormalizedProperty<T> extends DefaultColumnProperty<T> implements DenormalizedProperty<T> {

  private static final long serialVersionUID = 1;

  private final Attribute<Entity> entityAttribute;
  private final Attribute<T> denormalizedAttribute;

  private DefaultDenormalizedProperty(DefaultDenormalizedPropertyBuilder<T, ?> builder) {
    super(builder);
    this.entityAttribute = builder.entityAttribute;
    this.denormalizedAttribute = builder.denormalizedAttribute;
  }

  @Override
  public Attribute<Entity> getEntityAttribute() {
    return entityAttribute;
  }

  @Override
  public Attribute<T> getDenormalizedAttribute() {
    return denormalizedAttribute;
  }

  @Override
  public boolean isDenormalized() {
    return true;
  }

  static final class DefaultDenormalizedPropertyBuilder<T, B extends ColumnProperty.Builder<T, B>>
          extends DefaultColumnPropertyBuilder<T, B> {

    private final Attribute<Entity> entityAttribute;
    private final Attribute<T> denormalizedAttribute;

    DefaultDenormalizedPropertyBuilder(Attribute<T> attribute, String caption, Attribute<Entity> entityAttribute,
                                       Attribute<T> denormalizedAttribute) {
      super(attribute, caption);
      this.entityAttribute = entityAttribute;
      this.denormalizedAttribute = denormalizedAttribute;
    }

    @Override
    public Property<T> build() {
      return new DefaultDenormalizedProperty<>(this);
    }
  }
}
