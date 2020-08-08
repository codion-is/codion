/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultForeignKeyProperty extends DefaultProperty<Entity> implements ForeignKeyProperty {

  private static final long serialVersionUID = 1;

  private List<Reference<?>> references;
  private EntityType<Entity> referencedEntityType;
  private int fetchDepth = Property.FOREIGN_KEY_FETCH_DEPTH.get();
  private boolean softReference = false;

  /**
   * @param attribute the attribute
   * @param caption the property caption
   */
  DefaultForeignKeyProperty(final Attribute<Entity> attribute, final String caption) {
    super(attribute, caption);
  }

  @Override
  public EntityType<Entity> getReferencedEntityType() {
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
  public List<Reference<?>> getReferences() {
    return references;
  }

  @Override
  public <T> Reference<T> getReference(final Attribute<T> attribute) {
    final Reference<T> reference = findReference(attribute);
    if (reference == null) {
      throw new IllegalArgumentException("Attribute " + attribute + " is not a foreign key reference attribute");
    }

    return reference;
  }

  /**
   * @return a builder for this property instance
   */
  ForeignKeyProperty.Builder builder() {
    return new DefaultForeignKeyPropertyBuilder(this);
  }

  private <T> Reference<T> findReference(final Attribute<T> attribute) {
    if (references != null) {
      for (int i = 0; i < references.size(); i++) {
        final Reference<?> reference = references.get(i);
        if (reference.getAttribute().equals(attribute)) {
          return (Reference<T>) reference;
        }
      }
    }

    return null;
  }

  private static final class DefaultReference<T> implements Reference<T>, Serializable {

    private final Attribute<T> attribute;
    private final Attribute<T> referencedAttribute;
    private final boolean readOnly;

    private DefaultReference(final Attribute<T> attribute, final Attribute<T> referencedAttribute,
                             final boolean readOnly) {
      this.attribute = attribute;
      this.referencedAttribute = referencedAttribute;
      this.readOnly = readOnly;
    }

    @Override
    public Attribute<T> getAttribute() {
      return attribute;
    }

    @Override
    public Attribute<T> getReferencedAttribute() {
      return referencedAttribute;
    }

    @Override
    public boolean isReadOnly() {
      return readOnly;
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

    @Override
    public <T> ForeignKeyProperty.Builder reference(final Attribute<T> attribute, final Attribute<T> referencedAttribute) {
      reference(attribute, referencedAttribute, false);
      return this;
    }

    @Override
    public <T> ForeignKeyProperty.Builder referenceReadOnly(final Attribute<T> attribute, final Attribute<T> referencedAttribute) {
      reference(attribute, referencedAttribute, true);
      return this;
    }

    private <T> void reference(final Attribute<T> attribute, final Attribute<T> referencedAttribute, final boolean readOnly) {
      requireNonNull(attribute, "attribute");
      requireNonNull(referencedAttribute, "referencedAttribute");
      if (attribute.equals(referencedAttribute)) {
        throw new IllegalArgumentException("Foreign key attribute can not reference itself");
      }
      if (!foreignKeyProperty.getAttribute().getEntityType().equals(attribute.getEntityType())) {
        throw new IllegalArgumentException("Entity type " + foreignKeyProperty.getAttribute() +
                " expected, got " + attribute.getEntityType());
      }
      if (foreignKeyProperty.referencedEntityType == null) {
        foreignKeyProperty.referencedEntityType = (EntityType<Entity>) referencedAttribute.getEntityType();
      }
      else if (!foreignKeyProperty.referencedEntityType.equals(referencedAttribute.getEntityType())) {
        throw new IllegalArgumentException("Entity type " + foreignKeyProperty.referencedEntityType +
                " expected, got " + referencedAttribute.getEntityType());
      }
      if (foreignKeyProperty.findReference(attribute) != null) {
        throw new IllegalArgumentException("Foreign key already contains a reference for column attribute: " + attribute);
      }

      foreignKeyProperty.references = addReference(attribute, referencedAttribute, readOnly);
    }

    private <T> List<Reference<?>> addReference(final Attribute<T> attribute, final Attribute<T> referencedAttribute,
                                                final boolean readOnly) {
      final List<Reference<?>> references = new ArrayList<>();
      if (foreignKeyProperty.references != null) {
        references.addAll(foreignKeyProperty.references);
      }
      references.add(new DefaultReference<>(attribute, referencedAttribute, readOnly));

      return unmodifiableList(references);
    }
  }
}
