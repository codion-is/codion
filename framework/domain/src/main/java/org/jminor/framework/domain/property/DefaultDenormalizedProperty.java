/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

/**
 * A property representing a column that should get its value automatically from a column in a referenced table
 */
final class DefaultDenormalizedProperty extends DefaultColumnProperty implements DenormalizedProperty {

  private static final long serialVersionUID = 1;

  private final String foreignKeyPropertyId;
  private final Property denormalizedProperty;

  /**
   * @param propertyId the property ID
   * @param foreignKeyPropertyId the ID of the foreign key property which references the entity which owns
   * the denormalized property
   * @param denormalizedProperty the property from which this property should get its value
   * @param caption the caption if this property
   */
  DefaultDenormalizedProperty(final String propertyId, final String foreignKeyPropertyId,
                              final Property denormalizedProperty, final String caption) {
    super(propertyId, denormalizedProperty.getType(), caption);
    this.foreignKeyPropertyId = foreignKeyPropertyId;
    this.denormalizedProperty = denormalizedProperty;
  }

  @Override
  public String getForeignKeyPropertyId() {
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
