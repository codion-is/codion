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

import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Column.Generator;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static is.codion.demos.chinook.domain.api.Chinook.DOMAIN;

/**
 * The {@link ColumnDefinition} javadoc snippets, each the region of the same name.
 * <p>{@link Invoice} mirrors a part of the Chinook domain API, for the usage region to declare.
 */
final class ColumnDefinitionSnippets {

  interface Invoice { // @start region=usage
    EntityType TYPE = DOMAIN.entityType("chinook.invoice");

    Column<Long> ID = TYPE.longColumn("id");
    Column<LocalDate> DATE = TYPE.localDateColumn("invoicedate");
    Column<String> BILLINGCITY = TYPE.stringColumn("billingcity");
    Column<BigDecimal> TOTAL = TYPE.bigDecimalColumn("total");
    Column<LocalDateTime> INSERT_TIME = TYPE.localDateTimeColumn("insert_time");
  }

  EntityDefinition invoice() {
    return Invoice.TYPE.as()
            .attributes(
                    // Primary key with auto-generation
                    Invoice.ID.as()
                            .primaryKey()
                            .generator(Generator.identity()),

                    // Required column
                    Invoice.DATE.as()
                            .column()
                            .caption("Date")
                            .nullable(false),

                    // String column with length constraint
                    Invoice.BILLINGCITY.as()
                            .column()
                            .caption("Billing city")
                            .maximumLength(40),

                    // Decimal column with precision and range validation,
                    // the database providing a default value
                    Invoice.TOTAL.as()
                            .column()
                            .caption("Total")
                            .nullable(false)
                            .minimum(0)
                            .fractionDigits(2)
                            .withDefault(true),

                    // Audit column (database-managed)
                    Invoice.INSERT_TIME.as()
                            .column()
                            .caption("Inserted")
                            .insertable(false)  // Not included in INSERT
                            .updatable(false)   // Not included in UPDATE
                            .withDefault(true)) // Database provides a default value
            .build();
  } // @end
}
