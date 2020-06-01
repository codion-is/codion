/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.attribute.EntityAttribute;

/**
 * A property representing a column that should get its value automatically from a column in a referenced table
 */
final class DefaultDenormalizedProperty<T> extends DefaultColumnProperty<T> implements DenormalizedProperty<T> {

  private static final long serialVersionUID = 1;

  private final EntityAttribute entityAttribute;
  private final Attribute<T> denormalizedAttribute;

  /**
   * @param attribute the attribute
   * @param entityAttribute the attribute of the foreign key references the entity which owns
   * the denormalized attribute
   * @param denormalizedAttribute the attribute from which this property should get its value
   * @param caption the caption if this property
   */
  DefaultDenormalizedProperty(final Attribute<T> attribute, final EntityAttribute entityAttribute,
                              final Attribute<T> denormalizedAttribute, final String caption) {
    super(attribute, caption);
    this.entityAttribute = entityAttribute;
    this.denormalizedAttribute = denormalizedAttribute;
  }

  @Override
  public EntityAttribute getEntityAttribute() {
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
