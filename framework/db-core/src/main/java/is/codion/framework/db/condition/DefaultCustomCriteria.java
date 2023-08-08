/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.CriteriaType;
import is.codion.framework.domain.entity.EntityDefinition;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

final class DefaultCustomCriteria extends AbstractCriteria implements CustomCriteria {

  private static final long serialVersionUID = 1;

  private final CriteriaType criteriaType;

  DefaultCustomCriteria(CriteriaType criteriaType, List<Attribute<?>> attributes, List<Object> values) {
    super(requireNonNull(criteriaType).entityType(), attributes, values);
    this.criteriaType = criteriaType;
  }

  @Override
  public CriteriaType criteriaType() {
    return criteriaType;
  }

  @Override
  public String toString(EntityDefinition definition) {
    return requireNonNull(definition).criteriaProvider(criteriaType).toString(attributes(), values());
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultCustomCriteria)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    DefaultCustomCriteria that = (DefaultCustomCriteria) object;
    return criteriaType.equals(that.criteriaType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), criteriaType);
  }
}
