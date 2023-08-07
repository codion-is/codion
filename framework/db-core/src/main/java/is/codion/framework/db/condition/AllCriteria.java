package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

final class AllCriteria implements Criteria, Serializable {

  private static final long serialVersionUID = 1;

  private final EntityType entityType;

  AllCriteria(EntityType entityType) {
    this.entityType = requireNonNull(entityType);
  }

  @Override
  public EntityType entityType() {
    return entityType;
  }

  @Override
  public List<?> values() {
    return emptyList();
  }

  @Override
  public List<Attribute<?>> attributes() {
    return emptyList();
  }

  @Override
  public String toString(EntityDefinition definition) {
    requireNonNull(definition);
    return "";
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof AllCriteria)) {
      return false;
    }
    AllCriteria that = (AllCriteria) object;
    return Objects.equals(entityType, that.entityType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityType);
  }
}
