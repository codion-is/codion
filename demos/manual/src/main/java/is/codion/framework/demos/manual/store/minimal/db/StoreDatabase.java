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

import java.sql.SQLException;
import java.util.List;

import static is.codion.framework.db.condition.Conditions.selectCondition;
import static is.codion.framework.demos.manual.store.minimal.domain.Store.*;
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
            connection.select(T_CUSTOMER, CUSTOMER_LAST_NAME, "Doe");

    List<Entity> doesAddresses =
            connection.select(T_ADDRESS, ADDRESS_CUSTOMER_FK, customersNamedDoe);

    List<Entity> customersWithoutEmail =
            connection.select(selectCondition(T_CUSTOMER, CUSTOMER_EMAIL, Operator.LIKE, null));

    //The domain model entities, a factory for Entity instances.
    Entities entities = connection.getEntities();

    Entity customer = entities.entity(T_CUSTOMER);
    customer.put(CUSTOMER_FIRST_NAME, "Björn");
    customer.put(CUSTOMER_LAST_NAME, "Sigurðsson");

    Entity.Key customerKey = connection.insert(customer);
    //select to get generated and default column values
    customer = connection.selectSingle(customerKey);

    Entity address = entities.entity(T_ADDRESS);
    address.put(ADDRESS_CUSTOMER_FK, customer);
    address.put(ADDRESS_STREET, "Stóragerði");
    address.put(ADDRESS_CITY, "Reykjavík");

    Entity.Key addressKey = connection.insert(address);

    customer.put(CUSTOMER_EMAIL, "valid@email.is");

    customer = connection.update(customer);

    connection.delete(asList(addressKey, customerKey));

    connection.disconnect();
  }
}
