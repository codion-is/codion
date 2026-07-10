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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.plugin.jasperreports;

import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.util.JRLoader;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Factory for {@link Report} based on JasperReports.
 */
public final class JasperReports {

	private JasperReports() {}

	/**
	 * Instantiates a JRReport for a classpath based report.
	 * Note that classpath reports are always cached.
	 * @param resourceClass the class owning the report resource
	 * @param reportPath the report path, relative to the resource class
	 * @return a report wrapper
	 */
	public static JRReport<JasperPrint> classPathReport(Class<?> resourceClass, String reportPath) {
		return new ClassPathJRReport(resourceClass, reportPath);
	}

	/**
	 * Instantiates a JRReport for a file based report, either loaded from a URL or from the filesystem.
	 * @param reportPath the report path, relative to the central report path {@link Report#REPORT_PATH}
	 * @return a report wrapper
	 */
	public static JRReport<JasperPrint> fileReport(String reportPath) {
		return fileReport(reportPath, Report.CACHE_REPORTS.getOrThrow());
	}

	/**
	 * Instantiates a JRReport for a file based report, either loaded from a URL or from the filesystem.
	 * @param reportPath the report path, relative to the central report path {@link Report#REPORT_PATH}
	 * @param cacheReport if true the report is only loaded once and cached
	 * @return a report wrapper
	 */
	public static JRReport<JasperPrint> fileReport(String reportPath, boolean cacheReport) {
		return new FileJRReport(reportPath, cacheReport);
	}

	/**
	 * Returns a report producing the result of the given export when filled, instead of a {@link JasperPrint}.
	 * <p>The export runs wherever the report is filled, on the server in case of a remote connection, so a
	 * report exported to {@link JRExport#PDF} reaches the client as bytes, requiring no reporting engine there.
	 * {@snippet :
	 * ReportType<Map<String, Object>, byte[]> REPORT = reportType("customer_report");
	 *
	 * add(REPORT, export(classPathReport(Store.class, "customer_report.jasper"), PDF));
	 *}
	 * <p>The loaded report and its cache are shared with the given report, so exporting the same report to
	 * more than one format loads and caches it once.
	 * @param report the report to export
	 * @param export the export to apply to the filled report
	 * @param <R> the export result type
	 * @return a report producing the result of the given export
	 * @see JRExport
	 */
	public static <R> JRReport<R> export(JRReport<JasperPrint> report, JRExport<R> export) {
		return new ExportingJRReport<>(report, export);
	}

	/**
	 * Exports the given filled report, for a {@link JasperPrint} already at hand, such as one
	 * returned by {@link #fillReport(JRReport, JRDataSource)}.
	 * @param print the filled report to export
	 * @param export the export to apply
	 * @param <R> the export result type
	 * @return the exported report
	 * @throws ReportException in case of an exception
	 * @see JRExport
	 */
	public static <R> R export(JasperPrint print, JRExport<R> export) {
		requireNonNull(print);
		requireNonNull(export);
		try {
			return export.export(print);
		}
		catch (Exception e) {
			throw reportException(e);
		}
	}

	/**
	 * Reconstructs a {@link JasperPrint} from the bytes {@link JRExport#SERIALIZED} produced, for a client
	 * receiving them from a report exported to it, letting a client with the reporting engine keep a
	 * {@link JasperPrint} report over a connection which can not transfer one, such as a JSON one.
	 * {@snippet :
	 * JasperPrint print = loadPrint(connection.report(REPORT, parameters));
	 *}
	 * @param bytes the bytes {@link JRExport#SERIALIZED} produced
	 * @return the reconstructed report
	 * @throws ReportException in case of an exception
	 * @see JRExport#SERIALIZED
	 */
	public static JasperPrint loadPrint(byte[] bytes) {
		requireNonNull(bytes);
		try {
			return (JasperPrint) JRLoader.loadObject(new ByteArrayInputStream(bytes));
		}
		catch (Exception e) {
			//the cause is kept, unlike the fill and export paths, this reconstructs the report on the
			//client, the last step, its failure crossing no wire to a client without the engine
			throw new ReportException(e);
		}
	}

	/**
	 * Fills the report using the data source wrapped by the given data wrapper
	 * @param report the report to fill
	 * @param dataSource the data provider to use for the report generation
	 * @return a filled report ready for display
	 * @throws ReportException in case of an exception
	 */
	public static JasperPrint fillReport(JRReport<JasperPrint> report, JRDataSource dataSource) {
		return fillReport(report, dataSource, new HashMap<>());
	}

	/**
	 * Fills the report using the given data.
	 * @param report the report to fill
	 * @param dataSource the data provider to use for the report generation
	 * @param reportParameters the report parameters, must be modifiable
	 * @return a filled report ready for display
	 * @throws ReportException in case of an exception
	 */
	public static JasperPrint fillReport(JRReport<JasperPrint> report, JRDataSource dataSource,
																			 Map<String, Object> reportParameters) {
		requireNonNull(report);
		requireNonNull(dataSource);
		requireNonNull(reportParameters);
		try {
			return JasperFillManager.fillReport(report.load(), reportParameters, dataSource);
		}
		catch (Exception e) {
			throw reportException(e);
		}
	}

	/**
	 * Returns the given exception as a {@link ReportException} carrying no cause, only its message
	 * and stack trace. Filling a report or exporting one runs the reporting engine, so the exception
	 * behind a failure, or the one a load failure chains, is a JasperReports type. Such a cause reaching
	 * the client would require the engine there to deserialize the failure of a report the client need
	 * never have heard of, exactly the coupling exporting the result removes. The engine stack trace,
	 * being strings, is kept, and the server logs the exception in full before rethrowing.
	 * <p>The dropped cause's message is folded into the message, being the message a client sees, the
	 * outer exception often naming only the operation, the cause naming what actually went wrong.
	 */
	static ReportException reportException(Exception exception) {
		if (exception instanceof ReportException && exception.getCause() == null) {
			//already the exception this contract throws, carrying no engine type
			return (ReportException) exception;
		}
		ReportException reportException = new ReportException(message(exception));
		reportException.setStackTrace(exception.getStackTrace());

		return reportException;
	}

	private static String message(Throwable exception) {
		String message = exception.getMessage();
		Throwable cause = exception.getCause();
		if (cause == null || cause.getMessage() == null) {
			return message;
		}
		if (message == null) {
			return cause.getMessage();
		}

		return message.contains(cause.getMessage()) ? message : message + ": " + cause.getMessage();
	}
}
