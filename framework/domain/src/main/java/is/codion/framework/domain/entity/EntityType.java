/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Defines an Entity type and serves as a Factory for {@link Attribute} instances associated with this entity type.
 * A factory for {@link EntityType} instances.
 */
public interface EntityType {

  /**
   * @return the name of the domain this entity type is associated with
   */
  String domainName();

  /**
   * @return the entity type name, unique within a domain.
   */
  String name();

  /**
   * @param <T> the entity class type
   * @return the entity type class
   */
  <T extends Entity> Class<T> entityClass();

  /**
   * @return the name of the resource bundle, containing captions for this entity type, if any
   */
  String resourceBundleName();

  /**
   * Creates a new {@link Attribute}, associated with this EntityType.
   * @param name the attribute name
   * @param valueClass the class representing the attribute value type
   * @param <T> the attribute type
   * @return a new {@link Attribute}
   */
  <T> Attribute<T> attribute(String name, Class<T> valueClass);

  /**
   * Creates a new Long based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new Long based attribute.
   */
  Attribute<Long> longAttribute(String name);

  /**
   * Creates a new Integer based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new Integer based attribute.
   */
  Attribute<Integer> integerAttribute(String name);

  /**
   * Creates a new Short based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new Short based attribute.
   */
  Attribute<Short> shortAttribute(String name);

  /**
   * Creates a new Double based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new Double based attribute.
   */
  Attribute<Double> doubleAttribute(String name);

  /**
   * Creates a new BigDecimal based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new BigDecimal based attribute.
   */
  Attribute<BigDecimal> bigDecimalAttribute(String name);

  /**
   * Creates a new LocalDate based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new LocalDate based attribute.
   */
  Attribute<LocalDate> localDateAttribute(String name);

  /**
   * Creates a new LocalTime based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new LocalTime based attribute.
   */
  Attribute<LocalTime> localTimeAttribute(String name);

  /**
   * Creates a new LocalDateTime based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new LocalDateTime based attribute.
   */
  Attribute<LocalDateTime> localDateTimeAttribute(String name);

  /**
   * Creates a new OffsetDateTime based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new OffsetDateTime based attribute.
   */
  Attribute<OffsetDateTime> offsetDateTimeAttribute(String name);

  /**
   * Creates a new String based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new String based attribute.
   */
  Attribute<String> stringAttribute(String name);

  /**
   * Creates a new Character based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new Character based attribute.
   */
  Attribute<Character> characterAttribute(String name);

  /**
   * Creates a new Boolean based attribute, associated with this EntityType.
   * @param name the attribute name.
   * @return a new Boolean based attribute.
   */
  Attribute<Boolean> booleanAttribute(String name);

  /**
   * Creates a new {@link Attribute}, associated with this EntityType.
   * @param name the attribute name
   * @return a new {@link Attribute}
   */
  Attribute<Entity> entityAttribute(String name);

  /**
   * Creates a new {@link Attribute}, associated with this EntityType.
   * @param name the attribute name
   * @return a new {@link Attribute}
   */
  Attribute<byte[]> byteArrayAttribute(String name);

  /**
   * Creates a new {@link ForeignKey} based on the given attributes.
   * @param name the attribute name
   * @param attribute the attribute
   * @param referencedAttribute the referenced attribute
   * @param <A> the attribute type
   * @return a new {@link ForeignKey}
   */
  <A> ForeignKey foreignKey(String name, Attribute<A> attribute, Attribute<A> referencedAttribute);

  /**
   * Creates a new {@link ForeignKey} based on the given attributes.
   * @param name the attribute name
   * @param firstAttribute the first attribute
   * @param firstReferencedAttribute the first referenced attribute
   * @param secondAttribute the second attribute
   * @param secondReferencedAttribute the second referenced attribute
   * @param <A> the first attribute type
   * @param <B> the second attribute type
   * @return a new {@link ForeignKey}
   */
  <A, B> ForeignKey foreignKey(String name,
                               Attribute<A> firstAttribute, Attribute<A> firstReferencedAttribute,
                               Attribute<B> secondAttribute, Attribute<B> secondReferencedAttribute);

  /**
   * Creates a new {@link ForeignKey} based on the given attributes.
   * @param name the attribute name
   * @param firstAttribute the first attribute
   * @param firstReferencedAttribute the first referenced attribute
   * @param secondAttribute the second attribute
   * @param secondReferencedAttribute the third referenced attribute
   * @param thirdAttribute the second attribute
   * @param thirdReferencedAttribute the third referenced attribute
   * @param <A> the first attribute type
   * @param <B> the second attribute type
   * @param <C> the third attribute type
   * @return a new {@link ForeignKey}
   */
  <A, B, C> ForeignKey foreignKey(String name,
                                  Attribute<A> firstAttribute, Attribute<A> firstReferencedAttribute,
                                  Attribute<B> secondAttribute, Attribute<B> secondReferencedAttribute,
                                  Attribute<C> thirdAttribute, Attribute<C> thirdReferencedAttribute);

  /**
   * Creates a new {@link ForeignKey} based on the given references.
   * @param name the attribute name
   * @param references the references
   * @return a new {@link ForeignKey}
   * @see ForeignKey#reference(Attribute, Attribute)
   */
  ForeignKey foreignKey(String name, List<ForeignKey.Reference<?>> references);

  /**
   * Instantiates a new {@link CriteriaType} for this entity type
   * @param name the name
   * @return a new criteria type
   */
  CriteriaType criteriaType(String name);

  /**
   * Creates a new EntityType instance.
   * @param name the entity type name
   * @param domainName the name of the domain to associate this entity type with
   * @param entityClass the entity representation class
   * @param <T> the entity representation type
   * @return a {@link EntityType} instance with the given name
   */
  static <T extends Entity> EntityType entityType(String name, String domainName,
                                                  Class<T> entityClass) {
    String bundleName = null;
    try {
      ResourceBundle.getBundle(entityClass.getName());
      bundleName = entityClass.getName();
    }
    catch (MissingResourceException e) {/* Non-existing bundle */}

    return new DefaultEntityType(domainName, name, entityClass, bundleName);
  }

  /**
   * Creates a new EntityType instance.
   * @param name the entity type name
   * @param domainName the name of the domain to associate this entity type with
   * @param resourceBundleName the name of a resource bundle to use for captions, if any
   * @return a {@link EntityType} instance with the given name
   */
  static EntityType entityType(String name, String domainName,
                               String resourceBundleName) {
    return new DefaultEntityType(domainName, name, Entity.class, resourceBundleName);
  }

  /**
   * Creates a new EntityType instance.
   * @param name the entity type name
   * @param domainName the name of the domain to associate this entity type with
   * @param entityClass the entity representation class
   * @param resourceBundleName the name of a resource bundle to use for captions, if any
   * @param <T> the entity representation type
   * @return a {@link EntityType} instance with the given name
   */
  static <T extends Entity> EntityType entityType(String name, String domainName,
                                                  Class<T> entityClass, String resourceBundleName) {
    return new DefaultEntityType(domainName, name, entityClass, resourceBundleName);
  }
}
