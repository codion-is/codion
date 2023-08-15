/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.Entity;

final class DenormalizedValueProvider<T> implements DerivedAttribute.Provider<T> {

  private static final long serialVersionUID = 1;

  private final Attribute<Entity> entityAttribute;
  private final Attribute<T> denormalizedAttribute;

  DenormalizedValueProvider(Attribute<Entity> entityAttribute, Attribute<T> denormalizedAttribute) {
    this.entityAttribute = entityAttribute;
    this.denormalizedAttribute = denormalizedAttribute;
  }

  @Override
  public T get(DerivedAttribute.SourceValues sourceValues) {
    Entity foreignKeyValue = sourceValues.get(entityAttribute);

    return foreignKeyValue == null ? null : foreignKeyValue.get(denormalizedAttribute);
  }
}
