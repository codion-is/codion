/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultForeignKeyProperty extends DefaultProperty<Entity> implements ForeignKeyProperty {

  private static final long serialVersionUID = 1;

  private final Set<Attribute<?>> readOnlyAttributes = new HashSet<>(1);
  private final EntityType referencedEntityType;
  private List<Attribute<?>> selectAttributes = emptyList();
  private int fetchDepth;
  private boolean softReference;

  /**
   * @param foreignKey the foreign key
   * @param caption the property caption
   */
  DefaultForeignKeyProperty(final ForeignKey foreignKey, final String caption) {
    super(foreignKey, caption);
    this.referencedEntityType = foreignKey.getReferences().get(0).getReferencedAttribute().getEntityType();
  }

  @Override
  public ForeignKey getAttribute() {
    return (ForeignKey) super.getAttribute();
  }

  @Override
  public EntityType getReferencedEntityType() {
    return referencedEntityType;
  }

  @Override
  public int getFetchDepth() {
    return fetchDepth;
  }

  @Override
  public boolean isSoftReference() {
    return softReference;
  }

  @Override
  public boolean isReadOnly(final Attribute<?> referenceAttribute) {
    return readOnlyAttributes.contains(referenceAttribute);
  }

  @Override
  public List<ForeignKey.Reference<?>> getReferences() {
    return getAttribute().getReferences();
  }

  @Override
  public List<Attribute<?>> getSelectAttributes() {
    return selectAttributes;
  }

  /**
   * @return a builder for this property instance
   */
  ForeignKeyProperty.Builder builder() {
    return new DefaultForeignKeyPropertyBuilder(this);
  }

  private static final class DefaultForeignKeyPropertyBuilder
          extends DefaultPropertyBuilder<Entity, ForeignKeyProperty.Builder> implements ForeignKeyProperty.Builder {

    private final DefaultForeignKeyProperty foreignKeyProperty;

    private DefaultForeignKeyPropertyBuilder(final DefaultForeignKeyProperty foreignKeyProperty) {
      super(foreignKeyProperty);
      this.foreignKeyProperty = foreignKeyProperty;
      foreignKeyProperty.fetchDepth = Property.FOREIGN_KEY_FETCH_DEPTH.get();
      foreignKeyProperty.softReference = false;
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
    public ForeignKeyProperty.Builder softReference() {
      foreignKeyProperty.softReference = true;
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder readOnly(final Attribute<?> referenceAttribute) {
      if (foreignKeyProperty.getAttribute().getReference(referenceAttribute) == null) {
        throw new IllegalArgumentException("Attribute " + referenceAttribute + " is not part of foreign key: " + foreignKeyProperty.getAttribute());
      }
      foreignKeyProperty.readOnlyAttributes.add(referenceAttribute);
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder selectAttributes(final Attribute<?>... attributes) {
      final Set<Attribute<?>> selectAttributes = new HashSet<>();
      for (final Attribute<?> attribute : requireNonNull(attributes)) {
        if (!attribute.getEntityType().equals(foreignKeyProperty.referencedEntityType)) {
          throw new IllegalArgumentException("Select attribute must be part of the referenced entity type");
        }
        selectAttributes.add(attribute);
      }
      foreignKeyProperty.selectAttributes = unmodifiableList(new ArrayList<>(selectAttributes));

      return this;
    }
  }
}
