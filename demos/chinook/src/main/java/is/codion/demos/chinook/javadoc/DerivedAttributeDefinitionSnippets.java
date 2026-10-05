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

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.DerivedAttributeDefinition;

import java.time.LocalDate;
import java.time.Period;

import static is.codion.demos.chinook.domain.api.Chinook.DOMAIN;

/**
 * The {@link DerivedAttributeDefinition} javadoc snippets, each the region of the same name.
 * <p>{@link Employee} mirrors a part of the Chinook domain API, with derived attributes added, for the usage region to declare.
 */
final class DerivedAttributeDefinitionSnippets {

  interface Employee { // @start region=usage
    EntityType TYPE = DOMAIN.entityType("chinook.employee");

    Column<Long> ID = TYPE.longColumn("id");
    Column<String> FIRSTNAME = TYPE.stringColumn("firstname");
    Column<String> LASTNAME = TYPE.stringColumn("lastname");
    Column<LocalDate> HIREDATE = TYPE.localDateColumn("hiredate");

    // Derived attributes
    Attribute<String> NAME = TYPE.stringAttribute("name");
    Attribute<Integer> YEARS_EMPLOYED = TYPE.integerAttribute("years_employed");
  }

  EntityDefinition employee() {
    return Employee.TYPE.as()
            .attributes(
                    Employee.ID.as()
                            .primaryKey(),
                    Employee.FIRSTNAME.as()
                            .column()
                            .caption("First name"),
                    Employee.LASTNAME.as()
                            .column()
                            .caption("Last name"),
                    Employee.HIREDATE.as()
                            .column()
                            .caption("Hire date"),

                    // Simple derived attribute (cached by default)
                    Employee.NAME.as()
                            .derived()
                            .from(Employee.FIRSTNAME, Employee.LASTNAME)
                            .with(values -> values.optional(Employee.FIRSTNAME).orElse("") + " " +
                                    values.optional(Employee.LASTNAME).orElse(""))
                            .caption("Name"),

                    // Time-dependent derived attribute (not cached)
                    Employee.YEARS_EMPLOYED.as()
                            .derived()
                            .from(Employee.HIREDATE)
                            .with(values -> values.optional(Employee.HIREDATE)
                                    .map(hireDate -> Period.between(hireDate, LocalDate.now()).getYears())
                                    .orElse(null))
                            .cached(false) // Changes over time
                            .caption("Years employed"))
            .build();
  }

  void employees(Entities entities) {
    // Usage
    Entity employee = entities.entity(Employee.TYPE)
            .with(Employee.FIRSTNAME, "Jane")
            .with(Employee.LASTNAME, "Peacock")
            .with(Employee.HIREDATE, LocalDate.of(2002, 4, 1))
            .build();

    // Derived values are computed automatically
    String name = employee.get(Employee.NAME);                     // "Jane Peacock" (cached)
    Integer yearsEmployed = employee.get(Employee.YEARS_EMPLOYED); // Computed on each access

    // Modifying source attributes updates derived values
    employee.set(Employee.LASTNAME, "Park");
    String newName = employee.get(Employee.NAME);                  // "Jane Park"
  } // @end
}
