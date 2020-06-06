/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.minimal.db;

import is.codion.common.db.Operator;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.Users;
import is.codion.dbms.h2database.H2DatabaseFactory;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.manual.store.minimal.domain.Store;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;

import java.sql.SQLException;
import java.util.List;

import static is.codion.framework.db.condition.Conditions.attributeCondition;
import static is.codion.framework.db.condition.Conditions.selectCondition;
import static is.codion.framework.demos.manual.store.minimal.domain.Store.Address;
import static is.codion.framework.demos.manual.store.minimal.domain.Store.Customer;
import static java.util.Arrays.asList;

public class StoreDatabase {

  static void storeEntityConnection() throws SQLException, DatabaseException {
    Database database = new H2DatabaseFactory()
            .createDatabase("jdbc:h2:mem:store",
                    "src/main/sql/create_schema_minimal.sql");

    EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(database)
                    .setDomainClassName(Store.class.getName())
                    .setUser(Users.parseUser("scott:tiger"));

    EntityConnection connection = connectionProvider.getConnection();

    List<Entity> customersNamedDoe =
            connection.select(Customer.TYPE, Customer.LAST_NAME, "Doe");

    List<Entity> doesAddresses =
            connection.select(Address.TYPE, Address.CUSTOMER_FK, customersNamedDoe);

    List<Entity> customersWithoutEmail =
            connection.select(selectCondition(Customer.TYPE, Customer.EMAIL, Operator.LIKE, null));

    List<String> activeCustomerEmailAddresses =
            connection.selectValues(Customer.EMAIL,
                    attributeCondition(Customer.IS_ACTIVE, Operator.LIKE, true));

    //The domain model entities, a factory for Entity instances.
    Entities entities = connection.getEntities();

    Entity customer = entities.entity(Customer.TYPE);
    customer.put(Customer.FIRST_NAME, "Peter");
    customer.put(Customer.LAST_NAME, "Jackson");

    Key customerKey = connection.insert(customer);
    //select to get generated and default column values
    customer = connection.selectSingle(customerKey);

    Entity address = entities.entity(Address.TYPE);
    address.put(Address.CUSTOMER_FK, customer);
    address.put(Address.STREET, "Elm st.");
    address.put(Address.CITY, "Boston");

    Key addressKey = connection.insert(address);

    customer.put(Customer.EMAIL, "mail@email.com");

    customer = connection.update(customer);

    connection.delete(asList(addressKey, customerKey));

    connection.disconnect();
  }
}
