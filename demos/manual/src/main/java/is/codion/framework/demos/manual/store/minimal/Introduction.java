package is.codion.framework.demos.manual.store.minimal;

import is.codion.framework.demos.manual.store.minimal.Introduction.Store.City;
import is.codion.framework.demos.manual.store.minimal.Introduction.Store.Customer;
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

import java.util.List;

import static is.codion.framework.domain.property.Properties.*;

class Introduction {

  // tag::storeApi[]
  public interface Store {

    DomainType DOMAIN = DomainType.domainType("StoreImpl");

    interface City {
      EntityType TYPE = DOMAIN.entityType("store.city");

      Attribute<Integer> ID = TYPE.integerAttribute("id");
      Attribute<String> NAME = TYPE.stringAttribute("name");
    }

    interface Customer {
      EntityType TYPE = DOMAIN.entityType("store.customer");

      Attribute<Integer> ID = TYPE.integerAttribute("id");
      Attribute<String> NAME = TYPE.stringAttribute("name");
      Attribute<Integer> CITY_ID = TYPE.integerAttribute("city_id");

      ForeignKey CITY_FK = TYPE.foreignKey("city", Customer.CITY_ID, City.ID);
    }
  }
  // end::storeApi[]
  // tag::storeImpl[]
  public static class StoreImpl extends DefaultDomain {

    public StoreImpl() {
      super(Store.DOMAIN);
      city();
      customer();
    }

    void city() {
      define(City.TYPE,
              primaryKeyProperty(City.ID),
              columnProperty(City.NAME, "Name"));
    }

    void customer() {
      define(Customer.TYPE,
              primaryKeyProperty(Customer.ID),
              columnProperty(Customer.NAME, "Name"),
              columnProperty(Customer.CITY_ID),
              foreignKeyProperty(Customer.CITY_FK, "City"));
    }
  }
  // end::storeImpl[]

  void usage() {
    // tag::domainUsage[]
    Domain store = new StoreImpl();
    Entities entities = store.getEntities();

    EntityDefinition customerDefinition = entities.getDefinition(Customer.TYPE);
    EntityDefinition cityDefinition = customerDefinition.getForeignDefinition(Customer.CITY_FK);
    List<Attribute<?>> cityPrimaryKeyAttributes = cityDefinition.getPrimaryKeyAttributes();

    Entity city = entities.builder(City.TYPE)
            .with(City.NAME, "Reykjavík")
            .build();

    Key customerKey = entities.keyBuilder(Customer.TYPE)
            .with(Customer.ID, 42)
            .build();

    Entity customer = entities.builder(customerKey)
            .with(Customer.NAME, "John")
            .with(Customer.CITY_FK, city)
            .build();
    // end::domainUsage[]
  }
}
