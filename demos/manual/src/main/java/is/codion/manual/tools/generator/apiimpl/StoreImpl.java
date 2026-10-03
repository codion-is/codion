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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.tools.generator.apiimpl;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.ColumnTemplate;
import is.codion.manual.tools.generator.apiimpl.api.Store.Customer;
import is.codion.manual.tools.generator.apiimpl.api.Store.Order;

import static is.codion.framework.domain.entity.attribute.Column.Generator.identity;
import static is.codion.manual.tools.generator.apiimpl.api.Store.DOMAIN;

public final class StoreImpl extends DomainModel {
	private static final ColumnTemplate<Integer> IDENTITY_KEY = column -> column.as()
					.primaryKey()
					.generator(identity());

	public StoreImpl() {
		super(DOMAIN);
		add(customer(), order());
	}

	EntityDefinition customer() {
		return Customer.TYPE.as()
						.attributes(
										Customer.ID.as(IDENTITY_KEY),
										Customer.NAME.as()
														.column()
														.caption("Name")
														.nullable(false)
														.maximumLength(100),
										Customer.EMAIL.as()
														.column()
														.caption("Email")
														.maximumLength(255))
						.caption("Customer")
						.build();
	}

	EntityDefinition order() {
		return Order.TYPE.as()
						.attributes(
										Order.ID.as(IDENTITY_KEY),
										Order.CUSTOMER_ID.as()
														.column(),
										Order.CUSTOMER_FK.as()
														.foreignKey()
														.caption("Customer"))
						.caption("Order")
						.build();
	}
}