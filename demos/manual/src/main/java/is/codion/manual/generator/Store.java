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
package is.codion.manual.generator;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import static is.codion.framework.domain.DomainType.domainType;

public final class Store extends DomainModel {
	public static final DomainType DOMAIN = domainType(Store.class);

	public Store() {
		super(DOMAIN);
		add(customer(), order());
	}

	public interface Customer {
		EntityType TYPE = DOMAIN.entityType("customer");
		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");
		// ...
	}

	public interface Order {
		EntityType TYPE = DOMAIN.entityType("order");
		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> CUSTOMER_ID = TYPE.integerColumn("customer_id");
		ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk", CUSTOMER_ID, Customer.ID);
		// ...
	}

	static EntityDefinition customer() {
		return Customer.TYPE.as(/* ... */).build();
	}

	static EntityDefinition order() {
		return Order.TYPE.as(/* ... */).build();
	}
}