/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.nextreports.model;

import is.codion.common.db.reports.Report;

import java.util.Map;

/**
 * A NextReport wrapper.
 */
public interface NextReport extends Report<ro.nextreports.engine.Report, NextReportsResult, Map<String, Object>> {}
