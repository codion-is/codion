/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.ReportComponentProvider;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.report.ReportType;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.Windows;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.util.ResourceBundle;

/**
 * A static utility class for displaying reports.
 */
public final class EntityReports {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityReports.class.getName());

  private static final Dimension MINIMUM_REPORT_WINDOW_SIZE = new Dimension(800, 600);
  private static final double SCREEN_SIZE_RATIO = 0.8;

  private EntityReports() {}

  /**
   * Shows a report viewer for report printing
   * @param component the component for which to set the wait cursor while the report is being filled
   * @param reportType the report
   * @param reportParameters the report parameters, if any
   * @param uiWrapper the ui wrapper
   * @param reportTitle the title to display on the frame
   * @param connectionProvider the db provider
   * @param <R> the report result type
   * @param <P> the report parameters type
   */
  public static <R, P> void viewJdbcReport(final JComponent component, final ReportType<?, R, P> reportType,
                                           final P reportParameters, final ReportComponentProvider<R, JComponent> uiWrapper,
                                           final String reportTitle, final EntityConnectionProvider connectionProvider) {
    try {
      Components.showWaitCursor(component);
      viewReport(connectionProvider.getConnection().fillReport(reportType, reportParameters), uiWrapper, reportTitle);
    }
    catch (final ReportException | DatabaseException e) {
      throw new RuntimeException(e);
    }
    finally {
      Components.hideWaitCursor(component);
    }
  }

  /**
   * Shows a report viewer for report printing
   * @param component the component for which to set the wait cursor while the report is being filled
   * @param reportResult the report result
   * @param uiWrapper the ui wrapper
   * @param reportTitle the title to display on the frame
   * @param <R> the report result type
   */
  public static <R> void viewReport(final JComponent component, final R reportResult,
                                    final ReportComponentProvider<R, JComponent> uiWrapper, final String reportTitle) {
    try {
      Components.showWaitCursor(component);
      viewReport(reportResult, uiWrapper, reportTitle);
    }
    finally {
      Components.hideWaitCursor(component);
    }
  }

  /**
   * Shows a view for report printing
   * @param reportResult the report result
   * @param uiWrapper the UI wrapper
   * @param frameTitle the title to display on the frame
   * @param <R> the report result type
   */
  public static <R> void viewReport(final R reportResult, final ReportComponentProvider<R, JComponent> uiWrapper, final String frameTitle) {
    SwingUtilities.invokeLater(() -> {
      final JFrame frame = new JFrame(frameTitle == null ? MESSAGES.getString("report_printer") : frameTitle);
      frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      frame.getContentPane().add(uiWrapper.createReportComponent(reportResult));
      Windows.resizeWindow(frame, SCREEN_SIZE_RATIO, MINIMUM_REPORT_WINDOW_SIZE);
      Windows.centerWindow(frame);
      frame.setVisible(true);
    });
  }
}
