/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey.Reference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultForeignKeyDefinition extends AbstractAttributeDefinition<Entity> implements ForeignKeyDefinition {

  private static final long serialVersionUID = 1;

  private final Set<Column<?>> readOnlyColumns;
  private final List<Attribute<?>> attributes;
  private final int fetchDepth;
  private final boolean soft;

  private DefaultForeignKeyDefinition(DefaultForeignKeyDefinitionBuilder builder) {
    super(builder);
    this.readOnlyColumns = builder.readOnlyColumns;
    this.attributes = builder.attributes;
    this.fetchDepth = builder.fetchDepth;
    this.soft = builder.soft;
  }

  @Override
  public ForeignKey attribute() {
    return (ForeignKey) super.attribute();
  }

  @Override
  public int fetchDepth() {
    return fetchDepth;
  }

  @Override
  public boolean soft() {
    return soft;
  }

  @Override
  public boolean readOnly(Column<?> referenceColumn) {
    return readOnlyColumns.contains(referenceColumn);
  }

  @Override
  public List<Reference<?>> references() {
    return this.attribute().references();
  }

  @Override
  public List<Attribute<?>> attributes() {
    return attributes;
  }

  static final class DefaultForeignKeyDefinitionBuilder extends AbstractAttributeDefinitionBuilder<Entity, ForeignKeyDefinition.Builder>
          implements ForeignKeyDefinition.Builder {

    private final Set<Column<?>> readOnlyColumns = new HashSet<>(1);
    private final EntityType referencedEntityType;
    private final int fetchDepth;
    private final boolean soft;

    private List<Attribute<?>> attributes = emptyList();

    DefaultForeignKeyDefinitionBuilder(ForeignKey foreignKey, int fetchDepth, boolean soft) {
      super(foreignKey);
      if (fetchDepth < -1) {
        throw new IllegalArgumentException("Fetch depth must be at least -1: " + foreignKey);
      }
      this.referencedEntityType = foreignKey.referencedType();
      this.soft = soft;
      this.fetchDepth = fetchDepth;
    }

    @Override
    public ForeignKeyDefinition build() {
      return new DefaultForeignKeyDefinition(this);
    }

    @Override
    public ForeignKeyDefinition.Builder readOnly(Column<?> referenceColumn) {
      if (((ForeignKey) attribute).reference(referenceColumn) == null) {
        throw new IllegalArgumentException("Column " + referenceColumn + " is not part of foreign key: " + attribute);
      }
      this.readOnlyColumns.add(referenceColumn);
      return this;
    }

    @Override
    public ForeignKeyDefinition.Builder attributes(Attribute<?>... attributes) {
      Set<Attribute<?>> attributeSet = new HashSet<>();
      for (Attribute<?> attribute : requireNonNull(attributes)) {
        if (!attribute.entityType().equals(referencedEntityType)) {
          throw new IllegalArgumentException("Attribute must be part of the referenced entity");
        }
        attributeSet.add(attribute);
      }
      this.attributes = unmodifiableList(new ArrayList<>(attributeSet));

      return this;
    }

    @Override
    public ForeignKeyDefinition.Builder comparator(Comparator<Entity> comparator) {
      throw new UnsupportedOperationException("Foreign key values are compared using the comparator of the underlying entity");
    }
  }
}
