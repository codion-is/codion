/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.manual.store;

import dev.codion.common.db.exception.DatabaseException;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.db.EntityConnectionProviders;
import dev.codion.framework.demos.manual.store.domain.Store;
import dev.codion.framework.demos.manual.store.model.CustomerEditModel;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.domain.entity.exception.ValidationException;

public final class Misc {

  public static void main(String[] args) throws DatabaseException, ValidationException {
    // tag::editModel[]
    EntityConnectionProvider connectionProvider =
            EntityConnectionProviders.connectionProvider()
                    .setDomainClassName(Store.class.getName())
                    .setUser(Users.parseUser("scott:tiger"))
                    .setClientTypeId("StoreMisc");

    CustomerEditModel editModel = new CustomerEditModel(connectionProvider);

    editModel.put(Store.CUSTOMER_ID, 42);
    editModel.put(Store.CUSTOMER_FIRST_NAME, "Björn");
    editModel.put(Store.CUSTOMER_LAST_NAME, "Sigurðsson");
    editModel.put(Store.CUSTOMER_IS_ACTIVE, true);

    //inserts and returns the inserted entity
    Entity customer = editModel.insert();

    //modify some property values
    editModel.put(Store.CUSTOMER_FIRST_NAME, "John");
    editModel.put(Store.CUSTOMER_LAST_NAME, "Doe");

    //updates and returns the updated entity
    customer = editModel.update();

    //deletes the active entity
    editModel.delete();
    // end::editModel[]
  }
}
