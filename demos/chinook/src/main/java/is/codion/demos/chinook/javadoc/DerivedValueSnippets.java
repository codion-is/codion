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

import is.codion.demos.chinook.domain.api.Chinook.InvoiceLine;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.DerivedValue;

import java.math.BigDecimal;

/**
 * The {@link DerivedValue} javadoc snippets, each the region of the same name.
 */
final class DerivedValueSnippets {

  void usage(Entities entities) {
    // Computes the invoice line total from the quantity and unit price // @start region=usage
    class InvoiceLineTotal implements DerivedValue<BigDecimal> {

      @Override
      public BigDecimal from(SourceValues values) {
        Integer quantity = values.get(InvoiceLine.QUANTITY);
        BigDecimal unitPrice = values.get(InvoiceLine.UNITPRICE);
        if (unitPrice == null || quantity == null) {
          return null;
        }

        return unitPrice.multiply(BigDecimal.valueOf(quantity));
      }
    }

    // In the entity definition
    InvoiceLine.TOTAL.as()
            .derived()
            .from(InvoiceLine.QUANTITY, InvoiceLine.UNITPRICE)
            .with(new InvoiceLineTotal())
            .caption("Total");

    // Usage
    Entity invoiceLine = entities.entity(InvoiceLine.TYPE)
            .with(InvoiceLine.UNITPRICE, BigDecimal.valueOf(0.99))
            .with(InvoiceLine.QUANTITY, 2)
            .build();

    // Derived values are computed automatically
    BigDecimal total = invoiceLine.get(InvoiceLine.TOTAL); // 1.98 // @end
  }
}
