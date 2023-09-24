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
package is.codion.framework.domain.entity.attribute;

/**
 * A {@link java.sql.Types#BLOB} based column attribute
 */
public interface BlobColumnDefinition extends ColumnDefinition<byte[]> {

  /**
   * @return true if this value should be loaded eagerly when selected
   */
  boolean isEagerlyLoaded();

  /**
   * Builds a {@link ColumnDefinition} for a boolean column.
   */
  interface Builder extends ColumnDefinition.Builder<byte[], Builder> {

    /**
     * Specifies that this value should be loaded eagerly when selected
     * @param eagerlyLoaded true if this value should be eagerly loaded
     * @return this instance
     */
    Builder eagerlyLoaded(boolean eagerlyLoaded);
  }
}
