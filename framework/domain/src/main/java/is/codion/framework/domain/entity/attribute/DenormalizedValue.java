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
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import org.jspecify.annotations.Nullable;

import java.io.Serial;

import static java.util.Objects.requireNonNull;

final class DenormalizedValue<T> implements DerivedValue<T> {

	@Serial
	private static final long serialVersionUID = 1;

	private final Attribute<Entity> entityAttribute;
	private final Attribute<T> denormalizedAttribute;

	DenormalizedValue(Attribute<Entity> entityAttribute, Attribute<T> denormalizedAttribute) {
		requireNonNull(entityAttribute);
		requireNonNull(denormalizedAttribute);
		if (entityAttribute instanceof ForeignKey) {
			EntityType referencedType = ((ForeignKey) entityAttribute).referencedType();
			if (!denormalizedAttribute.entityType().equals(referencedType)) {
				throw new IllegalArgumentException("Denormalized attribute " + denormalizedAttribute + " must be from entity" + referencedType);
			}
		}
		this.entityAttribute = entityAttribute;
		this.denormalizedAttribute = denormalizedAttribute;
	}

	@Override
	public @Nullable T get(SourceValues values) {
		Entity foreignKeyValue = values.get(entityAttribute);

		return foreignKeyValue == null ? null : foreignKeyValue.get(denormalizedAttribute);
	}
}
