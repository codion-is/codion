/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.overview;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnections;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.swing.common.ui.dialog.Dialogs;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.ui.EntityPanel;

import java.sql.Types;
import java.util.List;

import static org.jminor.framework.demos.manual.overview.Overview.Store.*;
import static org.jminor.framework.domain.KeyGenerators.automatic;
import static org.jminor.framework.domain.property.Properties.*;

public final class Overview {

  // tag::domainModel[]
  static class Store extends Domain {

    public Store() {
      customer();
      address();
      customerAddress();
    }

    // tag::customer[]
    public static final String T_CUSTOMER = "store.customer";
    public static final String CUSTOMER_ID = "id";
    public static final String CUSTOMER_FIRST_NAME = "first_name";
    public static final String CUSTOMER_LAST_NAME = "last_name";

    void customer() {
      define(T_CUSTOMER,
              primaryKeyProperty(CUSTOMER_ID, Types.INTEGER),
              columnProperty(CUSTOMER_FIRST_NAME, Types.VARCHAR, "First name"),
              columnProperty(CUSTOMER_LAST_NAME, Types.VARCHAR, "Last name"))
              .setKeyGenerator(automatic(T_CUSTOMER));
    }
    // end::customer[]
    // tag::address[]
    public static final String T_ADDRESS = "store.address";
    public static final String ADDRESS_ID = "id";
    public static final String ADDRESS_STREET = "street";
    public static final String ADDRESS_CITY = "city";

    void address() {
      define(T_ADDRESS,
              primaryKeyProperty(ADDRESS_ID, Types.INTEGER),
              columnProperty(ADDRESS_STREET, Types.VARCHAR, "Street"),
              columnProperty(ADDRESS_CITY, Types.VARCHAR, "City"))
              .setKeyGenerator(automatic(T_ADDRESS));
    }
    // end::address[]
    // tag::customerAddress[]
    public static final String T_CUSTOMER_ADDRESS = "store.customer_address";
    public static final String CUSTOMER_ADDRESS_ID = "id";
    public static final String CUSTOMER_ADDRESS_CUSTOMER_ID = "customer_id";
    public static final String CUSTOMER_ADDRESS_CUSTOMER_FK = "customer_fk";
    public static final String CUSTOMER_ADDRESS_ADDRESS_ID = "address_id";
    public static final String CUSTOMER_ADDRESS_ADDRESS_FK = "address_fk";

    void customerAddress() {
      define(T_CUSTOMER_ADDRESS,
              primaryKeyProperty(CUSTOMER_ADDRESS_ID, Types.INTEGER),
              foreignKeyProperty(CUSTOMER_ADDRESS_CUSTOMER_FK, "Customer", T_CUSTOMER,
                      columnProperty(CUSTOMER_ADDRESS_CUSTOMER_ID)),
              foreignKeyProperty(CUSTOMER_ADDRESS_ADDRESS_FK, "Address", T_ADDRESS,
                      columnProperty(CUSTOMER_ADDRESS_ADDRESS_ID)))
              .setKeyGenerator(automatic(T_CUSTOMER_ADDRESS));
    }
    // end::customerAddress[]
  }
  // end::domainModel[]

  static void customerPanel() {
    // tag::customerPanel[]
    EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(Databases.getInstance());

    SwingEntityModel customerModel = new SwingEntityModel(T_CUSTOMER, connectionProvider);
    EntityPanel customerPanel = new EntityPanel(customerModel);

    customerPanel.initializePanel();

    Dialogs.displayInDialog(null, customerPanel, "Customers");
    // end::customerPanel[]
  }

  static void selectEntities() throws DatabaseException {
    // tag::select[]
    Store domain = new Store();

    EntityConnection connection =
            LocalEntityConnections.createConnection(
                    domain, Databases.getInstance(), User.parseUser("scott:tiger"));

    //select customer where last name = Doe
    Entity johnDoe = connection.selectSingle(T_CUSTOMER, CUSTOMER_LAST_NAME, "Doe");

    //select all customer addresses
    List<Entity> customerAddresses = //where customer = john doe
            connection.select(T_CUSTOMER_ADDRESS, CUSTOMER_ADDRESS_CUSTOMER_FK, johnDoe);

    Entity customerAddress = customerAddresses.get(0);

    Entity address = customerAddress.getForeignKey(CUSTOMER_ADDRESS_ADDRESS_FK);
    // end::select[]
  }

  static void persistEntities() throws DatabaseException {
    // tag::persist[]
    Store domain = new Store();

    EntityConnection connection =
            LocalEntityConnections.createConnection(
                    domain, Databases.getInstance(), User.parseUser("scott:tiger"));

    Entity customer = domain.entity(T_CUSTOMER);
    customer.put(CUSTOMER_FIRST_NAME, "John");
    customer.put(CUSTOMER_LAST_NAME, "Doe");

    connection.insert(customer);

    Entity address = domain.entity(T_ADDRESS);
    address.put(ADDRESS_STREET, "Elm Street 321");
    address.put(ADDRESS_CITY, "Syracuse");

    connection.insert(address);

    Entity customerAddress = domain.entity(T_CUSTOMER_ADDRESS);
    customerAddress.put(CUSTOMER_ADDRESS_CUSTOMER_FK, customer);
    customerAddress.put(CUSTOMER_ADDRESS_ADDRESS_FK, address);

    connection.insert(customerAddress);

    customer.put(CUSTOMER_FIRST_NAME, "Jonathan");

    connection.update(customer);

    connection.delete(customerAddress.getKey());
    // end::persist[]
  }
}
