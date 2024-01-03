/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.framework.domain.entity.Entity;

import java.util.List;

/**
 * Represents a reference to another entity, typically but not necessarily based on a foreign key.
 */
public interface ForeignKeyDefinition extends AttributeDefinition<Entity> {

  int DEFAULT_FOREIGN_KEY_FETCH_DEPTH = 1;

  /**
   * Specifies the default foreign key fetch depth<br>
   * Value type: Integer<br>
   * Default value: 1
   */
  PropertyValue<Integer> FOREIGN_KEY_FETCH_DEPTH = Configuration.integerValue("codion.domain.foreignKeyFetchDepth", DEFAULT_FOREIGN_KEY_FETCH_DEPTH);

  /**
   * @return the foreign key attribute this foreign key is based on.
   */
  @Override
  ForeignKey attribute();

  /**
   * @return the default query fetch depth for this foreign key
   */
  int fetchDepth();

  /**
   * @return true if this foreign key is not based on a physical (table) foreign key and should not prevent deletion
   */
  boolean soft();

  /**
   * Returns true if the given foreign key reference column is read-only, as in, not updated when the foreign key value is set.
   * @param referenceColumn the reference column
   * @return true if the given foreign key reference column is read-only
   */
  boolean readOnly(Column<?> referenceColumn);

  /**
   * @return the {@link ForeignKey.Reference}s that comprise this foreign key
   */
  List<ForeignKey.Reference<?>> references();

  /**
   * @return the attributes to select when fetching entities referenced via this foreign key, an empty list in case of all attributes
   */
  List<Attribute<?>> attributes();

  /**
   * Builds a {@link ForeignKeyDefinition}.
   */
  interface Builder extends AttributeDefinition.Builder<Entity, Builder> {

    /**
     * Marks the given foreign key reference column as read-only, as in, not updated when the foreign key value is set.
     * @param referenceColumn the reference column
     * @return this instance
     */
    Builder readOnly(Column<?> referenceColumn);

    /**
     * Specifies the attributes from the referenced entity to select. Note that the primary key attributes
     * are always selected and do not have to be added via this method.
     * @param attributes the attributes to select
     * @return this instance
     */
    Builder attributes(Attribute<?>... attributes);
  }
}
