/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.IdSource;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A static utility class.
 * User: Bjorn Darri
 * Date: 30.3.2010
 * Time: 21:47:16
 */
public class EntityDbUtil {

  /**
   * Returns the properties used when inserting an instance of this entity, leaving out properties with null values
   * @param entity the entity
   * @return the properties used to insert the given entity type
   */
  public static Collection<Property> getInsertProperties(final Entity entity) {
    final Collection<Property> properties = new ArrayList<Property>();
    for (final Property property : EntityRepository.getDatabaseProperties(entity.getEntityID(),
            EntityRepository.getIdSource(entity.getEntityID()) != IdSource.AUTO_INCREMENT, false, true)) {
      if (!(property instanceof Property.ForeignKeyProperty) && !entity.isValueNull(property.getPropertyID()))
        properties.add(property);
    }

    return properties;
  }

  /**
   * @param entity the Entity instance
   * @return the properties used to update this entity, modified properties that is
   */
  public static Collection<Property> getUpdateProperties(final Entity entity) {
    final List<Property> properties = new ArrayList<Property>();
    for (final Property property : EntityRepository.getDatabaseProperties(entity.getEntityID(), true, false, false))
      if (entity.isModified(property.getPropertyID()) && !(property instanceof Property.ForeignKeyProperty))
        properties.add(property);

    return properties;
  }
}
