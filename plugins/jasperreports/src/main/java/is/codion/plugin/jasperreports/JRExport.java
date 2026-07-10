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
package is.codion.plugin.jasperreports;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.util.JRSaver;

import java.io.ByteArrayOutputStream;

/**
 * Exports a filled report.
 * <p>Applied by {@link JasperReports#export(JRReport, JRExport)}, which runs the export wherever the
 * report is filled. Filling a report through an {@code EntityConnection} runs it on the server, so a
 * report exported to {@link #PDF} reaches the client as bytes, which no reporting engine is required
 * to read.
 * {@snippet :
 * ReportType<Map<String, Object>, byte[]> REPORT = reportType("customer_report");
 *
 * add(REPORT, export(classPathReport(Store.class, "customer_report.jasper"), PDF));
 *}
 * <p>Exports beyond the ones defined here are lambdas, JasperReports exporters all following
 * the same shape:
 * {@snippet :
 * JRExport<byte[]> xlsx = print -> {
 *   ByteArrayOutputStream bytes = new ByteArrayOutputStream();
 *   JRXlsxExporter exporter = new JRXlsxExporter();
 *   exporter.setExporterInput(new SimpleExporterInput(print));
 *   exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(bytes));
 *   exporter.exportReport();
 *
 *   return bytes.toByteArray();
 * };
 *}
 * @param <R> the export result type
 */
public interface JRExport<R> {

	/**
	 * The identity export, the filled report itself.
	 * <p>Requires JasperReports wherever the result is received, {@link JasperPrint} being a
	 * JasperReports type, serialized when the report is filled through a remote connection.
	 */
	JRExport<JasperPrint> PRINT = print -> print;

	/**
	 * Serializes the filled report to bytes, reconstructed with {@link JasperReports#loadPrint(byte[])}.
	 * <p>The {@link JasperPrint} itself, unlike {@link #PRINT}, but carried as a {@code byte[]}, which any
	 * connection transfers, a JSON one included, where a {@link JasperPrint} cannot go. A client with the
	 * reporting engine, one displaying reports with a {@code JRViewer}, can therefore keep filling them to
	 * a {@link JasperPrint} over any connection, reconstructing it from the bytes:
	 * {@snippet :
	 * ReportType<Map<String, Object>, byte[]> REPORT = reportType("customer_report");
	 *
	 * add(REPORT, export(classPathReport(Store.class, "customer_report.jasper"), SERIALIZED));
	 *
	 * //client side, with the engine on hand
	 * JasperPrint print = loadPrint(connection.report(REPORT, parameters));
	 *}
	 * <p>The bytes are the report's own Java serialization, so a client reconstructing them needs the
	 * {@code net.sf.jasperreports.engine.**} classes allowed by any deserialization filter it applies,
	 * the same classes a server loading a {@code .jasper} file needs.
	 * @see JasperReports#loadPrint(byte[])
	 */
	JRExport<byte[]> SERIALIZED = print -> {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		JRSaver.saveObject(print, bytes);

		return bytes.toByteArray();
	};

	/**
	 * Exports to PDF.
	 * <p>Requires the {@code net.sf.jasperreports:jasperreports-pdf} artifact, which is not a
	 * dependency of this plugin. It registers itself through a {@code jasperreports_extension.properties}
	 * resource, which is scanned off the classpath, so nothing requires its module and it is neither
	 * resolved on the module path nor included in a jlink image unless named explicitly, via
	 * {@code --add-modules net.sf.jasperreports.pdf}. A missing extension surfaces as a
	 * {@link is.codion.common.db.report.ReportException} when the report is filled.
	 */
	JRExport<byte[]> PDF = JasperExportManager::exportReportToPdf;

	/**
	 * Exports to the JasperReports XML print format.
	 */
	JRExport<String> XML = JasperExportManager::exportReportToXml;

	/**
	 * @param print the filled report to export
	 * @return the exported report
	 * @throws JRException in case of an exception
	 */
	R export(JasperPrint print) throws JRException;
}
