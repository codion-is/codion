/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.manual.store;

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

	static void jasperReports() {
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

	public static void main(String[] args) throws ValidationException {
		// tag::editModel[]
		EntityConnectionProvider connectionProvider =
						EntityConnectionProvider.builder()
										.domainType(Store.DOMAIN)
										.user(User.parse("scott:tiger"))
										.clientTypeId("StoreMisc")
										.build();

		CustomerEditModel editModel = new CustomerEditModel(connectionProvider);

		editModel.value(Customer.ID).set(UUID.randomUUID().toString());
		editModel.value(Customer.FIRST_NAME).set("Björn");
		editModel.value(Customer.LAST_NAME).set("Sigurðsson");
		editModel.value(Customer.ACTIVE).set(true);

		//inserts and returns the inserted entity
		Entity customer = editModel.insert();

		//modify some values
		editModel.value(Customer.FIRST_NAME).set("John");
		editModel.value(Customer.LAST_NAME).set("Doe");

		//updates and returns the updated entity
		customer = editModel.update();

		//deletes the active entity
		editModel.delete();
		// end::editModel[]
	}
}
