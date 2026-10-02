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

import is.codion.demos.chinook.domain.api.Chinook.InvoiceLineTotal;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;

import java.math.BigDecimal;

import static is.codion.demos.chinook.domain.api.Chinook.DOMAIN;

/**
 * The {@link Attribute} javadoc snippets, each the region of the same name.
 * <p>{@link InvoiceLine} mirrors a part of the Chinook domain API, for the usage region to declare.
 */
final class AttributeSnippets {

	interface InvoiceLine { // @start region=usage
		EntityType TYPE = DOMAIN.entityType("chinook.invoiceline");

		// Typed columns, mapped to table columns
		Column<Long> ID = TYPE.longColumn("id");
		Column<BigDecimal> UNITPRICE = TYPE.bigDecimalColumn("unitprice");
		Column<Integer> QUANTITY = TYPE.integerColumn("quantity");

		// An attribute not mapped to a column, here a derived one
		Attribute<BigDecimal> TOTAL = TYPE.bigDecimalAttribute("total");
	}

	EntityDefinition invoiceLine() {
		return InvoiceLine.TYPE.as()
						.attributes(
										InvoiceLine.ID.as()
														.primaryKey(),
										InvoiceLine.UNITPRICE.as()
														.column()
														.caption("Price")
														.nullable(false),
										InvoiceLine.QUANTITY.as()
														.column()
														.caption("Quantity")
														.nullable(false)
														.defaultValue(1),
										InvoiceLine.TOTAL.as()
														.derived()
														.from(InvoiceLine.QUANTITY, InvoiceLine.UNITPRICE)
														.with(new InvoiceLineTotal())
														.caption("Total"))
						.build();
	}

	void attributes(Entities entities) {
		// Usage with entities
		Entity invoiceLine = entities.entity(InvoiceLine.TYPE)
						.with(InvoiceLine.UNITPRICE, BigDecimal.valueOf(0.99))
						.with(InvoiceLine.QUANTITY, 2)
						.build();

		// Type-safe value access
		BigDecimal unitPrice = invoiceLine.get(InvoiceLine.UNITPRICE);
		Integer quantity = invoiceLine.get(InvoiceLine.QUANTITY);
		BigDecimal total = invoiceLine.get(InvoiceLine.TOTAL); // 1.98

		// Attribute type information
		Class<BigDecimal> priceType = InvoiceLine.UNITPRICE.type().get(); // BigDecimal.class
		boolean numeric = InvoiceLine.QUANTITY.type().isNumeric();      // true
		boolean decimal = InvoiceLine.UNITPRICE.type().isDecimal();     // true
	} // @end
}
