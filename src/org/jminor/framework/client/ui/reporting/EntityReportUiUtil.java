/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.reporting;

import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.reports.ReportUIWrapper;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.JFrame;
import java.awt.Dimension;

/**
 * A static utility class for displaying reports.
 */
public class EntityReportUiUtil {

  private EntityReportUiUtil() {}

  /**
   * Shows a view for report printing
   * @param report the report result
   * @param uiWrapper the UI wrapper
   * @param frameTitle the title to display on the frame
   * @throws org.jminor.common.model.reports.ReportException in case of a report exc
   */
  public static void viewReport(final ReportResult report, final ReportUIWrapper uiWrapper, final String frameTitle) throws ReportException {
    final JFrame frame = new JFrame(frameTitle == null ? FrameworkMessages.get(FrameworkMessages.REPORT_PRINTER) : frameTitle);
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.getContentPane().add(uiWrapper.createReportComponent(report));
    UiUtil.resizeWindow(frame, 0.8, new Dimension(800, 600));
    UiUtil.centerWindow(frame);
    frame.setVisible(true);
  }
}
