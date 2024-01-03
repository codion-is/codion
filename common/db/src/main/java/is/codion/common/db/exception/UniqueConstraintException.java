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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.db.exception;

import java.sql.SQLException;

/**
 * An exception indication a unique constraint failure
 */
public final class UniqueConstraintException extends DatabaseException {

  /**
   * Instantiates a new {@link UniqueConstraintException}
   * @param cause the underlying cause
   * @param message the error message
   */
  public UniqueConstraintException(SQLException cause, String message) {
    super(cause, message);
  }
}
