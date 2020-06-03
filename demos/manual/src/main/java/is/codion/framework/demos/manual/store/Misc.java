/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.reports.ReportException;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.EntityConnectionProviders;
import is.codion.framework.demos.manual.store.domain.Store;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.framework.demos.manual.store.model.CustomerEditModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.plugin.jasperreports.model.JasperReports;
import is.codion.plugin.jasperreports.model.JasperReportsDataSource;

import net.sf.jasperreports.engine.JasperPrint;

import java.util.Iterator;
import java.util.UUID;

import static is.codion.framework.db.condition.Conditions.selectCondition;

public final class Misc {

  static void jasperReports() throws DatabaseException, ReportException {
   EntityConnectionProvider connectionProvider =
            EntityConnectionProviders.connectionProvider()
                    .setDomainClassName(Store.class.getName())
                    .setUser(Users.parseUser("scott:tiger"))
                    .setClientTypeId("StoreMisc");

   // tag::jasperReportDataSource[]
    EntityConnection connection = connectionProvider.getConnection();

    Iterator<Entity> customerIterator =
            connection.select(selectCondition(Customer.TYPE)).iterator();

    JasperReportsDataSource<Entity> dataSource = new JasperReportsDataSource<>(customerIterator,
            (entity, reportField) -> entity.get(Customer.TYPE.objectAttribute(reportField.getName())));

    JasperPrint jasperPrint = JasperReports.fillReport(Store.CUSTOMER_REPORT, dataSource);
    // end::jasperReportDataSource[]
  }

  public static void main(String[] args) throws DatabaseException, ValidationException {
    // tag::editModel[]
    EntityConnectionProvider connectionProvider =
            EntityConnectionProviders.connectionProvider()
                    .setDomainClassName(Store.class.getName())
                    .setUser(Users.parseUser("scott:tiger"))
                    .setClientTypeId("StoreMisc");

    CustomerEditModel editModel = new CustomerEditModel(connectionProvider);

    editModel.put(Customer.ID, UUID.randomUUID().toString());
    editModel.put(Customer.FIRST_NAME, "Björn");
    editModel.put(Customer.LAST_NAME, "Sigurðsson");
    editModel.put(Customer.IS_ACTIVE, true);

    //inserts and returns the inserted entity
    Entity customer = editModel.insert();

    //modify some property values
    editModel.put(Customer.FIRST_NAME, "John");
    editModel.put(Customer.LAST_NAME, "Doe");

    //updates and returns the updated entity
    customer = editModel.update();

    //deletes the active entity
    editModel.delete();
    // end::editModel[]
  }
}
