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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.query;

/**
 * Defines a select query or parts of a select query, that is, from, column, where and orderBy clauses.
 * {@link Builder} provided by {@link #builder()}.
 */
public interface SelectQuery {

  /**
   * @return the COLUMNS clause string
   */
  String columns();

  /**
   * @return the FROM clause
   */
  String from();

  /**
   * @return the WHERE clause
   */
  String where();

  /**
   * @return the GROUP BY clause
   */
  String groupBy();

  /**
   * @return the HAVING clause
   */
  String having();

  /**
   * @return the order by clause
   */
  String orderBy();

  /**
   * Creates a {@link Builder}
   * @return a new {@link SelectQuery.Builder} instance.
   */
  static Builder builder() {
    return new DefaultSelectQuery.DefaultSelectQueryBuilder();
  }

  /**
   * Builds a {@link SelectQuery}.
   */
  interface Builder {

    /**
     * Specifies the columns clause to use, without the SELECT keyword.
     * @param columns the columns clause
     * @return this Builder instance
     */
    Builder columns(String columns);

    /**
     * Specifies the from clause to use.
     * @param from the from clause, without the FROM keyword
     * @return this Builder instance
     */
    Builder from(String from);

    /**
     * Specifies the where clause to use, without the WHERE keyword.
     * @param where the where clause
     * @return this Builder instance
     */
    Builder where(String where);

    /**
     * Specifies the group by clause to use, without the GROUP BY keywords.
     * @param groupBy the group by clause
     * @return this Builder instance
     */
    Builder groupBy(String groupBy);

    /**
     * Specifies the having clause to use, without the HAVING keyword.
     * @param having the having clause
     * @return this Builder instance
     */
    Builder having(String having);

    /**
     * Specifies the order by clause to use, without the ORDER BY keywords.
     * @param orderBy the order by clause
     * @return this Builder instance
     */
    Builder orderBy(String orderBy);

    /**
     * @return a new SelectQuery instance
     */
    SelectQuery build();
  }
}
