/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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

  /**
   * @param attribute the attribute
   * @param entityAttribute the attribute of the foreign key references the entity which owns
   * the denormalized attribute
   * @param denormalizedAttribute the attribute from which this property should get its value
   * @param caption the caption if this property
   */
  DefaultDenormalizedProperty(Attribute<T> attribute, Attribute<Entity> entityAttribute,
                              Attribute<T> denormalizedAttribute, String caption) {
    super(attribute, caption);
    this.entityAttribute = entityAttribute;
    this.denormalizedAttribute = denormalizedAttribute;
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
}
