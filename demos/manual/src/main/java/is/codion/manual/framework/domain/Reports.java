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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.framework.domain;

import is.codion.common.db.report.ReportType;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.plugin.jasperreports.JRReport;

import net.sf.jasperreports.engine.JasperPrint;

import java.util.Map;

import static is.codion.common.db.report.ReportType.reportType;
import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.plugin.jasperreports.JRExport.PDF;
import static is.codion.plugin.jasperreports.JasperReports.classPathReport;
import static is.codion.plugin.jasperreports.JasperReports.export;

public final class Reports extends DomainModel {

	static final DomainType DOMAIN = domainType("reports");

	public interface Customer {
		EntityType TYPE = DOMAIN.entityType("reports.customer");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");

		// tag::reportTypes[]
		// Fills to a JasperPrint, which the receiver needs JasperReports to read
		ReportType<Map<String, Object>, JasperPrint> REPORT =
						reportType("customer_report");

		// Fills to a PDF, which the receiver needs nothing at all to read
		ReportType<Map<String, Object>, byte[]> PDF_REPORT =
						reportType("customer_pdf_report");
		// end::reportTypes[]
	}

	public Reports() {
		super(DOMAIN);
		add(customer());
		// tag::addReports[]
		JRReport<JasperPrint> customerReport =
						classPathReport(Reports.class, "customer_report.jasper");

		add(Customer.REPORT, customerReport);
		// The export runs where the report is filled, on the server for a remote
		// connection, so only the PDF crosses the wire. Both report types share
		// the one loaded report and its cache
		add(Customer.PDF_REPORT, export(customerReport, PDF));
		// end::addReports[]
	}

	EntityDefinition customer() {
		return Customer.TYPE.as()
						.attributes(
										Customer.ID.as()
														.primaryKey(),
										Customer.NAME.as()
														.column())
						.build();
	}

	static void fillReports(EntityConnection connection, Map<String, Object> reportParameters) {
		// tag::fillReports[]
		JasperPrint print = connection.report(Customer.REPORT, reportParameters);

		byte[] pdf = connection.report(Customer.PDF_REPORT, reportParameters);
		// end::fillReports[]
	}
}
