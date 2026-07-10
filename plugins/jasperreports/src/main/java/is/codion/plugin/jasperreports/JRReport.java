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

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.util.Map;

/**
 * A JasperReport, producing a result of type {@code R} when filled.
 * <p>Identified by a plain {@link is.codion.common.db.report.ReportType}, created via
 * {@link is.codion.common.db.report.ReportType#reportType(String)}, since a report type names
 * a report and says nothing of the engine backing it:
 * {@snippet :
 * ReportType<Map<String, Object>, JasperPrint> REPORT = reportType("customer_report");
 *
 * add(REPORT, classPathReport(Store.class, "customer_report.jasper"));
 *}
 * <p>Filling produces a {@link JasperPrint} unless an export is applied via
 * {@link JasperReports#export(JRReport, JRExport)}, in which case the report produces
 * whatever that export produces, a PDF for example, in which case the client never
 * sees a JasperReports type:
 * {@snippet :
 * ReportType<Map<String, Object>, byte[]> REPORT = reportType("customer_report");
 *
 * add(REPORT, export(classPathReport(Store.class, "customer_report.jasper"), PDF));
 *}
 * @param <R> the type this report produces when filled
 * @see JRExport
 */
public interface JRReport<R> extends Report<JasperReport, Map<String, Object>, R> {}
