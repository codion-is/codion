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
 * Copyright (c) 2017 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.db.report;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static is.codion.common.db.report.ReportType.reportType;
import static org.junit.jupiter.api.Assertions.*;

public class ReportTest {

	@Test
	void reportPathNotSpecified() {
		Report.REPORT_PATH.set(null);
		assertThrows(NoSuchElementException.class, Report::reportPath);
	}

	@Test
	void reportPath() {
		final String path = "test/path";
		Report.REPORT_PATH.set(path);
		assertEquals(path, Report.reportPath());
	}

	@Test
	void reportTypeEqualsByName() {
		assertNotEquals(reportType("name"), reportType("another"));
		//the json tier reconstructs a report type from the name alone and looks it up
		//in the domain, so a report type must equal any other one sharing its name,
		//regardless of the parameter and result types it was declared with
		ReportType<String, String> declared = reportType("report");
		ReportType<Object, Object> reconstructed = reportType("report");
		assertEquals(declared, reconstructed);
		assertEquals(declared.hashCode(), reconstructed.hashCode());
	}
}
