/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.domain.entity.condition.Condition;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

final class DefaultCount implements Count, Serializable {

  private static final long serialVersionUID = 1;

  private final Condition where;
  private final Condition having;

  DefaultCount(DefaultBuilder builder) {
    this.where = builder.where;
    this.having = builder.having;
  }

  @Override
  public Condition where() {
    return where;
  }

  @Override
  public Condition having() {
    return having;
  }

  static final class DefaultBuilder implements Count.Builder {

    private final Condition where;

    private Condition having;

    public DefaultBuilder(Condition where) {
      this.where = requireNonNull(where);
      this.having = Condition.all(where.entityType());
    }

    @Override
    public Builder having(Condition having) {
      this.having = having;
      return this;
    }

    @Override
    public Count build() {
      return new DefaultCount(this);
    }
  }
}
