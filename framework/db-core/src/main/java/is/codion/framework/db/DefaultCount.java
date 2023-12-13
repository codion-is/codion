/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023, Björn Darri Sigurðsson.
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

  @Override
  public String toString() {
    return "DefaultCount{" +
            "where=" + where +
            ", having=" + having + "}";
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
