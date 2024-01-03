/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.ReportException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.framework.demos.manual.store.model.CustomerEditModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.plugin.jasperreports.JRReport;
import is.codion.plugin.jasperreports.JasperReports;
import is.codion.plugin.jasperreports.JasperReportsDataSource;

import net.sf.jasperreports.engine.JasperPrint;

import java.util.Iterator;
import java.util.UUID;

import static is.codion.framework.domain.entity.condition.Condition.all;
import static is.codion.plugin.jasperreports.JasperReports.fileReport;

public final class Misc {

  static void jasperReports() throws DatabaseException, ReportException {
    EntityConnectionProvider connectionProvider =
            EntityConnectionProvider.builder()
                    .domainType(Store.DOMAIN)
                    .user(User.parse("scott:tiger"))
                    .clientTypeId("StoreMisc")
                    .build();

    // tag::jasperReportDataSource[]
    EntityConnection connection = connectionProvider.connection();

    EntityDefinition customerDefinition =
            connection.entities().definition(Customer.TYPE);

    Iterator<Entity> customerIterator =
            connection.select(all(Customer.TYPE)).iterator();

    JasperReportsDataSource<Entity> dataSource =
            new JasperReportsDataSource<>(customerIterator,
                    (entity, reportField) ->
                            entity.get(customerDefinition.attributes().get(reportField.getName())));

    JRReport customerReport = fileReport("reports/customer.jasper");

    JasperPrint jasperPrint = JasperReports.fillReport(customerReport, dataSource);
    // end::jasperReportDataSource[]
  }

  public static void main(String[] args) throws DatabaseException, ValidationException {
    // tag::editModel[]
    EntityConnectionProvider connectionProvider =
            EntityConnectionProvider.builder()
                    .domainType(Store.DOMAIN)
                    .user(User.parse("scott:tiger"))
                    .clientTypeId("StoreMisc")
                    .build();

    CustomerEditModel editModel = new CustomerEditModel(connectionProvider);

    editModel.put(Customer.ID, UUID.randomUUID().toString());
    editModel.put(Customer.FIRST_NAME, "Björn");
    editModel.put(Customer.LAST_NAME, "Sigurðsson");
    editModel.put(Customer.ACTIVE, true);

    //inserts and returns the inserted entity
    Entity customer = editModel.insert();

    //modify some values
    editModel.put(Customer.FIRST_NAME, "John");
    editModel.put(Customer.LAST_NAME, "Doe");

    //updates and returns the updated entity
    customer = editModel.update();

    //deletes the active entity
    editModel.delete();
    // end::editModel[]
  }
}
