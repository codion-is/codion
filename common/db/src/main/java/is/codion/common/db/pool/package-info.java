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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
/**
 * <p>Connection pool related classes.
 * <p>Package configuration values:
 * <ul>
 * <li>{@link is.codion.common.db.pool.ConnectionPoolWrapper#MAXIMUM_POOL_SIZE}
 * <li>{@link is.codion.common.db.pool.ConnectionPoolWrapper#MINIMUM_POOL_SIZE}
 * <li>{@link is.codion.common.db.pool.ConnectionPoolWrapper#IDLE_TIMEOUT}
 * <li>{@link is.codion.common.db.pool.ConnectionPoolWrapper#CHECK_OUT_TIMEOUT}
 * </ul>
 */
@org.jspecify.annotations.NullMarked
package is.codion.common.db.pool;