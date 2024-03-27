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

import is.codion.common.db.report.ReportException;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

import static java.util.Objects.requireNonNull;

final class ClassPathJRReport extends AbstractJRReport {

	private final Class<?> resourceClass;

	ClassPathJRReport(Class<?> resourceClass, String reportPath) {
		super(reportPath, true);
		this.resourceClass = requireNonNull(resourceClass);
	}

	@Override
	public JasperReport load() throws ReportException {
		try {
			return (JasperReport) JRLoader.loadObject(resourceClass.getResource(reportPath));
		}
		catch (Exception e) {
			throw new ReportException("Unable to load report '" + reportPath + "' from classpath", e);
		}
	}

	@Override
	protected String fullReportPath() {
		return resourceClass.getName() + " " + reportPath;
	}
}
