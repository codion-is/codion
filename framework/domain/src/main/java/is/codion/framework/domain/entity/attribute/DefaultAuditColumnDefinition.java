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
package is.codion.framework.domain.entity.attribute;

import static java.util.Objects.requireNonNull;

final class DefaultAuditColumnDefinition<T> extends DefaultColumnDefinition<T> implements AuditColumnDefinition<T> {

	private static final long serialVersionUID = 1;

	private final AuditColumn.AuditAction auditAction;

	private DefaultAuditColumnDefinition(DefaultAuditColumnDefinitionBuilder<T, ?> builder) {
		super(builder);
		this.auditAction = builder.auditAction;
	}

	@Override
	public AuditColumn.AuditAction auditAction() {
		return auditAction;
	}

	static class DefaultAuditColumnDefinitionBuilder<T, B extends ColumnDefinition.Builder<T, B>>
					extends AbstractReadOnlyColumnDefinitionBuilder<T, B> {

		private final AuditColumn.AuditAction auditAction;

		DefaultAuditColumnDefinitionBuilder(Column<T> column, AuditColumn.AuditAction auditAction) {
			super(column);
			this.auditAction = requireNonNull(auditAction);
		}

		@Override
		public AuditColumnDefinition<T> build() {
			return new DefaultAuditColumnDefinition<>(this);
		}
	}
}
