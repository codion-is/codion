/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultForeignKey extends DefaultAttribute<Entity> implements ForeignKey {

  private static final long serialVersionUID = 1;

  private final List<Reference<?>> references;

  DefaultForeignKey(String name, EntityType entityType, List<Reference<?>> references) {
    super(name, Entity.class, entityType);
    this.references = validate(requireNonNull(references));
  }

  @Override
  public EntityType getReferencedEntityType() {
    return references.get(0).getReferencedAttribute().getEntityType();
  }

  @Override
  public List<Reference<?>> getReferences() {
    return references;
  }

  @Override
  public <T> Reference<T> getReference(Attribute<T> attribute) {
    for (int i = 0; i < references.size(); i++) {
      Reference<?> reference = references.get(i);
      if (reference.getAttribute().equals(attribute)) {
        return (Reference<T>) reference;
      }
    }

    throw new IllegalArgumentException("Attribute " + attribute + " is not part of foreign key " + getName());
  }

  private List<Reference<?>> validate(List<Reference<?>> references) {
    if (references.isEmpty()) {
      throw new IllegalArgumentException("No references provided for foreign key: " + getName());
    }
    EntityType referencedEntityType = references.get(0).getReferencedAttribute().getEntityType();
    List<Reference<?>> referenceList = new ArrayList<>(references.size());
    for (Reference<?> reference : references) {
      if (!getEntityType().equals(reference.getAttribute().getEntityType())) {
        throw new IllegalArgumentException("Entity type " + getEntityType() +
                " expected, got " + reference.getAttribute().getEntityType());
      }
      if (!referencedEntityType.equals(reference.getReferencedAttribute().getEntityType())) {
        throw new IllegalArgumentException("Entity type " + referencedEntityType +
                " expected, got " + reference.getReferencedAttribute().getEntityType());
      }
      if (referenceList.stream().anyMatch(existingReference -> existingReference.getAttribute().equals(reference.getAttribute()))) {
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

    DefaultReference(Attribute<T> attribute, Attribute<T> referencedAttribute) {
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
