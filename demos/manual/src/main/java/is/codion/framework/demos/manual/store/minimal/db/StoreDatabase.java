/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.minimal.db;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.dbms.h2database.H2DatabaseFactory;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.manual.store.minimal.domain.Store;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;

import java.util.List;

import static is.codion.framework.demos.manual.store.minimal.domain.Store.Address;
import static is.codion.framework.demos.manual.store.minimal.domain.Store.Customer;
import static is.codion.framework.domain.entity.attribute.Condition.and;
import static java.util.Arrays.asList;

public class StoreDatabase {

  static void storeEntityConnection() throws DatabaseException {
    Database database = H2DatabaseFactory
            .createDatabase("jdbc:h2:mem:store",
                    "src/main/sql/create_schema_minimal.sql");

    EntityConnectionProvider connectionProvider =
            LocalEntityConnectionProvider.builder()
                    .database(database)
                    .domain(new Store())
                    .user(User.parse("scott:tiger"))
                    .build();

    EntityConnection connection = connectionProvider.connection();

    List<Entity> customersNamedDoe =
            connection.select(Customer.LAST_NAME.equalTo("Doe"));

    List<Entity> doesAddresses =
            connection.select(Address.CUSTOMER_FK.in(customersNamedDoe));

    List<Entity> customersWithoutEmail =
            connection.select(Customer.EMAIL.isNull());

    List<String> activeCustomerEmailAddresses =
            connection.select(Customer.EMAIL,
                    Customer.ACTIVE.equalTo(true));

    List<Entity> activeCustomersWithEmailAddresses =
            connection.select(and(
                    Customer.ACTIVE.equalTo(true),
                    Customer.EMAIL.isNotNull()));

    // The domain model entities, a factory for Entity instances.
    Entities entities = connection.entities();

    Entity customer = entities.builder(Customer.TYPE)
            .with(Customer.FIRST_NAME, "Peter")
            .with(Customer.LAST_NAME, "Jackson")
            .build();

    customer = connection.insertSelect(customer);

    Entity address = entities.builder(Address.TYPE)
            .with(Address.CUSTOMER_FK, customer)
            .with(Address.STREET, "Elm st.")
            .with(Address.CITY, "Boston")
            .build();

    Entity.Key addressKey = connection.insert(address);

    customer.put(Customer.EMAIL, "mail@email.com");

    customer = connection.updateSelect(customer);

    connection.delete(asList(addressKey, customer.primaryKey()));

    connection.close();
  }
}
