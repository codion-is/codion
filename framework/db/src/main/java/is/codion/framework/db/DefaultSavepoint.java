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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

import is.codion.framework.db.EntityConnection.Savepoint;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

final class DefaultSavepoint implements Savepoint, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private final UUID id;

	DefaultSavepoint(UUID id) {
		this.id = requireNonNull(id);
	}

	@Override
	public UUID id() {
		return id;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof Savepoint)) {
			return false;
		}

		return id.equals(((Savepoint) object).id());
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return "Savepoint{id=" + id + "}";
	}
}
