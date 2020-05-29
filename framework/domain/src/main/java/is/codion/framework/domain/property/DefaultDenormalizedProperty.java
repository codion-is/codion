/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Entity;

/**
 * A property representing a column that should get its value automatically from a column in a referenced table
 */
final class DefaultDenormalizedProperty extends DefaultColumnProperty implements DenormalizedProperty {

  private static final long serialVersionUID = 1;

  private final Attribute<Entity> foreignKeyPropertyId;
  private final Property denormalizedProperty;

  /**
   * @param  propertyId the propertyId
   * @param foreignKeyPropertyId the id of the foreign key property which references the entity which owns
   * the denormalized property
   * @param denormalizedProperty the property from which this property should get its value
   * @param caption the caption if this property
   */
  DefaultDenormalizedProperty(final Attribute<?> propertyId, final Attribute<Entity> foreignKeyPropertyId,
                              final Property denormalizedProperty, final String caption) {
    super(propertyId, denormalizedProperty.getType(), caption);
    this.foreignKeyPropertyId = foreignKeyPropertyId;
    this.denormalizedProperty = denormalizedProperty;
  }

  @Override
  public Attribute<Entity> getForeignKeyPropertyId() {
    return foreignKeyPropertyId;
  }

  @Override
  public Property getDenormalizedProperty() {
    return denormalizedProperty;
  }

  @Override
  public boolean isDenormalized() {
    return true;
  }
}
