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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.manual.store.minimal;

import is.codion.framework.demos.manual.store.minimal.ManualDomainModel.Store.City;
import is.codion.framework.demos.manual.store.minimal.ManualDomainModel.Store.Customer;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.List;

class ManualDomainModel {

  // tag::storeApi[]
  public interface Store {

    DomainType DOMAIN = DomainType.domainType("Store");

    interface City {
      EntityType TYPE = DOMAIN.entityType("store.city"); //<1>

      Column<Integer> ID = TYPE.integerColumn("id"); //<2>
      Column<String> NAME = TYPE.stringColumn("name");
    }

    interface Customer {
      EntityType TYPE = DOMAIN.entityType("store.customer");

      Column<Integer> ID = TYPE.integerColumn("id");
      Column<String> NAME = TYPE.stringColumn("name");
      Column<Integer> CITY_ID = TYPE.integerColumn("city_id");

      ForeignKey CITY_FK = TYPE.foreignKey("city", CITY_ID, City.ID);
    }
  }

  // end::storeApi[]
  // tag::storeImpl[]
  public static class StoreImpl extends DefaultDomain {

    public StoreImpl() {
      super(Store.DOMAIN); //<1>
      city();
      customer();
    }

    void city() {
      add(City.TYPE.define(
              City.ID.define()
                      .primaryKey(),
              City.NAME.define()
                      .column()
                      .caption("Name")
                      .nullable(false))
              .keyGenerator(KeyGenerator.identity())
              .caption("Cities"));
    }

    void customer() {
      add(Customer.TYPE.define(
              Customer.ID.define()
                      .primaryKey(),
              Customer.NAME.define()
                      .column()
                      .caption("Name")
                      .maximumLength(42),
              Customer.CITY_ID.define()
                      .column(),
              Customer.CITY_FK.define()
                      .foreignKey()
                      .caption("City"))
              .keyGenerator(KeyGenerator.identity())
              .caption("Customers"));
    }
  }
  // end::storeImpl[]

  void usage() {
    // tag::domainUsage[]
    Domain store = new StoreImpl();

    Entities entities = store.entities();

    Entity city = entities.builder(City.TYPE)
            .with(City.NAME, "Reykjavík")
            .build();

    Entity.Key customerKey = entities.keyBuilder(Customer.TYPE)
            .with(Customer.ID, 42)
            .build();

    Entity customer = Entity.builder(customerKey)
            .with(Customer.NAME, "John")
            .with(Customer.CITY_FK, city)
            .build();

    EntityDefinition customerDefinition = entities.definition(Customer.TYPE);

    EntityDefinition cityDefinition = customerDefinition.foreignKeys().referencedBy(Customer.CITY_FK);

    List<Column<?>> cityPrimaryKeyColumns = cityDefinition.primaryKey().columns();
    // end::domainUsage[]
  }
}
