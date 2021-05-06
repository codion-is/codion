/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.quickstart;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.KeyGenerator;
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
import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.automatic;
import static is.codion.framework.domain.entity.StringFactory.stringFactory;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.UUID.randomUUID;

public final class Example {

  public static class Store extends DefaultDomain {

    static final DomainType DOMAIN = domainType(Store.class);

    public Store() {
      super(DOMAIN);
      customer();
      address();
      customerAddress();
    }

    // tag::customer[]
    public interface Customer {
      EntityType<Entity> TYPE = DOMAIN.entityType("store.customer");

      Attribute<String> ID = TYPE.stringAttribute("id");
      Attribute<String> FIRST_NAME = TYPE.stringAttribute("first_name");
      Attribute<String> LAST_NAME = TYPE.stringAttribute("last_name");
    }

    void customer() {
      define(Customer.TYPE,
              primaryKeyProperty(Customer.ID),
              columnProperty(Customer.FIRST_NAME, "First name")
                      .nullable(false).maximumLength(40),
              columnProperty(Customer.LAST_NAME, "Last name")
                      .nullable(false).maximumLength(40))
              .keyGenerator(new KeyGenerator() {
                @Override
                public void beforeInsert(Entity entity, List<ColumnProperty<?>> primaryKeyProperties,
                                         DatabaseConnection connection) throws SQLException {
                  entity.put(Customer.ID, randomUUID().toString());
                }
              })
              .stringFactory(stringFactory(Customer.LAST_NAME)
                      .text(", ").value(Customer.FIRST_NAME));
    }
    // end::customer[]
    // tag::address[]
    public interface Address {
      EntityType<Entity> TYPE = DOMAIN.entityType("store.address");

      Attribute<Integer> ID = TYPE.integerAttribute("id");
      Attribute<String> STREET = TYPE.stringAttribute("street");
      Attribute<String> CITY = TYPE.stringAttribute("city");
    }

    void address() {
      define(Address.TYPE,
              primaryKeyProperty(Address.ID),
              columnProperty(Address.STREET, "Street")
                      .nullable(false).maximumLength(120),
              columnProperty(Address.CITY, "City")
                      .nullable(false).maximumLength(50))
              .keyGenerator(automatic("store.address"))
              .stringFactory(stringFactory(Address.STREET)
                      .text(", ").value(Address.CITY));
    }
    // end::address[]
    // tag::customerAddress[]
    public interface CustomerAddress {
      EntityType<Entity> TYPE = DOMAIN.entityType("store.customer_address");

      Attribute<Integer> ID = TYPE.integerAttribute("id");
      Attribute<String> CUSTOMER_ID = TYPE.stringAttribute("customer_id");
      Attribute<Integer> ADDRESS_ID = TYPE.integerAttribute("address_id");

      ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk",
              CustomerAddress.CUSTOMER_ID, Customer.ID);
      ForeignKey ADDRESS_FK = TYPE.foreignKey("address_fk",
              CustomerAddress.ADDRESS_ID, Address.ID);
    }

    void customerAddress() {
      define(CustomerAddress.TYPE,
              primaryKeyProperty(CustomerAddress.ID),
              columnProperty(CustomerAddress.CUSTOMER_ID)
                      .nullable(false),
              foreignKeyProperty(CustomerAddress.CUSTOMER_FK, "Customer"),
              columnProperty(CustomerAddress.ADDRESS_ID)
                      .nullable(false),
              foreignKeyProperty(CustomerAddress.ADDRESS_FK, "Address"))
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
        setInitialFocusAttribute(Customer.FIRST_NAME);
        createTextField(Customer.FIRST_NAME).setColumns(12);
        createTextField(Customer.LAST_NAME).setColumns(12);
        addInputPanel(Customer.FIRST_NAME);
        addInputPanel(Customer.LAST_NAME);
      }
    }

    EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(DatabaseFactory.getDatabase())
                    .setDomainClassName(Store.class.getName())
                    .setUser(User.parseUser("scott:tiger"));

    SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connectionProvider);

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
        setInitialFocusAttribute(CustomerAddress.CUSTOMER_FK);
        createForeignKeyComboBox(CustomerAddress.CUSTOMER_FK);
        createForeignKeyComboBox(CustomerAddress.ADDRESS_FK);
        addInputPanel(CustomerAddress.CUSTOMER_FK);
        addInputPanel(CustomerAddress.ADDRESS_FK);
      }
    }

    SwingEntityModel customerAddressModel = new SwingEntityModel(CustomerAddress.TYPE, connectionProvider);

    customerModel.addDetailModel(customerAddressModel);

    EntityPanel customerAddressPanel = new EntityPanel(customerAddressModel,
            new CustomerAddressEditPanel(customerAddressModel.getEditModel()));

    customerPanel.addDetailPanel(customerAddressPanel);

    //lazy initialization of UI components
    customerPanel.initializePanel();

    //populate the model with data from the database
    customerModel.refresh();

    Dialogs.builder()
            .component(customerPanel)
            .title("Customers")
            .build()
            .setVisible(true);
    // end::detailPanel[]
  }

  static void domainModelTest() {
    // tag::domainModelTest[]
    class StoreTest extends EntityTestUnit {

      public StoreTest() {
        super(Store.class.getName(), User.parseUser("scott:tiger"));
      }

      @Test
      void customer() throws DatabaseException {
        test(Customer.TYPE);
      }

      @Test
      void address() throws DatabaseException {
        test(Address.TYPE);
      }

      @Test
      void customerAddress() throws DatabaseException {
        test(CustomerAddress.TYPE);
      }
    }
    // end::domainModelTest[]
  }

  static void selectEntities() throws DatabaseException {
    // tag::select[]
    Store domain = new Store();

    EntityConnection connection =
            LocalEntityConnection.localEntityConnection(
                    domain, DatabaseFactory.getDatabase(), User.parseUser("scott:tiger"));

    //select customer where last name = Doe
    Entity johnDoe = connection.selectSingle(Customer.LAST_NAME, "Doe");

    //select all customer addresses
    List<Entity> customerAddresses = //where customer = john doe
            connection.select(CustomerAddress.CUSTOMER_FK, johnDoe);

    Entity customerAddress = customerAddresses.get(0);

    Entity address = customerAddress.getForeignKey(CustomerAddress.ADDRESS_FK);

    String lastName = johnDoe.get(Customer.LAST_NAME);
    String street = address.get(Address.STREET);
    String city = address.get(Address.CITY);
    // end::select[]
  }

  static void persistEntities() throws DatabaseException {
    // tag::persist[]
    Store domain = new Store();

    EntityConnection connection =
            LocalEntityConnection.localEntityConnection(
                    domain, DatabaseFactory.getDatabase(), User.parseUser("scott:tiger"));

    Entities entities = domain.getEntities();

    Entity customer = entities.entity(Customer.TYPE);
    customer.put(Customer.FIRST_NAME, "John");
    customer.put(Customer.LAST_NAME, "Doe");

    connection.insert(customer);

    Entity address = entities.entity(Address.TYPE);
    address.put(Address.STREET, "Elm Street 321");
    address.put(Address.CITY, "Syracuse");

    connection.insert(address);

    Entity customerAddress = entities.entity(CustomerAddress.TYPE);
    customerAddress.put(CustomerAddress.CUSTOMER_FK, customer);
    customerAddress.put(CustomerAddress.ADDRESS_FK, address);

    connection.insert(customerAddress);

    customer.put(Customer.FIRST_NAME, "Jonathan");

    connection.update(customer);

    connection.delete(customerAddress.getPrimaryKey());
    // end::persist[]
  }
}
