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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.db.report;

import java.io.Serial;
import java.io.Serializable;

import static java.util.Objects.requireNonNull;

final class DefaultReportType<T, R, P> implements ReportType<T, R, P>, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private final String name;

	DefaultReportType(String name) {
		this.name = requireNonNull(name);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DefaultReportType)) {
			return false;
		}

		DefaultReportType<?, ?, ?> that = (DefaultReportType<?, ?, ?>) o;

		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return name;
	}
}
