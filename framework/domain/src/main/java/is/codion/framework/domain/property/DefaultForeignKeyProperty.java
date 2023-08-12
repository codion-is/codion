/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.ForeignKey.Reference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultForeignKeyProperty extends AbstractProperty<Entity> implements ForeignKeyProperty {

  private static final long serialVersionUID = 1;

  private final Set<Column<?>> readOnlyColumns;
  private final EntityType referencedEntityType;
  private final List<Attribute<?>> selectAttributes;
  private final int fetchDepth;
  private final boolean softReference;

  private DefaultForeignKeyProperty(DefaultForeignKeyPropertyBuilder builder) {
    super(builder);
    this.readOnlyColumns = builder.readOnlyColumns;
    this.referencedEntityType = builder.referencedEntityType;
    this.selectAttributes = builder.selectAttributes;
    this.fetchDepth = builder.fetchDepth;
    this.softReference = builder.softReference;
  }

  @Override
  public ForeignKey attribute() {
    return (ForeignKey) super.attribute();
  }

  @Override
  public EntityType referencedType() {
    return referencedEntityType;
  }

  @Override
  public int fetchDepth() {
    return fetchDepth;
  }

  @Override
  public boolean isSoftReference() {
    return softReference;
  }

  @Override
  public boolean isReadOnly(Column<?> referenceColumn) {
    return readOnlyColumns.contains(referenceColumn);
  }

  @Override
  public List<Reference<?>> references() {
    return this.attribute().references();
  }

  @Override
  public List<Attribute<?>> selectAttributes() {
    return selectAttributes;
  }

  static final class DefaultForeignKeyPropertyBuilder extends AbstractPropertyBuilder<Entity, ForeignKeyProperty.Builder>
          implements ForeignKeyProperty.Builder {

    private final Set<Column<?>> readOnlyColumns = new HashSet<>(1);
    private final EntityType referencedEntityType;
    private List<Attribute<?>> selectAttributes = emptyList();
    private int fetchDepth;
    private boolean softReference;

    DefaultForeignKeyPropertyBuilder(ForeignKey foreignKey, String caption) {
      super(foreignKey, caption);
      this.referencedEntityType = foreignKey.referencedType();
      this.fetchDepth = Property.FOREIGN_KEY_FETCH_DEPTH.get();
      this.softReference = false;
    }

    @Override
    public ForeignKeyProperty build() {
      return new DefaultForeignKeyProperty(this);
    }

    @Override
    public ForeignKeyProperty.Builder fetchDepth(int fetchDepth) {
      this.fetchDepth = fetchDepth;
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder softReference(boolean softReference) {
      this.softReference = softReference;
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder readOnly(Column<?> referenceColumn) {
      if (((ForeignKey) attribute).reference(referenceColumn) == null) {
        throw new IllegalArgumentException("Column " + referenceColumn + " is not part of foreign key: " + attribute);
      }
      this.readOnlyColumns.add(referenceColumn);
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder selectAttributes(Attribute<?>... attributes) {
      Set<Attribute<?>> selectAttributeSet = new HashSet<>();
      for (Attribute<?> attribute : requireNonNull(attributes)) {
        if (!attribute.entityType().equals(referencedEntityType)) {
          throw new IllegalArgumentException("Select attribute must be part of the referenced entity type");
        }
        selectAttributeSet.add(attribute);
      }
      this.selectAttributes = unmodifiableList(new ArrayList<>(selectAttributeSet));

      return this;
    }

    @Override
    public ForeignKeyProperty.Builder comparator(Comparator<Entity> comparator) {
      throw new UnsupportedOperationException("Foreign key values are compared using the comparator of the underlying entity");
    }
  }
}
