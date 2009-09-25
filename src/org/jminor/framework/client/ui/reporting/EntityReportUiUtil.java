package org.jminor.framework.client.ui.reporting;

import org.jminor.common.ui.UiUtil;
import org.jminor.framework.i18n.FrameworkMessages;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JRViewer;

import javax.swing.JFrame;
import java.awt.Dimension;

public class EntityReportUiUtil {

  /**
   * Shows a JRViewer for report printing
   * @param jasperPrint the JasperPrint object to view
   * @param frameTitle the title to display on the frame
   */
  public static void viewReport(final JasperPrint jasperPrint, final String frameTitle) {
    final JFrame frame = new JFrame(frameTitle == null ? FrameworkMessages.get(FrameworkMessages.REPORT_PRINTER) : frameTitle);
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.getContentPane().add(new JRViewer(jasperPrint));
    UiUtil.resizeWindow(frame, 0.8, new Dimension(800, 600));
    UiUtil.centerWindow(frame);
    frame.setVisible(true);
  }
}
