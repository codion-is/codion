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
package is.codion.demos.chinook.javadoc;

import is.codion.demos.chinook.domain.ChinookImpl;
import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.report.ReportType;
import is.codion.plugin.jasperreports.JRExport;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static is.codion.demos.chinook.domain.api.Chinook.DOMAIN;
import static is.codion.framework.domain.report.ReportType.reportType;
import static is.codion.plugin.jasperreports.JRExport.PDF;
import static is.codion.plugin.jasperreports.JRExport.SERIALIZED;
import static is.codion.plugin.jasperreports.JasperReports.*;

/**
 * The JasperReports plugin javadoc snippets, for {@code JasperReports}, {@code JRExport} and {@code JRReport},
 * each the region of the same name.
 */
final class JasperReportsSnippets {

	static final class Reports extends DomainModel {

		Reports() {
			super(DOMAIN);
		}

		void pdfReport() {
			ReportType<Map<String, Object>, byte[]> REPORT = reportType("customer_report"); // @start region=export

			add(REPORT, export(classPathReport(ChinookImpl.class, "customer_report.jasper"), PDF)); // @end
		}

		void printReport() {
			ReportType<Map<String, Object>, JasperPrint> REPORT = reportType("customer_report"); // @start region=print

			add(REPORT, classPathReport(ChinookImpl.class, "customer_report.jasper")); // @end
		}

		void serializedReport(EntityConnection connection, Map<String, Object> parameters) throws Exception {
			ReportType<Map<String, Object>, byte[]> REPORT = reportType("customer_report"); // @start region=serialized

			add(REPORT, export(classPathReport(ChinookImpl.class, "customer_report.jasper"), SERIALIZED));

			//client side, with the engine on hand
			JasperPrint print = loadPrint(connection.report(REPORT, parameters)); // @end
		}
	}

	void loadPrintUsage(EntityConnection connection, Map<String, Object> parameters) throws Exception {
		JasperPrint print = loadPrint(connection.report(Customer.REPORT, parameters)); // @start region=loadPrint @end
	}

	void xlsx() {
		JRExport<byte[]> xlsx = print -> { // @start region=xlsx
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			JRXlsxExporter exporter = new JRXlsxExporter();
			exporter.setExporterInput(new SimpleExporterInput(print));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(bytes));
			exporter.exportReport();

			return bytes.toByteArray();
		}; // @end
	}
}
