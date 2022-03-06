/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

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

  private final Set<Attribute<?>> readOnlyAttributes;
  private final EntityType referencedEntityType;
  private final List<Attribute<?>> selectAttributes;
  private final int fetchDepth;
  private final boolean softReference;

  private DefaultForeignKeyProperty(DefaultForeignKeyPropertyBuilder builder) {
    super(builder);
    this.readOnlyAttributes = builder.readOnlyAttributes;
    this.referencedEntityType = builder.referencedEntityType;
    this.selectAttributes = builder.selectAttributes;
    this.fetchDepth = builder.fetchDepth;
    this.softReference = builder.softReference;
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
  public boolean isReadOnly(Attribute<?> referenceAttribute) {
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

  static final class DefaultForeignKeyPropertyBuilder extends AbstractPropertyBuilder<Entity, ForeignKeyProperty.Builder>
          implements ForeignKeyProperty.Builder {

    private final Set<Attribute<?>> readOnlyAttributes = new HashSet<>(1);
    private final EntityType referencedEntityType;
    private List<Attribute<?>> selectAttributes = emptyList();
    private int fetchDepth;
    private boolean softReference;

    DefaultForeignKeyPropertyBuilder(ForeignKey foreignKey, String caption) {
      super(foreignKey, caption);
      this.referencedEntityType = foreignKey.getReferencedEntityType();
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
    public ForeignKeyProperty.Builder softReference() {
      this.softReference = true;
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder readOnly(Attribute<?> referenceAttribute) {
      if (((ForeignKey) attribute).getReference(referenceAttribute) == null) {
        throw new IllegalArgumentException("Attribute " + referenceAttribute + " is not part of foreign key: " + attribute);
      }
      this.readOnlyAttributes.add(referenceAttribute);
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder selectAttributes(Attribute<?>... attributes) {
      Set<Attribute<?>> selectAttributes = new HashSet<>();
      for (Attribute<?> attribute : requireNonNull(attributes)) {
        if (!attribute.getEntityType().equals(referencedEntityType)) {
          throw new IllegalArgumentException("Select attribute must be part of the referenced entity type");
        }
        selectAttributes.add(attribute);
      }
      this.selectAttributes = unmodifiableList(new ArrayList<>(selectAttributes));

      return this;
    }

    @Override
    public ForeignKeyProperty.Builder comparator(Comparator<Entity> comparator) {
      throw new UnsupportedOperationException("Foreign key values are compared using the comparator of the underlying entity");
    }
  }
}
