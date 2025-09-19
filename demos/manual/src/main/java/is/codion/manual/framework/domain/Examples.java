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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.manual.framework.domain;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnTemplate;

import static is.codion.framework.domain.entity.KeyGenerator.identity;

public final class Examples {
	// tag::columnTemplates[]
	public static final class Store extends DomainModel {

		public static final DomainType DOMAIN = DomainType.domainType("store");

		private static final ColumnTemplate<String> NAME = column ->
						column.define()
										.column()
										.nullable(false)
										.maximumLength(50);

		private static <T extends Number> ColumnTemplate<T> positiveNumber(double maximum) {
			return column -> column.define()
							.column()
							.nullable(false)
							.minimum(0)
							.maximum(maximum);
		}

		interface Customer {
			EntityType TYPE = DOMAIN.entityType("store.customer");

			Column<Integer> ID = TYPE.integerColumn("id");
			Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
			Column<String> LAST_NAME = TYPE.stringColumn("last_name");
			Column<Integer> BIRTH_YEAR = TYPE.integerColumn("age");
			Column<Double> DISCOUNT = TYPE.doubleColumn("discount");
		}

		public Store() {
			super(DOMAIN);
			add(customer());
		}

		EntityDefinition customer() {
			return Customer.TYPE.define(
											Customer.ID.define()
															.primaryKey(),
											Customer.FIRST_NAME.define()
															.column(NAME)
															.caption("First Name"),
											Customer.LAST_NAME.define()
															.column(NAME)
															.caption("Last Name"),
											Customer.BIRTH_YEAR.define()
															.column(positiveNumber(2100))
															.caption("Age"),
											Customer.DISCOUNT.define()
															.column(positiveNumber(8))
															.defaultValue(0d)
															.caption("Discount"))
							.keyGenerator(identity())
							.build();
		}
	}
	// end::columnTemplates[]
}
