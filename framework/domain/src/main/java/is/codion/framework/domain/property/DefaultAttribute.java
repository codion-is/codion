/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import static java.util.Objects.requireNonNull;

final class DefaultAttribute<T> implements Attribute<T> {

  private static final long serialVersionUID = 1;

  private final String attributeId;

  DefaultAttribute(final String attributeId) {
    this.attributeId = requireNonNull(attributeId);
  }

  @Override
  public String getId() {
    return attributeId;
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    final DefaultAttribute<?> that = (DefaultAttribute<?>) object;

    return attributeId.equals(that.attributeId);
  }

  @Override
  public int hashCode() {
    return attributeId.hashCode();
  }

  @Override
  public String toString() {
    return attributeId;
  }
}
