/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.quickstart;

import org.jminor.common.db.connection.DatabaseConnection;
import org.jminor.common.db.database.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnections;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.Entities;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.entity.KeyGenerator;
import org.jminor.framework.domain.entity.StringProvider;
import org.jminor.framework.domain.entity.test.EntityTestUnit;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.swing.common.ui.dialog.Dialogs;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanel;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import static java.util.UUID.randomUUID;
import static org.jminor.framework.demos.manual.quickstart.Example.Store.*;
import static org.jminor.framework.domain.entity.KeyGenerators.automatic;
import static org.jminor.framework.domain.property.Properties.*;

public final class Example {

  public static class Store extends Domain {

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
              primaryKeyProperty(CUSTOMER_ID, Types.VARCHAR),
              columnProperty(CUSTOMER_FIRST_NAME, Types.VARCHAR, "First name")
                      .nullable(false).maximumLength(40),
              columnProperty(CUSTOMER_LAST_NAME, Types.VARCHAR, "Last name")
                      .nullable(false).maximumLength(40))
              .keyGenerator(new KeyGenerator() {
                @Override
                public void beforeInsert(Entity entity, List<ColumnProperty> primaryKeyProperties,
                                         DatabaseConnection connection) throws SQLException {
                  entity.put(CUSTOMER_ID, randomUUID().toString());
                }
              })
              .stringProvider(new StringProvider(CUSTOMER_LAST_NAME)
                      .addText(", ").addValue(CUSTOMER_FIRST_NAME));
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
              columnProperty(ADDRESS_STREET, Types.VARCHAR, "Street")
                      .nullable(false).maximumLength(120),
              columnProperty(ADDRESS_CITY, Types.VARCHAR, "City")
                      .nullable(false).maximumLength(50))
              .keyGenerator(automatic(T_ADDRESS))
              .stringProvider(new StringProvider(ADDRESS_STREET)
                      .addText(", ").addValue(ADDRESS_CITY));
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
                      columnProperty(CUSTOMER_ADDRESS_CUSTOMER_ID, Types.VARCHAR))
                      .nullable(false),
              foreignKeyProperty(CUSTOMER_ADDRESS_ADDRESS_FK, "Address", T_ADDRESS,
                      columnProperty(CUSTOMER_ADDRESS_ADDRESS_ID, Types.INTEGER))
                      .nullable(false))
              .keyGenerator(automatic(T_CUSTOMER_ADDRESS))
              .caption("Customer address");
    }
    // end::customerAddress[]
  }

  static void customerPanel() {
    // tag::customerPanel[]
    class CustomerEditPanel extends EntityEditPanel {

      public CustomerEditPanel(SwingEntityEditModel editModel) {
        super(editModel);
      }

      @Override
      protected void initializeUI() {
        setInitialFocusProperty(CUSTOMER_FIRST_NAME);
        createTextField(CUSTOMER_FIRST_NAME).setColumns(12);
        createTextField(CUSTOMER_LAST_NAME).setColumns(12);
        addPropertyPanel(CUSTOMER_FIRST_NAME);
        addPropertyPanel(CUSTOMER_LAST_NAME);
      }
    }

    EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(Databases.getInstance())
                    .setDomainClassName(Store.class.getName())
                    .setUser(Users.parseUser("scott:tiger"));

    SwingEntityModel customerModel = new SwingEntityModel(T_CUSTOMER, connectionProvider);

    EntityPanel customerPanel = new EntityPanel(customerModel,
            new CustomerEditPanel(customerModel.getEditModel()));
    // end::customerPanel[]

    // tag::detailPanel[]
    class CustomerAddressEditPanel extends EntityEditPanel {

      public CustomerAddressEditPanel(SwingEntityEditModel editModel) {
        super(editModel);
      }

      @Override
      protected void initializeUI() {
        setInitialFocusProperty(CUSTOMER_ADDRESS_CUSTOMER_FK);
        createForeignKeyComboBox(CUSTOMER_ADDRESS_CUSTOMER_FK);
        createForeignKeyComboBox(CUSTOMER_ADDRESS_ADDRESS_FK);
        addPropertyPanel(CUSTOMER_ADDRESS_CUSTOMER_FK);
        addPropertyPanel(CUSTOMER_ADDRESS_ADDRESS_FK);
      }
    }

    SwingEntityModel customerAddressModel = new SwingEntityModel(T_CUSTOMER_ADDRESS, connectionProvider);

    customerModel.addDetailModel(customerAddressModel);

    EntityPanel customerAddressPanel = new EntityPanel(customerAddressModel,
            new CustomerAddressEditPanel(customerAddressModel.getEditModel()));

    customerPanel.addDetailPanel(customerAddressPanel);

    //lazy initialization of UI components
    customerPanel.initializePanel();

    //populate the model with data from the database
    customerModel.refresh();

    Dialogs.displayInDialog(null, customerPanel, "Customers");
    // end::detailPanel[]
  }

  static void domainModelTest() {
    // tag::domainModelTest[]
    class StoreTest extends EntityTestUnit {

      public StoreTest() {
        super(Store.class.getName(), Users.parseUser("scott:tiger"));
      }

      @Test
      void customer() throws DatabaseException {
        test(T_CUSTOMER);
      }

      @Test
      void address() throws DatabaseException {
        test(T_ADDRESS);
      }

      @Test
      void customerAddress() throws DatabaseException {
        test(T_CUSTOMER_ADDRESS);
      }
    }
    // end::domainModelTest[]
  }

  static void selectEntities() throws DatabaseException {
    // tag::select[]
    Store domain = new Store();

    EntityConnection connection =
            LocalEntityConnections.createConnection(
                    domain, Databases.getInstance(), Users.parseUser("scott:tiger"));

    //select customer where last name = Doe
    Entity johnDoe = connection.selectSingle(T_CUSTOMER, CUSTOMER_LAST_NAME, "Doe");

    //select all customer addresses
    List<Entity> customerAddresses = //where customer = john doe
            connection.select(T_CUSTOMER_ADDRESS, CUSTOMER_ADDRESS_CUSTOMER_FK, johnDoe);

    Entity customerAddress = customerAddresses.get(0);

    Entity address = customerAddress.getForeignKey(CUSTOMER_ADDRESS_ADDRESS_FK);

    String lastName = johnDoe.getString(CUSTOMER_LAST_NAME);
    String street = address.getString(ADDRESS_STREET);
    String city = address.getString(ADDRESS_CITY);
    // end::select[]
  }

  static void persistEntities() throws DatabaseException {
    // tag::persist[]
    Store domain = new Store();

    EntityConnection connection =
            LocalEntityConnections.createConnection(
                    domain, Databases.getInstance(), Users.parseUser("scott:tiger"));

    Entities entities = domain.getEntities();

    Entity customer = entities.entity(T_CUSTOMER);
    customer.put(CUSTOMER_FIRST_NAME, "John");
    customer.put(CUSTOMER_LAST_NAME, "Doe");

    connection.insert(customer);

    Entity address = entities.entity(T_ADDRESS);
    address.put(ADDRESS_STREET, "Elm Street 321");
    address.put(ADDRESS_CITY, "Syracuse");

    connection.insert(address);

    Entity customerAddress = entities.entity(T_CUSTOMER_ADDRESS);
    customerAddress.put(CUSTOMER_ADDRESS_CUSTOMER_FK, customer);
    customerAddress.put(CUSTOMER_ADDRESS_ADDRESS_FK, address);

    connection.insert(customerAddress);

    customer.put(CUSTOMER_FIRST_NAME, "Jonathan");

    connection.update(customer);

    connection.delete(customerAddress.getKey());
    // end::persist[]
  }
}
