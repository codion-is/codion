/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.demos.manual.store.domain.Store;
import org.jminor.framework.demos.manual.store.model.CustomerEditModel;
import org.jminor.framework.domain.Entity;

public class Misc {

  public static void main(String[] args) throws DatabaseException, ValidationException {
    //Initialize a database connection provider using
    //the credentials scott/tiger and application identifier StoreMisc
    EntityConnectionProvider connectionProvider =
            EntityConnectionProviders.connectionProvider()
                    .setDomainClassName(Store.class.getName())
                    .setUser(new User("scott", "tiger".toCharArray()))
                    .setClientTypeId("StoreMisc");

    CustomerEditModel editModel =
            new CustomerEditModel(connectionProvider);

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
  }
}
