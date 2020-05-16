/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.db;

import is.codion.common.db.Operator;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.Users;
import is.codion.dbms.h2database.H2DatabaseProvider;
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

public class StoreDatabase {

  private static void storeEntityConnection() throws SQLException, DatabaseException {
    // tag::databaseAccess[]
    Database database = new H2DatabaseProvider()
            .createDatabase("jdbc:h2:mem:h2db",
                    "src/main/sql/create_schema.sql");

    EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(database)
                    .setDomainClassName(Store.class.getName())
                    .setUser(Users.parseUser("scott:tiger"));

    EntityConnection connection = connectionProvider.getConnection();

    List<Entity> customersNamedJoe =
            connection.select(T_CUSTOMER, CUSTOMER_FIRST_NAME, "Joe");

    List<Entity> customersWithoutEmail =
            connection.select(selectCondition(T_CUSTOMER, CUSTOMER_EMAIL, Operator.LIKE, null));

    Entities domainEntities = connection.getEntities();

    Entity customer = domainEntities.entity(T_CUSTOMER);
    customer.put(CUSTOMER_FIRST_NAME, "Björn");
    customer.put(CUSTOMER_LAST_NAME, "Sigurðsson");

    Entity.Key customerKey = connection.insert(customer);

    customer.put(CUSTOMER_EMAIL, "valid@email.bla");

    customer = connection.update(customer);

    connection.delete(customerKey);

    connection.disconnect();
    // end::databaseAccess[]
  }
}
