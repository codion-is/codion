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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import java.io.Serial;

final class DefaultTransientAttributeDefinition<T> extends AbstractValueAttributeDefinition<T> implements TransientAttributeDefinition<T> {

	@Serial
	private static final long serialVersionUID = 1;

	private final boolean modifies;

	private DefaultTransientAttributeDefinition(DefaultTransientAttributeDefinitionBuilder<T, ?> builder) {
		super(builder);
		this.modifies = builder.modifies;
	}

	@Override
	public boolean modifies() {
		return modifies;
	}

	static final class DefaultTransientAttributeDefinitionBuilder<T, B extends TransientAttributeDefinition.Builder<T, B>>
					extends AbstractValueAttributeDefinition.AbstractValueAttributeDefinitionBuilder<T, B> implements TransientAttributeDefinition.Builder<T, B> {

		private boolean modifies = true;

		DefaultTransientAttributeDefinitionBuilder(Attribute<T> attribute) {
			super(attribute, true);
		}

		@Override
		public ValueAttributeDefinition<T> build() {
			return new DefaultTransientAttributeDefinition<>(this);
		}

		@Override
		public TransientAttributeDefinition.Builder<T, B> modifies(boolean modifies) {
			this.modifies = modifies;
			return this;
		}
	}
}
