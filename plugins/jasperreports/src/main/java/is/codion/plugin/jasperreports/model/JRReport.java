/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports.model;

import is.codion.common.db.reports.Report;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.util.Map;

/**
 * A Jasper Reports report.
 */
public interface JRReport extends Report<JasperReport, JasperPrint, Map<String, Object>> {}
