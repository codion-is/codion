/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.util.List;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultForeignKeyProperty extends DefaultProperty<Entity> implements ForeignKeyProperty {

  private static final long serialVersionUID = 1;

  private final EntityType<Entity> referencedEntityType;
  private final List<Attribute<?>> attributes;
  private int fetchDepth = Property.FOREIGN_KEY_FETCH_DEPTH.get();
  private boolean softReference = false;

  /**
   * @param attribute the attribute, note that this is not a column
   * @param caption the property caption
   * @param referencedEntityType the type of the entity referenced by this foreign key
   * @param columnAttributes the underlying column attributes comprising this foreign key
   */
  DefaultForeignKeyProperty(final Attribute<Entity> attribute, final String caption,
                            final EntityType<?> referencedEntityType, final List<Attribute<?>> columnAttributes) {
    super(attribute, caption);
    requireNonNull(referencedEntityType, "foreignEntityType");
    validateParameters(attribute, referencedEntityType, columnAttributes);
    this.referencedEntityType = (EntityType<Entity>) referencedEntityType;
    this.attributes = unmodifiableList(columnAttributes);
  }

  @Override
  public EntityType<Entity> getReferencedEntityType() {
    return referencedEntityType;
  }

  @Override
  public List<Attribute<?>> getColumnAttributes() {
    return attributes;
  }

  @Override
  public int getFetchDepth() {
    return fetchDepth;
  }

  @Override
  public boolean isSoftReference() {
    return softReference;
  }

  /**
   * @return a builder for this property instance
   */
  ForeignKeyProperty.Builder builder() {
    return new DefaultForeignKeyPropertyBuilder(this);
  }

  private static void validateParameters(final Attribute<Entity> attribute, final EntityType<?> foreignEntityType,
                                         final List<Attribute<?>> columnAttributes) {
    if (nullOrEmpty(columnAttributes)) {
      throw new IllegalArgumentException("No column attributes specified");
    }
    for (final Attribute<?> columnAttribute : columnAttributes) {
      requireNonNull(columnAttribute, "columnAttribute");
      if (columnAttribute.equals(attribute)) {
        throw new IllegalArgumentException(foreignEntityType + ", column attribute is the same as foreign key attribute: " + attribute);
      }
    }
  }

  private static final class DefaultForeignKeyPropertyBuilder
          extends DefaultPropertyBuilder<Entity> implements ForeignKeyProperty.Builder {

    private final DefaultForeignKeyProperty foreignKeyProperty;

    private DefaultForeignKeyPropertyBuilder(final DefaultForeignKeyProperty foreignKeyProperty) {
      super(foreignKeyProperty);
      this.foreignKeyProperty = foreignKeyProperty;
    }

    @Override
    public ForeignKeyProperty get() {
      return foreignKeyProperty;
    }

    @Override
    public ForeignKeyProperty.Builder fetchDepth(final int fetchDepth) {
      foreignKeyProperty.fetchDepth = fetchDepth;
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder softReference(final boolean softReference) {
      foreignKeyProperty.softReference = softReference;
      return this;
    }
  }
}
