package is.codion.framework.demos.manual.store.minimal;

import is.codion.framework.demos.manual.store.minimal.ManualDomainModel.Store.City;
import is.codion.framework.demos.manual.store.minimal.ManualDomainModel.Store.Customer;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.property.Property;

import java.util.List;

class ManualDomainModel {

  // tag::storeApi[]
  public interface Store {

    DomainType DOMAIN = DomainType.domainType("StoreImpl"); //<1>

    interface City {
      EntityType TYPE = DOMAIN.entityType("store.city"); //<2>

      Attribute<Integer> ID = TYPE.integerAttribute("id"); //<3>
      Attribute<String> NAME = TYPE.stringAttribute("name");
    }

    interface Customer {
      EntityType TYPE = DOMAIN.entityType("store.customer");

      Attribute<Integer> ID = TYPE.integerAttribute("id");
      Attribute<String> NAME = TYPE.stringAttribute("name");
      Attribute<Integer> CITY_ID = TYPE.integerAttribute("city_id");

      ForeignKey CITY_FK = TYPE.foreignKey("city", CITY_ID, City.ID);// <3>
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
      add(EntityDefinition.definition(
              Property.primaryKeyProperty(City.ID),
              Property.columnProperty(City.NAME, "Name")
                      .nullable(false))
              .keyGenerator(KeyGenerator.identity())
              .caption("Cities"));
    }

    void customer() {
      add(EntityDefinition.definition(
              Property.primaryKeyProperty(Customer.ID),
              Property.columnProperty(Customer.NAME, "Name")
                      .maximumLength(42),
              Property.columnProperty(Customer.CITY_ID),
              Property.foreignKeyProperty(Customer.CITY_FK, "City"))
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

    EntityDefinition cityDefinition = customerDefinition.referencedDefinition(Customer.CITY_FK);

    List<Attribute<?>> cityPrimaryKeyAttributes = cityDefinition.primaryKeyAttributes();
    // end::domainUsage[]
  }
}
