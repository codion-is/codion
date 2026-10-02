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
package is.codion.demos.chinook.javadoc;

import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.math.BigDecimal;
import java.time.LocalDate;

import static is.codion.demos.chinook.domain.api.Chinook.DOMAIN;
import static is.codion.framework.domain.DomainType.domainType;

/**
 * The {@link EntityType} javadoc snippets, each the region of the same name.
 * <p>Each region declares its entities, mirroring a part of the Chinook domain API, in a holder class of its own,
 * the usage region declaring the domain type as well.
 */
final class EntityTypeSnippets {

	static final class Usage {

		// The domain API // @start region=usage
		public interface Chinook {

			DomainType DOMAIN = domainType(Chinook.class);

			// Define entity types as interfaces for organization
			interface Customer {
				EntityType TYPE = DOMAIN.entityType("chinook.customer");

				// Define typed columns
				Column<Long> ID = TYPE.longColumn("id");
				Column<String> FIRSTNAME = TYPE.stringColumn("firstname");
				Column<String> LASTNAME = TYPE.stringColumn("lastname");
				Column<String> EMAIL = TYPE.stringColumn("email");
			}

			interface Invoice {
				EntityType TYPE = DOMAIN.entityType("chinook.invoice");

				Column<Long> ID = TYPE.longColumn("id");
				Column<LocalDate> DATE = TYPE.localDateColumn("invoicedate");
				Column<BigDecimal> TOTAL = TYPE.bigDecimalColumn("total");

				// Define foreign key to Customer
				Column<Long> CUSTOMER_ID = TYPE.longColumn("customer_id");
				ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk", CUSTOMER_ID, Customer.ID);
			}
		} // @end
	}

	static final class SingleColumnForeignKey {

		// Single column foreign key // @start region=foreignKey
		interface Invoice {
			EntityType TYPE = DOMAIN.entityType("chinook.invoice");

			Column<Long> ID = TYPE.longColumn("id");
			Column<Long> CUSTOMER_ID = TYPE.longColumn("customer_id");

			// Define foreign key to Customer entity
			ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk",
							CUSTOMER_ID, Customer.ID);
		}

		// Usage in entity definition
		EntityDefinition invoice() {
			return Invoice.TYPE.as()
							.attributes(
											Invoice.ID.as()
															.primaryKey(),
											Invoice.CUSTOMER_ID.as()
															.column(),
											Invoice.CUSTOMER_FK.as()
															.foreignKey()
															.caption("Customer"))
							.build();
		} // @end
	}
}
