/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKeyAttribute;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class DefaultForeignKeyProperty extends DefaultProperty<Entity> implements ForeignKeyProperty {

  private static final long serialVersionUID = 1;

  private final Set<Attribute<?>> readOnlyAttributes = new HashSet<>(1);
  private final EntityType<?> referencedEntityType;
  private int fetchDepth = Property.FOREIGN_KEY_FETCH_DEPTH.get();
  private boolean softReference = false;

  /**
   * @param attribute the attribute
   * @param caption the property caption
   */
  DefaultForeignKeyProperty(final ForeignKeyAttribute attribute, final String caption) {
    super(attribute, caption);
    this.referencedEntityType = attribute.getReferences().get(0).getReferencedAttribute().getEntityType();
  }

  @Override
  public ForeignKeyAttribute getAttribute() {
    return (ForeignKeyAttribute) super.getAttribute();
  }

  @Override
  public EntityType<?> getReferencedEntityType() {
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
  public List<ForeignKeyAttribute.Reference<?>> getReferences() {
    return getAttribute().getReferences();
  }

  @Override
  public <T> ForeignKeyAttribute.Reference<T> getReference(final Attribute<T> attribute) {
    return getAttribute().getReference(attribute);
  }

  /**
   * @return a builder for this property instance
   */
  ForeignKeyProperty.Builder builder() {
    return new DefaultForeignKeyPropertyBuilder(this);
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

    @Override
    public <T> ForeignKeyProperty.Builder readOnly(final Attribute<T> referenceAttribute) {
      if (foreignKeyProperty.getAttribute().getReference(referenceAttribute) == null) {
        throw new IllegalArgumentException("Attribute " + referenceAttribute + " is not part of foreign key");
      }
      foreignKeyProperty.readOnlyAttributes.add(referenceAttribute);
      return this;
    }
  }
}
