/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.reporting;

import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.reports.ReportUIWrapper;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;

/**
 * A static utility class for displaying reports.
 */
public final class EntityReportUiUtil {

  private EntityReportUiUtil() {}

  /**
   * Shows a view for report printing
   * @param reportResult the report result
   * @param uiWrapper the UI wrapper
   * @param frameTitle the title to display on the frame
   */
  public static <T> void viewReport(final ReportResult<T> reportResult, final ReportUIWrapper<T> uiWrapper, final String frameTitle) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        final JFrame frame = new JFrame(frameTitle == null ? FrameworkMessages.get(FrameworkMessages.REPORT_PRINTER) : frameTitle);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(uiWrapper.createReportComponent(reportResult));
        UiUtil.resizeWindow(frame, 0.8, new Dimension(800, 600));
        UiUtil.centerWindow(frame);
        frame.setVisible(true);
      }
    });
  }
}
