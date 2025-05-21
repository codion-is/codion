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

import is.codion.common.property.PropertyValue;
import is.codion.framework.domain.entity.Entity;

import java.util.List;

import static is.codion.common.Configuration.integerValue;

/**
 * Represents a reference to another entity, typically but not necessarily based on a foreign key.
 */
public interface ForeignKeyDefinition extends AttributeDefinition<Entity> {

	/**
	 * The default number of levels of foreign keys to fetch
	 */
	int DEFAULT_FOREIGN_KEY_FETCH_DEPTH = 1;

	/**
	 * Specifies the default foreign key fetch depth
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 1
	 * </ul>
	 */
	PropertyValue<Integer> FOREIGN_KEY_FETCH_DEPTH = integerValue("codion.domain.foreignKeyFetchDepth", DEFAULT_FOREIGN_KEY_FETCH_DEPTH);

	/**
	 * @return the foreign key attribute this foreign key is based on.
	 */
	@Override
	ForeignKey attribute();

	/**
	 * @return the default query fetch depth for this foreign key
	 */
	int fetchDepth();

	/**
	 * @return true if this foreign key is not based on a physical (table) foreign key and should not prevent deletion
	 */
	boolean soft();

	/**
	 * Returns true if the given foreign key reference column is read-only, as in, not updated when the foreign key value is set.
	 * @param referenceColumn the reference column
	 * @return true if the given foreign key reference column is read-only
	 */
	boolean readOnly(Column<?> referenceColumn);

	/**
	 * @return the {@link ForeignKey.Reference}s that comprise this foreign key
	 */
	List<ForeignKey.Reference<?>> references();

	/**
	 * @return the attributes to select when fetching entities referenced via this foreign key, an empty list in case of all attributes
	 */
	List<Attribute<?>> attributes();

	/**
	 * Builds a {@link ForeignKeyDefinition}.
	 */
	interface Builder extends AttributeDefinition.Builder<Entity, Builder> {

		/**
		 * Marks this foreign key as being soft, that is, not based on a physical (table) foreign key and should not prevent deletion
		 * @param soft if true then this foreign key is marked as a non-physical soft key
		 * @return this instance
		 */
		Builder soft(boolean soft);

		/**
		 * Marks the given foreign key reference column as read-only, which causes an exception being
		 * thrown when the given column value is modified via this foreign key.
		 * @param column the reference column
		 * @return this instance
		 */
		Builder readOnly(Column<?> column);

		/**
		 * Specifies the attributes from the referenced entity to select. Note that the primary key attributes
		 * are always selected and do not have to be added via this method.
		 * @param attributes the attributes to select
		 * @return this instance
		 */
		Builder attributes(Attribute<?>... attributes);
	}
}
