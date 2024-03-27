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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.plugin.jasperreports;

import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Factory for {@link Report} based on JasperReports.
 */
public final class JasperReports {

	private JasperReports() {}

	/**
	 * @param name the report name
	 * @return a JRReport
	 */
	public static JRReportType reportType(String name) {
		return new DefaultJRReportType(name);
	}

	/**
	 * Instantiates a JRReport for a classpath based report.
	 * Note that classpath reports are always cached.
	 * @param resourceClass the class owning the report resource
	 * @param reportPath the report path, relative to the resource class
	 * @return a report wrapper
	 */
	public static JRReport classPathReport(Class<?> resourceClass, String reportPath) {
		return new ClassPathJRReport(resourceClass, reportPath);
	}

	/**
	 * Instantiates a JRReport for a file based report, either loaded from a URL or from the filesystem.
	 * @param reportPath the report path, relative to the central report path {@link Report#REPORT_PATH}
	 * @return a report wrapper
	 */
	public static JRReport fileReport(String reportPath) {
		return fileReport(reportPath, Report.CACHE_REPORTS.get());
	}

	/**
	 * Instantiates a JRReport for a file based report, either loaded from a URL or from the filesystem.
	 * @param reportPath the report path, relative to the central report path {@link Report#REPORT_PATH}
	 * @param cacheReport if true the report is only loaded once and cached
	 * @return a report wrapper
	 */
	public static JRReport fileReport(String reportPath, boolean cacheReport) {
		return new FileJRReport(reportPath, cacheReport);
	}

	/**
	 * Fills the report using the data source wrapped by the given data wrapper
	 * @param report the report to fill
	 * @param dataSource the data provider to use for the report generation
	 * @return a filled report ready for display
	 * @throws ReportException in case of an exception
	 */
	public static JasperPrint fillReport(JRReport report, JRDataSource dataSource) throws ReportException {
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
	public static JasperPrint fillReport(JRReport report, JRDataSource dataSource,
																			 Map<String, Object> reportParameters) throws ReportException {
		requireNonNull(report, "report");
		requireNonNull(dataSource, "dataSource");
		requireNonNull(reportParameters, "reportParameters");
		try {
			return JasperFillManager.fillReport(report.load(), reportParameters, dataSource);
		}
		catch (RuntimeException re) {
			throw re;
		}
		catch (Exception e) {
			throw new ReportException(e);
		}
	}
}
