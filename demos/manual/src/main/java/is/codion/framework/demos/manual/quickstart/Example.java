/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.quickstart;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnections;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityId;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.entity.test.EntityTestUnit;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static is.codion.framework.demos.manual.quickstart.Example.Store.*;
import static is.codion.framework.domain.entity.Entities.entityId;
import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.UUID.randomUUID;

public final class Example {

  public static class Store extends Domain {

    public Store() {
      customer();
      address();
      customerAddress();
    }

    // tag::customer[]
    public static final EntityId T_CUSTOMER = entityId("store.customer");
    public static final Attribute<String> CUSTOMER_ID = T_CUSTOMER.stringAttribute("id");
    public static final Attribute<String> CUSTOMER_FIRST_NAME = T_CUSTOMER.stringAttribute("first_name");
    public static final Attribute<String> CUSTOMER_LAST_NAME = T_CUSTOMER.stringAttribute("last_name");

    void customer() {
      define(T_CUSTOMER,
              primaryKeyProperty(CUSTOMER_ID),
              columnProperty(CUSTOMER_FIRST_NAME, "First name")
                      .nullable(false).maximumLength(40),
              columnProperty(CUSTOMER_LAST_NAME, "Last name")
                      .nullable(false).maximumLength(40))
              .keyGenerator(new KeyGenerator() {
                @Override
                public void beforeInsert(Entity entity, List<ColumnProperty<?>> primaryKeyProperties,
                                         DatabaseConnection connection) throws SQLException {
                  entity.put(CUSTOMER_ID, randomUUID().toString());
                }
              })
              .stringProvider(new StringProvider(CUSTOMER_LAST_NAME)
                      .addText(", ").addValue(CUSTOMER_FIRST_NAME));
    }
    // end::customer[]
    // tag::address[]
    public static final EntityId T_ADDRESS = entityId("store.address");
    public static final Attribute<Integer> ADDRESS_ID = T_ADDRESS.integerAttribute("id");
    public static final Attribute<String> ADDRESS_STREET = T_ADDRESS.stringAttribute("street");
    public static final Attribute<String> ADDRESS_CITY = T_ADDRESS.stringAttribute("city");

    void address() {
      define(T_ADDRESS,
              primaryKeyProperty(ADDRESS_ID),
              columnProperty(ADDRESS_STREET, "Street")
                      .nullable(false).maximumLength(120),
              columnProperty(ADDRESS_CITY, "City")
                      .nullable(false).maximumLength(50))
              .keyGenerator(automatic("store.address"))
              .stringProvider(new StringProvider(ADDRESS_STREET)
                      .addText(", ").addValue(ADDRESS_CITY));
    }
    // end::address[]
    // tag::customerAddress[]
    public static final EntityId T_CUSTOMER_ADDRESS = entityId("store.customer_address");
    public static final Attribute<Integer> CUSTOMER_ADDRESS_ID = T_CUSTOMER_ADDRESS.integerAttribute("id");
    public static final Attribute<Integer> CUSTOMER_ADDRESS_CUSTOMER_ID = T_CUSTOMER_ADDRESS.integerAttribute("customer_id");
    public static final Attribute<Entity> CUSTOMER_ADDRESS_CUSTOMER_FK = T_CUSTOMER_ADDRESS.entityAttribute("customer_fk");
    public static final Attribute<Integer> CUSTOMER_ADDRESS_ADDRESS_ID = T_CUSTOMER_ADDRESS.integerAttribute("address_id");
    public static final Attribute<Entity> CUSTOMER_ADDRESS_ADDRESS_FK = T_CUSTOMER_ADDRESS.entityAttribute("address_fk");

    void customerAddress() {
      define(T_CUSTOMER_ADDRESS,
              primaryKeyProperty(CUSTOMER_ADDRESS_ID),
              foreignKeyProperty(CUSTOMER_ADDRESS_CUSTOMER_FK, "Customer", T_CUSTOMER,
                      columnProperty(CUSTOMER_ADDRESS_CUSTOMER_ID))
                      .nullable(false),
              foreignKeyProperty(CUSTOMER_ADDRESS_ADDRESS_FK, "Address", T_ADDRESS,
                      columnProperty(CUSTOMER_ADDRESS_ADDRESS_ID))
                      .nullable(false))
              .keyGenerator(automatic("store.customer_address"))
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
        setInitialFocusAttribute(CUSTOMER_FIRST_NAME);
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
        setInitialFocusAttribute(CUSTOMER_ADDRESS_CUSTOMER_FK);
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

    String lastName = johnDoe.get(CUSTOMER_LAST_NAME);
    String street = address.get(ADDRESS_STREET);
    String city = address.get(ADDRESS_CITY);
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
