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
import is.codion.framework.domain.entity.Key;

import java.util.List;

import static is.codion.framework.db.condition.Condition.*;
import static is.codion.framework.demos.manual.store.minimal.domain.Store.Address;
import static is.codion.framework.demos.manual.store.minimal.domain.Store.Customer;
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
            connection.select(Customer.LAST_NAME, "Doe");

    List<Entity> doesAddresses =
            connection.select(Address.CUSTOMER_FK, customersNamedDoe);

    List<Entity> customersWithoutEmail =
            connection.select(where(attribute(Customer.EMAIL).isNull()));

    List<String> activeCustomerEmailAddresses =
            connection.select(Customer.EMAIL,
                    where(attribute(Customer.IS_ACTIVE).equalTo(true)));

    List<Entity> activeCustomersWithEmailAddresses =
            connection.select(where(and(
                    attribute(Customer.IS_ACTIVE).equalTo(true),
                    attribute(Customer.EMAIL).isNotNull())));

    //The domain model entities, a factory for Entity instances.
    Entities entities = connection.entities();

    Entity customer = entities.builder(Customer.TYPE)
            .with(Customer.FIRST_NAME, "Peter")
            .with(Customer.LAST_NAME, "Jackson")
            .build();

    Key customerKey = connection.insert(customer);
    //select to get generated and default column values
    customer = connection.select(customerKey);

    Entity address = entities.builder(Address.TYPE)
            .with(Address.CUSTOMER_FK, customer)
            .with(Address.STREET, "Elm st.")
            .with(Address.CITY, "Boston")
            .build();

    Key addressKey = connection.insert(address);

    customer.put(Customer.EMAIL, "mail@email.com");

    customer = connection.update(customer);

    connection.delete(asList(addressKey, customerKey));

    connection.close();
  }
}
