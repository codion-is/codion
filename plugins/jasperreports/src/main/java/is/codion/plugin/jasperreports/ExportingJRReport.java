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

import is.codion.common.db.report.ReportException;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.sql.Connection;
import java.util.Map;

import static java.util.Objects.requireNonNull;

final class ExportingJRReport<R> implements JRReport<R> {

	private final JRReport<JasperPrint> report;
	private final JRExport<R> export;

	ExportingJRReport(JRReport<JasperPrint> report, JRExport<R> export) {
		this.report = requireNonNull(report);
		this.export = requireNonNull(export);
	}

	@Override
	public R fill(Connection connection, Map<String, Object> parameters) {
		return export(report.fill(connection, parameters));
	}

	//the loaded report and its cache belong to the wrapped report, exporting says nothing of either.
	//clearCache() in particular must reach it, the server clears the cache of every report it hosts

	@Override
	public JasperReport load() {
		return report.load();
	}

	@Override
	public boolean cached() {
		return report.cached();
	}

	@Override
	public void clearCache() {
		report.clearCache();
	}

	@Override
	public String toString() {
		return report.toString();
	}

	private R export(JasperPrint print) {
		try {
			return export.export(print);
		}
		catch (ReportException e) {
			throw e;
		}
		catch (Exception e) {
			//as in AbstractJRReport.fill(), no engine exception may escape, a JRException or a
			//JRRuntimeException from a missing exporter extension being the likely ones here
			throw new ReportException(e);
		}
	}
}
