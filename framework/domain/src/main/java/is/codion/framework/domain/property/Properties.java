/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;

final class Properties {

  private Properties() {}

  static <T, B extends TransientProperty.Builder<T, B>> TransientProperty.Builder<T, B> denormalizedViewProperty(Attribute<T> attribute, String caption,
                                                                                                                 Attribute<Entity> entityAttribute,
                                                                                                                 Attribute<T> denormalizedAttribute) {
    //todo replace with concrete package level class, see below
    DerivedProperty.Provider<T> valueProvider = sourceValues -> {
      Entity foreignKeyValue = sourceValues.get(entityAttribute);

      return foreignKeyValue == null ? null : foreignKeyValue.get(denormalizedAttribute);
    };

    return new DefaultDerivedProperty.DefaultDerivedPropertyBuilder<>(attribute, caption, valueProvider, entityAttribute);
  }

  /*
  final class DenormalizedViewValueProvider<T> implements DerivedProperty.Provider<T> {

    private static final long serialVersionUID = 1;

    private final Attribute<Entity> entityAttribute;
    private final Attribute<T> denormalizedAttribute;

    DenormalizedViewValueProvider(Attribute<Entity> entityAttribute, Attribute<T> denormalizedAttribute) {
      this.entityAttribute = entityAttribute;
      this.denormalizedAttribute = denormalizedAttribute;
    }

    @Override
    public T get(DerivedProperty.SourceValues sourceValues) {
      Entity foreignKeyValue = sourceValues.get(entityAttribute);

      return foreignKeyValue == null ? null : foreignKeyValue.get(denormalizedAttribute);
    }
  }
  */
}
