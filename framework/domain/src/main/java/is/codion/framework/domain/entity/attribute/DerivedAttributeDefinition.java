/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import java.util.List;

/**
 * A definition for attributes which value is derived from the values of one or more attribute.
 * @param <T> the underlying type
 */
public interface DerivedAttributeDefinition<T> extends AttributeDefinition<T> {

  /**
   * @return the attributes this attribute derives from.
   */
  List<Attribute<?>> sourceAttributes();

  /**
   * @return the value provider, providing the derived value
   */
  DerivedAttribute.Provider<T> valueProvider();
}
