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

class DefaultTransientAttributeDefinition<T> extends AbstractAttributeDefinition<T> implements TransientAttributeDefinition<T> {

	private static final long serialVersionUID = 1;

	private final boolean modifiesEntity;

	private DefaultTransientAttributeDefinition(DefaultTransientAttributeDefinitionBuilder<T, ?> builder) {
		super(builder);
		this.modifiesEntity = builder.modifiesEntity;
	}

	@Override
	public final boolean modifiesEntity() {
		return modifiesEntity;
	}

	static class DefaultTransientAttributeDefinitionBuilder<T, B extends TransientAttributeDefinition.Builder<T, B>>
					extends AbstractAttributeDefinitionBuilder<T, B> implements TransientAttributeDefinition.Builder<T, B> {

		private boolean modifiesEntity = true;

		DefaultTransientAttributeDefinitionBuilder(Attribute<T> attribute) {
			super(attribute);
		}

		@Override
		public AttributeDefinition<T> build() {
			return new DefaultTransientAttributeDefinition<>(this);
		}

		@Override
		public final TransientAttributeDefinition.Builder<T, B> modifiesEntity(boolean modifiesEntity) {
			this.modifiesEntity = modifiesEntity;
			return this;
		}
	}
}
