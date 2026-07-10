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

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;
import is.codion.common.rmi.client.Clients;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.http.HttpEntityConnection;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerConfiguration;
import is.codion.framework.servlet.EntityService;
import is.codion.framework.servlet.EntityServiceFactory;
import is.codion.plugin.jasperreports.TestDomain.Employee;

import net.sf.jasperreports.engine.JasperPrint;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The acceptance test for exporting a report to bytes: a real {@link EntityServer} fills and exports a
 * JasperReports report, a remote client receives the bytes. The client names no JasperReports type, the
 * PDF it receives is no JasperReports type, and neither is anything in a report failure, so a client
 * without the reporting engine could run this exact code. The engine lives only on the server.
 */
public final class JasperReportsWireTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static EntityServer server;
	private static EntityConnection serial;
	private static EntityConnection json;

	@BeforeAll
	public static void setUp() throws Exception {
		Clients.SERVER_HOSTNAME.set("localhost");
		Clients.TRUSTSTORE.set("../../framework/server/src/main/config/truststore.jks");
		Clients.resolveTrustStore();
		Report.REPORT_PATH.set("build/resources/test");
		HttpEntityConnection.SECURE.set(true);
		EntityService.SERIALIZATION.set(true);
		EntityService.KEYSTORE_PATH.set("../../framework/server/src/main/config/keystore.jks");
		EntityService.KEYSTORE_PASSWORD.set("crappypass");

		server = EntityServer.startServer(EntityServerConfiguration.builder()
						.port(3423)
						.registryPort(3421)
						.adminPort(3423)
						.database(Database.instance())
						.domainClasses(singletonList(TestDomain.class.getName()))
						.sslEnabled(false)
						.auxiliaryServerFactory(singletonList(EntityServiceFactory.class.getName()))
						.objectInputFilterFactoryRequired(false)
						.build());
		serial = connection(false);
		json = connection(true);
	}

	private static EntityConnection connection(boolean json) {
		return HttpEntityConnection.builder()
						.json(json)
						.domain(TestDomain.DOMAIN)
						.user(UNIT_TEST_USER)
						.clientType("JasperReportsWireTest")
						.clientId(UUID.randomUUID())
						.build();
	}

	@AfterAll
	public static void tearDown() {
		if (serial != null) {
			serial.close();
		}
		if (json != null) {
			json.close();
		}
		if (server != null) {
			server.shutdown();
		}
		Clients.SERVER_HOSTNAME.set(null);
		HttpEntityConnection.SECURE.set(true);
	}

	@Test
	void pdfReportCrossesTheWireAsBytes() {
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("DEPTNO", asList(10, 20));

		//PDF_REPORT is a ReportType<Map<String, Object>, byte[]>, the client names no JasperReports type.
		//The serial tier serializes the bytes, the JSON tier base64 encodes them
		assertArrayEquals(new byte[] {'%', 'P', 'D', 'F'}, copyOf(serial.report(Employee.PDF_REPORT, reportParameters), 4));
		assertArrayEquals(new byte[] {'%', 'P', 'D', 'F'}, copyOf(json.report(Employee.PDF_REPORT, reportParameters), 4));
	}

	@Test
	void serializedPrintCrossesTheJsonWire() {
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("DEPTNO", asList(10, 20));

		//a JasperPrint can not cross a JSON connection, but its bytes can, so a client with the engine
		//keeps a JasperPrint report over JSON, reconstructing it from the bytes with a JRViewer to follow
		byte[] bytes = json.report(Employee.SERIALIZED_REPORT, reportParameters);
		JasperPrint print = JasperReports.loadPrint(bytes);

		assertNotNull(print.getName());
		assertFalse(print.getPages().isEmpty());
	}

	@Test
	void reportFailureCarriesNoEngineType() {
		Map<String, Object> reportParameters = new HashMap<>();
		//DEPTNO is declared java.util.Collection, the server throws deep in the engine
		reportParameters.put("DEPTNO", "not a collection");

		//the serial tier serializes the exception, so the ReportException crosses intact
		ReportException serialException = assertThrows(ReportException.class, () ->
						serial.report(Employee.PDF_REPORT, reportParameters));
		assertNoEngineType(serialException);

		//the json tier reconstructs the exception from an error envelope keyed by a closed set of kinds;
		//a ReportException, being none of them, generalizes to a DatabaseException, still no engine type
		Exception jsonException = assertThrows(DatabaseException.class, () ->
						json.report(Employee.PDF_REPORT, reportParameters));
		assertNoEngineType(jsonException);
	}

	private static void assertNoEngineType(Throwable throwable) {
		for (Throwable t = throwable; t != null; t = t.getCause()) {
			assertFalse(t.getClass().getName().startsWith("net.sf.jasperreports"),
							"engine type crossed the wire: " + t.getClass().getName());
		}
	}
}
