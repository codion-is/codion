/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.plugin.jasperreports.model;

import dev.codion.common.db.reports.ReportWrapper;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.util.Map;

/**
 * A JasperReport wrapper.
 */
public interface JasperReportWrapper extends ReportWrapper<JasperReport, JasperPrint, Map<String, Object>> {}
