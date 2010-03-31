/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.model.SearchType;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;

import java.util.Arrays;
import java.util.List;

/**
 * A static utility class for constructing query criteria implementations.
 */
public class CriteriaUtil {

  public static SelectCriteria selectCriteria(final Entity.Key key) {
    return selectCriteria(Arrays.asList(key));
  }

  public static SelectCriteria selectCriteria(final List<Entity.Key> keys) {
    final EntityKeyCriteria keyCriteria = new EntityKeyCriteria(keys);
    return new SelectCriteria(keyCriteria.getEntityID(), keyCriteria);
  }

  public static SelectCriteria selectCriteria(final String entityID, final String propertyID,
                                              final SearchType searchType, final Object... values) {
    return new SelectCriteria(entityID, new PropertyCriteria(EntityRepository.getProperty(entityID, propertyID),
            searchType, values));
  }

  public static SelectCriteria selectCriteria(final String entityID, final String propertyID,
                                              final SearchType searchType, final int fetchCount,
                                              final Object... values) {
    return new SelectCriteria(entityID, new PropertyCriteria(EntityRepository.getProperty(entityID, propertyID),
            searchType, values), fetchCount);
  }

  public static SelectCriteria selectCriteria(final String entityID, final String propertyID,
                                              final SearchType searchType, final String orderByClause,
                                              final int fetchCount, final Object... values) {
    return new SelectCriteria(entityID, new PropertyCriteria(EntityRepository.getProperty(entityID, propertyID),
            searchType, values), orderByClause, fetchCount);
  }

  public static EntityCriteria criteria(final Entity.Key key) {
    return criteria(Arrays.asList(key));
  }

  public static EntityCriteria criteria(final List<Entity.Key> keys) {
    final EntityKeyCriteria keyCriteria = new EntityKeyCriteria(keys);
    return new EntityCriteria(keyCriteria.getEntityID(), keyCriteria);
  }

  public static EntityCriteria criteria(final String entityID, final String propertyID,
                                        final SearchType searchType, final Object... values) {
    return new EntityCriteria(entityID, new PropertyCriteria(EntityRepository.getProperty(entityID, propertyID),
            searchType, values));
  }
}
