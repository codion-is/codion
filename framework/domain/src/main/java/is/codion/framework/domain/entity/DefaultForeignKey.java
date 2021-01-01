/*
 * Copyright (c) 2020 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

final class DefaultForeignKey extends DefaultAttribute<Entity> implements ForeignKey {

  private static final long serialVersionUID = 1;

  private final List<Reference<?>> references;

  DefaultForeignKey(final String name, final EntityType<?> entityType, final List<Reference<?>> references) {
    super(name, Entity.class, entityType);
    this.references = validate(requireNonNull(references));
  }

  @Override
  public EntityType<?> getReferencedEntityType() {
    return references.get(0).getReferencedAttribute().getEntityType();
  }

  @Override
  public List<Reference<?>> getReferences() {
    if (references.isEmpty()) {
      throw new IllegalStateException("No references defined for foreign key property: " + this);
    }

    return references;
  }

  @Override
  public <T> Reference<T> getReference(final Attribute<T> attribute) {
    final Reference<T> reference = findReference(attribute);
    if (reference == null) {
      throw new IllegalArgumentException("Attribute " + attribute + " is not part of foreign key " + getName());
    }

    return reference;
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

  private List<Reference<?>> validate(final List<Reference<?>> references) {
    if (references.isEmpty()) {
      throw new IllegalArgumentException("No references provided for foreign key: " + getName());
    }
    final EntityType<?> referencedEntityType = references.get(0).getReferencedAttribute().getEntityType();
    final List<Reference<?>> referenceList = new ArrayList<>(references.size());
    for (final Reference<?> reference : references) {
      if (!getEntityType().equals(reference.getAttribute().getEntityType())) {
        throw new IllegalArgumentException("Entity type " + getEntityType() +
                " expected, got " + reference.getAttribute().getEntityType());
      }
      if (!referencedEntityType.equals(reference.getReferencedAttribute().getEntityType())) {
        throw new IllegalArgumentException("Entity type " + referencedEntityType +
                " expected, got " + reference.getReferencedAttribute().getEntityType());
      }
      final Optional<Reference<?>> existing =
              referenceList.stream().filter(existingReference -> existingReference.getAttribute().equals(reference.getAttribute())).findFirst();
      if (existing.isPresent()) {
        throw new IllegalArgumentException("Foreign key already contains a reference for attribute: " + reference.getAttribute());
      }

      referenceList.add(reference);
    }

    return Collections.unmodifiableList(referenceList);
  }

  static final class DefaultReference<T> implements Reference<T>, Serializable {

    private static final long serialVersionUID = 1;

    private final Attribute<T> attribute;
    private final Attribute<T> referencedAttribute;

    DefaultReference(final Attribute<T> attribute, final Attribute<T> referencedAttribute) {
      if (requireNonNull(attribute, "attribute").equals(requireNonNull(referencedAttribute, "referencedAttribute"))) {
        throw new IllegalArgumentException("attribute and referencedAttribute can not be the same");
      }
      this.attribute = attribute;
      this.referencedAttribute = referencedAttribute;
    }

    @Override
    public Attribute<T> getAttribute() {
      return attribute;
    }

    @Override
    public Attribute<T> getReferencedAttribute() {
      return referencedAttribute;
    }
  }
}
