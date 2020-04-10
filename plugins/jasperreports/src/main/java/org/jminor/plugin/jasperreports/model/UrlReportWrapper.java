/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jasperreports.model;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

import java.net.URL;
import java.util.Map;

final class UrlReportWrapper extends AbstractReportWrapper {

  private static final long serialVersionUID = 1;

  UrlReportWrapper(final String reportUrl, final Map<String, Object> reportParameters) {
    super(reportUrl, reportParameters);
    if (!reportUrl.toLowerCase().startsWith("http")) {
      throw new IllegalArgumentException(reportUrl + " is not a URL");
    }
  }

  @Override
  public int hashCode() {
    return reportPath.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    return obj instanceof UrlReportWrapper && ((UrlReportWrapper) obj).reportPath.equals(this.reportPath);
  }

  @Override
  public String toString() {
    return reportPath;
  }

  @Override
  protected JasperReport loadReport() throws Exception {
    return (JasperReport) JRLoader.loadObject(new URL(reportPath));
  }
}
