/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.nextreports.model;

import org.jminor.common.db.reports.ReportWrapper;

import ro.nextreports.engine.Report;

import java.util.Map;

/**
 * A NextReport wrapper.
 */
public interface NextReportWrapper extends ReportWrapper<Report, NextReportsResult, Map<String, Object>> {}
