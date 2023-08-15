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
import is.codion.framework.domain.entity.Key;
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
              City.ID.primaryKey(),
              City.NAME.column("Name")
                      .nullable(false))
              .keyGenerator(KeyGenerator.identity())
              .caption("Cities"));
    }

    void customer() {
      add(Customer.TYPE.define(
              Customer.ID.primaryKey(),
              Customer.NAME.column("Name")
                      .maximumLength(42),
              Customer.CITY_ID.column(),
              Customer.CITY_FK.foreignKey("City"))
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

    Key customerKey = entities.keyBuilder(Customer.TYPE)
            .with(Customer.ID, 42)
            .build();

    Entity customer = Entity.builder(customerKey)
            .with(Customer.NAME, "John")
            .with(Customer.CITY_FK, city)
            .build();

    EntityDefinition customerDefinition = entities.definition(Customer.TYPE);

    EntityDefinition cityDefinition = customerDefinition.referencedEntity(Customer.CITY_FK);

    List<Column<?>> cityPrimaryKeyColumns = cityDefinition.primaryKeyColumns();
    // end::domainUsage[]
  }
}
