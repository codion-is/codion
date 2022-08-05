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
  public EntityType referencedType() {
    return references.get(0).referencedAttribute().entityType();
  }

  @Override
  public List<Reference<?>> references() {
    return references;
  }

  @Override
  public <T> Reference<T> reference(Attribute<T> attribute) {
    for (int i = 0; i < references.size(); i++) {
      Reference<?> reference = references.get(i);
      if (reference.attribute().equals(attribute)) {
        return (Reference<T>) reference;
      }
    }

    throw new IllegalArgumentException("Attribute " + attribute + " is not part of foreign key " + name());
  }

  private List<Reference<?>> validate(List<Reference<?>> references) {
    if (references.isEmpty()) {
      throw new IllegalArgumentException("No references provided for foreign key: " + name());
    }
    EntityType referencedEntityType = references.get(0).referencedAttribute().entityType();
    List<Reference<?>> referenceList = new ArrayList<>(references.size());
    for (Reference<?> reference : references) {
      if (!entityType().equals(reference.attribute().entityType())) {
        throw new IllegalArgumentException("Entity type " + entityType() +
                " expected, got " + reference.attribute().entityType());
      }
      if (!referencedEntityType.equals(reference.referencedAttribute().entityType())) {
        throw new IllegalArgumentException("Entity type " + referencedEntityType +
                " expected, got " + reference.referencedAttribute().entityType());
      }
      if (referenceList.stream().anyMatch(existingReference -> existingReference.attribute().equals(reference.attribute()))) {
        throw new IllegalArgumentException("Foreign key already contains a reference for attribute: " + reference.attribute());
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
    public Attribute<T> attribute() {
      return attribute;
    }

    @Override
    public Attribute<T> referencedAttribute() {
      return referencedAttribute;
    }
  }
}
