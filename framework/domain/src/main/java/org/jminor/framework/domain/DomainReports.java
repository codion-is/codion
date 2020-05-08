/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportWrapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;

class DomainReports {

  private final Set<ReportWrapper> reports = new HashSet<>();

  DomainReports() {}

  /**
   * Returns true if this domain contains the given report.
   * @param reportWrapper the report.
   * @return true if this domain contains the report.
   */
  boolean containsReport(final ReportWrapper reportWrapper) {
    return reports.contains(requireNonNull(reportWrapper, "reportWrapper"));
  }

  void addReport(final ReportWrapper report) {
    if (containsReport(report)) {
      throw new IllegalArgumentException("Report has already been added: " + report);
    }
    try {
      report.loadReport();
      reports.add(report);
    }
    catch (final ReportException e) {
      throw new RuntimeException(e);
    }
  }

  Collection<ReportWrapper> getReports() {
    return unmodifiableCollection(reports);
  }

  void addAll(final DomainReports reports) {
    requireNonNull(reports, "reports");
    this.reports.addAll(reports.getReports());
  }
}
