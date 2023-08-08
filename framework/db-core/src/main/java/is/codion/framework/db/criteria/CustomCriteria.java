/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.criteria;

import is.codion.framework.domain.entity.CriteriaProvider;
import is.codion.framework.domain.entity.CriteriaType;

/**
 * A Criteria based on a custom {@link CriteriaProvider}
 * associated with {@link CriteriaType}
 */
public interface CustomCriteria extends Criteria {

  /**
   * @return the criteria type
   */
  CriteriaType criteriaType();
}
