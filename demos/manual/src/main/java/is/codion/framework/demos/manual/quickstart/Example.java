/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.quickstart;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.StringFactory;
import is.codion.framework.domain.entity.test.EntityTestUnit;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static is.codion.framework.db.condition.Condition.column;
import static is.codion.framework.db.condition.Condition.foreignKey;
import static is.codion.framework.demos.manual.quickstart.Example.Store.*;
import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.entity.KeyGenerator.automatic;
import static is.codion.framework.domain.property.Property.*;
import static java.util.UUID.randomUUID;

public final class Example {

  // tag::store[]
  // tag::storeDomain[]
  public static class Store extends DefaultDomain {

    public static final DomainType DOMAIN = domainType(Store.class);

    public Store() {
      super(DOMAIN);
      customer();
      address();
      customerAddress();
    }
    // end::storeDomain[]

    // tag::customer[]
    public interface Customer {
      EntityType TYPE = DOMAIN.entityType("store.customer");

      Column<String> ID = TYPE.stringColumn("id");
      Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
      Column<String> LAST_NAME = TYPE.stringColumn("last_name");
    }

    void customer() {
      add(definition(
              primaryKeyProperty(Customer.ID),
              columnProperty(Customer.FIRST_NAME, "First name")
                      .nullable(false)
                      .maximumLength(40),
              columnProperty(Customer.LAST_NAME, "Last name")
                      .nullable(false)
                      .maximumLength(40))
              .keyGenerator(new CustomerKeyGenerator())
              .stringFactory(StringFactory.builder()
                      .value(Customer.LAST_NAME)
                      .text(", ")
                      .value(Customer.FIRST_NAME)
                      .build()));
    }

    private static final class CustomerKeyGenerator implements KeyGenerator {
      @Override
      public void beforeInsert(Entity entity, DatabaseConnection connection) {
        entity.put(Customer.ID, randomUUID().toString());
      }
    }

    // end::customer[]
    // tag::address[]
    public interface Address {
      EntityType TYPE = DOMAIN.entityType("store.address");

      Column<Integer> ID = TYPE.integerColumn("id");
      Column<String> STREET = TYPE.stringColumn("street");
      Column<String> CITY = TYPE.stringColumn("city");
    }

    void address() {
      add(definition(
              primaryKeyProperty(Address.ID),
              columnProperty(Address.STREET, "Street")
                      .nullable(false)
                      .maximumLength(120),
              columnProperty(Address.CITY, "City")
                      .nullable(false)
                      .maximumLength(50))
              .keyGenerator(automatic("store.address"))
              .stringFactory(StringFactory.builder()
                      .value(Address.STREET)
                      .text(", ")
                      .value(Address.CITY)
                      .build()));
    }
    // end::address[]
    // tag::customerAddress[]
    public interface CustomerAddress {
      EntityType TYPE = DOMAIN.entityType("store.customer_address");

      Column<Integer> ID = TYPE.integerColumn("id");
      Column<String> CUSTOMER_ID = TYPE.stringColumn("customer_id");
      Column<Integer> ADDRESS_ID = TYPE.integerColumn("address_id");

      ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk", CUSTOMER_ID, Customer.ID);
      ForeignKey ADDRESS_FK = TYPE.foreignKey("address_fk", ADDRESS_ID, Address.ID);
    }

    void customerAddress() {
      add(definition(
              primaryKeyProperty(CustomerAddress.ID),
              columnProperty(CustomerAddress.CUSTOMER_ID)
                      .nullable(false),
              foreignKeyProperty(CustomerAddress.CUSTOMER_FK, "Customer"),
              columnProperty(CustomerAddress.ADDRESS_ID)
                      .nullable(false),
              foreignKeyProperty(CustomerAddress.ADDRESS_FK, "Address"))
              .keyGenerator(automatic("store.customer_address"))
              .caption("Customer address"));
    }
    // end::customerAddress[]
  }
  // end::store[]

  static void customerPanel() {
    // tag::customerPanel[]
    class CustomerEditPanel extends EntityEditPanel {

      public CustomerEditPanel(SwingEntityEditModel editModel) {
        super(editModel);
      }

      @Override
      protected void initializeUI() {
        setInitialFocusAttribute(Customer.FIRST_NAME);
        createTextField(Customer.FIRST_NAME);
        createTextField(Customer.LAST_NAME);
        addInputPanel(Customer.FIRST_NAME);
        addInputPanel(Customer.LAST_NAME);
      }
    }

    EntityConnectionProvider connectionProvider =
            LocalEntityConnectionProvider.builder()
                    .domain(new Store())
                    .user(User.parse("scott:tiger"))
                    .build();

    SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connectionProvider);

    EntityPanel customerPanel = new EntityPanel(customerModel,
            new CustomerEditPanel(customerModel.editModel()));
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
            new CustomerAddressEditPanel(customerAddressModel.editModel()));

    customerPanel.addDetailPanel(customerAddressPanel);

    //lazy initialization of UI components
    customerPanel.initialize();

    //populate the model with data from the database
    customerModel.tableModel().refresh();

    Dialogs.componentDialog(customerPanel)
            .title("Customers")
            .show();
    // end::detailPanel[]
  }

  static void domainModelTest() {
    // tag::domainModelTest[]
    class StoreTest extends EntityTestUnit {

      public StoreTest() {
        super(new Store(), User.parse("scott:tiger"));
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
                    Database.instance(), domain, User.parse("scott:tiger"));

    //select customer where last name = Doe
    Entity johnDoe = connection.selectSingle(column(Customer.LAST_NAME).equalTo("Doe"));

    //select all customer addresses
    List<Entity> customerAddresses = //where customer = john doe
            connection.select(foreignKey(CustomerAddress.CUSTOMER_FK).equalTo(johnDoe));

    Entity customerAddress = customerAddresses.get(0);

    Entity address = customerAddress.referencedEntity(CustomerAddress.ADDRESS_FK);

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
                    Database.instance(), domain, User.parse("scott:tiger"));

    Entities entities = domain.entities();

    Entity customer = entities.builder(Customer.TYPE)
            .with(Customer.FIRST_NAME, "John")
            .with(Customer.LAST_NAME, "Doe")
            .build();

    connection.insert(customer);

    Entity address = entities.builder(Address.TYPE)
            .with(Address.STREET, "Elm Street 321")
            .with(Address.CITY, "Syracuse")
            .build();

    connection.insert(address);

    Entity customerAddress = entities.builder(CustomerAddress.TYPE)
            .with(CustomerAddress.CUSTOMER_FK, customer)
            .with(CustomerAddress.ADDRESS_FK, address)
            .build();

    connection.insert(customerAddress);

    customer.put(Customer.FIRST_NAME, "Jonathan");

    connection.update(customer);

    connection.delete(customerAddress.primaryKey());
    // end::persist[]
  }
}
