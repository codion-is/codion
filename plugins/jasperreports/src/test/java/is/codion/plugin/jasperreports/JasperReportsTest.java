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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.plugin.jasperreports;

import is.codion.common.db.database.Database;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.report.ReportType;
import is.codion.common.utilities.user.User;
import is.codion.dbms.h2.H2DatabaseFactory;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.plugin.jasperreports.TestDomain.Employee;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JasperPrint;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static org.junit.jupiter.api.Assertions.*;

public class JasperReportsTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final String REPORT_PATH = "build/resources/test";

	private static final LocalEntityConnectionProvider CONNECTION_PROVIDER =
					LocalEntityConnectionProvider.builder()
									.database(H2DatabaseFactory.create("jdbc:h2:mem:JasperReportsWrapperTest",
													Database.INIT_SCRIPTS.get()))
									.domain(new TestDomain())
									.user(UNIT_TEST_USER)
									.build();

	@AfterAll
	public static void tearDown() {
		CONNECTION_PROVIDER.close();
	}

	@Test
	void fillJdbcReport() {
		Report.CACHE_REPORTS.set(false);
		Report.REPORT_PATH.set(REPORT_PATH);
		HashMap<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("DEPTNO", asList(10, 20));
		LocalEntityConnection connection = CONNECTION_PROVIDER.connection();
		JasperPrint print = Employee.CLASS_PATH_REPORT.fill(connection.connection(), reportParameters);
		assertNotNull(print);
	}

	@Test
	void fillJdbcReportFailure() {
		Report.CACHE_REPORTS.set(false);
		Report.REPORT_PATH.set(REPORT_PATH);
		HashMap<String, Object> reportParameters = new HashMap<>();
		//DEPTNO is declared java.util.Collection, JasperFillManager throws JRRuntimeException
		reportParameters.put("DEPTNO", "not a collection");
		LocalEntityConnection connection = CONNECTION_PROVIDER.connection();
		ReportException exception = assertThrows(ReportException.class, () ->
						Employee.CLASS_PATH_REPORT.fill(connection.connection(), reportParameters));
		//no engine type may escape fill(), a client throwing it must not need the engine to deserialize it
		assertFalse(exception.getClass().getName().startsWith("net.sf.jasperreports"));
	}

	@Test
	void fillJdbcReportPropagatesLoadFailure() {
		Report.CACHE_REPORTS.set(false);
		Report.REPORT_PATH.set(REPORT_PATH);
		LocalEntityConnection connection = CONNECTION_PROVIDER.connection();
		//load() already throws ReportException, neither it nor fill() may wrap it in another one
		ReportException exception = assertThrows(ReportException.class, () ->
						JasperReports.fileReport("/non-existing.jasper", false)
										.fill(connection.connection(), new HashMap<>()));
		assertNull(exception.getCause());
		assertTrue(exception.getMessage().contains("not found in filesystem"));
	}

	@Test
	void fillDataSourceReport() {
		Report.CACHE_REPORTS.set(false);
		Report.REPORT_PATH.set(REPORT_PATH);
		JRReport<JasperPrint> wrapper = JasperReports.fileReport("employees.jasper");
		JRDataSource dataSource = new JRDataSource() {
			boolean done = false;

			@Override
			public boolean next() {
				if (done) {
					return false;
				}

				return done = true;
			}

			@Override
			public Object getFieldValue(JRField jrField) {
				return null;
			}
		};
		JasperReports.fillReport(wrapper, dataSource);
	}

	@Test
	void exportToPdf() {
		Report.CACHE_REPORTS.set(false);
		Report.REPORT_PATH.set(REPORT_PATH);
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("DEPTNO", asList(10, 20));
		LocalEntityConnection connection = CONNECTION_PROVIDER.connection();

		JRReport<byte[]> pdfReport = JasperReports.export(Employee.CLASS_PATH_REPORT, JRExport.PDF);
		byte[] pdf = pdfReport.fill(connection.connection(), reportParameters);

		//the export is what crosses the wire, a client reading it needs no reporting engine
		assertArrayEquals(new byte[] {'%', 'P', 'D', 'F'}, copyOf(pdf, 4));
	}

	@Test
	void exportToXml() {
		Report.CACHE_REPORTS.set(false);
		Report.REPORT_PATH.set(REPORT_PATH);
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("DEPTNO", asList(10, 20));
		LocalEntityConnection connection = CONNECTION_PROVIDER.connection();

		String xml = JasperReports.export(Employee.CLASS_PATH_REPORT, JRExport.XML)
						.fill(connection.connection(), reportParameters);

		assertTrue(xml.startsWith("<?xml"));
	}

	@Test
	void exportPrintIsTheIdentity() {
		Report.CACHE_REPORTS.set(false);
		Report.REPORT_PATH.set(REPORT_PATH);
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("DEPTNO", asList(10, 20));
		LocalEntityConnection connection = CONNECTION_PROVIDER.connection();

		JasperPrint print = JasperReports.export(Employee.CLASS_PATH_REPORT, JRExport.PRINT)
						.fill(connection.connection(), reportParameters);

		assertNotNull(print);
	}

	@Test
	void exportSharesTheReportCache() {
		Report.CACHE_REPORTS.set(true);
		Report.REPORT_PATH.set(REPORT_PATH);
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("DEPTNO", asList(10, 20));
		LocalEntityConnection connection = CONNECTION_PROVIDER.connection();

		JRReport<JasperPrint> report = JasperReports.fileReport("/employees.jasper", true);
		JRReport<byte[]> pdf = JasperReports.export(report, JRExport.PDF);
		JRReport<String> xml = JasperReports.export(report, JRExport.XML);

		report.clearCache();
		assertFalse(pdf.cached());
		pdf.fill(connection.connection(), reportParameters);
		//one loaded report behind both exports, and the server clears the cache
		//of every report it hosts, so clearCache() must reach through the export
		assertTrue(report.cached());
		assertTrue(xml.cached());
		xml.clearCache();
		assertFalse(report.cached());
		assertFalse(pdf.cached());
	}

	@Test
	void exportFilledReport() throws Exception {
		Report.CACHE_REPORTS.set(false);
		Report.REPORT_PATH.set(REPORT_PATH);
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("DEPTNO", asList(10, 20));
		LocalEntityConnection connection = CONNECTION_PROVIDER.connection();
		JasperPrint print = Employee.CLASS_PATH_REPORT.fill(connection.connection(), reportParameters);

		assertSame(print, JasperReports.export(print, JRExport.PRINT));
		assertArrayEquals(new byte[] {'%', 'P', 'D', 'F'}, copyOf(JasperReports.export(print, JRExport.PDF), 4));
	}

	@Test
	void exportFailure() {
		Report.CACHE_REPORTS.set(false);
		Report.REPORT_PATH.set(REPORT_PATH);
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("DEPTNO", asList(10, 20));
		LocalEntityConnection connection = CONNECTION_PROVIDER.connection();

		JRExport<byte[]> failing = print -> {
			throw new JRException("Missing export extension");
		};
		//a failing export, a missing exporter extension being the likely one, must not
		//let a JasperReports exception escape any more than a failing fill() may
		ReportException exception = assertThrows(ReportException.class, () ->
						JasperReports.export(Employee.CLASS_PATH_REPORT, failing)
										.fill(connection.connection(), reportParameters));
		assertInstanceOf(JRException.class, exception.getCause());
		assertThrows(ReportException.class, () ->
						JasperReports.export(Employee.CLASS_PATH_REPORT.fill(connection.connection(), reportParameters), failing));
	}

	@Test
	void exportedReportThroughEntityConnection() {
		Report.CACHE_REPORTS.set(false);
		Report.REPORT_PATH.set(REPORT_PATH);
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("DEPTNO", asList(10, 20));

		//the report type names a report and a byte[], nothing of JasperReports
		byte[] pdf = CONNECTION_PROVIDER.connection().report(Employee.PDF_REPORT, reportParameters);

		assertArrayEquals(new byte[] {'%', 'P', 'D', 'F'}, copyOf(pdf, 4));
	}

	@Test
	void fillJdbcReportInvalidReport() {
		Report.CACHE_REPORTS.set(false);
		Report.REPORT_PATH.set(REPORT_PATH);
		ReportType<Object, Object> nonExisting = ReportType.reportType("test");
		assertThrows(IllegalArgumentException.class, () -> CONNECTION_PROVIDER.connection().report(nonExisting, new HashMap<>()));
	}

	@Test
	void urlReport() throws Exception {
		Report.CACHE_REPORTS.set(false);
		Report.REPORT_PATH.set("http://localhost:1234");
		Server server = new Server(1234);
		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setBaseResource(ResourceFactory.of(resourceHandler).newResource(Path.of(REPORT_PATH)));
		server.setHandler(resourceHandler);
		try {
			server.start();
			Map<String, Object> reportParameters = new HashMap<>();
			reportParameters.put("DEPTNO", asList(10, 20));
			LocalEntityConnection connection = CONNECTION_PROVIDER.connection();
			Employee.FILE_REPORT.fill(connection.connection(), reportParameters);
		}
		finally {
			server.stop();
		}
	}

	@Test
	void classPathReport() {
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("DEPTNO", asList(10, 20));
		LocalEntityConnection connection = CONNECTION_PROVIDER.connection();
		Employee.CLASS_PATH_REPORT.fill(connection.connection(), reportParameters);

		assertThrows(ReportException.class, () -> new ClassPathJRReport(JasperReportsTest.class, "non-existing.jasper").load());
	}

	@Test
	void fileReport() {
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("DEPTNO", asList(10, 20));
		LocalEntityConnection connection = CONNECTION_PROVIDER.connection();
		Employee.FILE_REPORT.fill(connection.connection(), reportParameters);
		assertTrue(Employee.FILE_REPORT.cached());
		Employee.FILE_REPORT.clearCache();
		assertFalse(Employee.FILE_REPORT.cached());

		assertThrows(ReportException.class, () -> new FileJRReport("/non-existing.jasper", false).load());
	}
}
