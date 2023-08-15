/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

/**
 * An attribute that does not map to an underlying database column. The value of a transient attribute
 * is initialized to null when entities are loaded, which means transient attributes always have null as the original value.
 * The value of transient attributes can be set and retrieved like normal attributes but are ignored during DML operations.
 * Note that by default setting a transient value marks the entity as being modified, but trying to update an entity
 * with only transient values modified will result in an error.
 * @param <T> the attribute value type
 */
public interface TransientAttributeDefinition<T> extends AttributeDefinition<T> {

  /**
   * @return true if the value of this attribute being modified should result in a modified entity
   */
  boolean modifiesEntity();

  /**
   * Builds a transient AttributeDefinition instance
   * @param <T> the attribute value type
   */
  interface Builder<T, B extends Builder<T, B>> extends AttributeDefinition.Builder<T, B> {

    /**
     * @param modifiesEntity if false then modifications to the value will not result in the owning entity becoming modified
     * @return this builder instance
     */
    Builder<T, B> modifiesEntity(boolean modifiesEntity);
  }
}
