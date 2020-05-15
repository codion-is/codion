/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.value.Value;
import is.codion.plugin.imagepanel.NavigableImagePanel;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

/**
 * A panel for displaying a cover image, based on a byte array.
 */
final class CoverArtPanel extends JPanel {

  private final NavigableImagePanel imagePanel;
  private final Value<byte[]> imageBytesValue;

  /**
   * @param imageBytesValue the image bytes value to base this panel on.
   */
  CoverArtPanel(final Value<byte[]> imageBytesValue) {
    super(borderLayout());
    this.imageBytesValue = imageBytesValue;
    this.imagePanel = createImagePanel();
    initializePanel();
    bindEvents();
  }

  private void initializePanel() {
    final JPanel coverPanel = new JPanel(borderLayout());
    coverPanel.setBorder(BorderFactory.createTitledBorder("Cover"));
    coverPanel.add(imagePanel, BorderLayout.CENTER);

    final JPanel coverButtonPanel = new JPanel(gridLayout(1, 2));
    coverButtonPanel.add(new JButton(Controls.control(this::setCover, "Select cover...")));
    coverButtonPanel.add(new JButton(Controls.control(this::removeCover, "Remove cover")));

    add(coverPanel, BorderLayout.CENTER);
    add(coverButtonPanel, BorderLayout.SOUTH);
  }

  private void bindEvents() {
    imageBytesValue.addDataListener(imageBytes -> imagePanel.setImage(readImage(imageBytes)));
  }

  private void setCover() throws IOException {
    final File coverFile = Dialogs.selectFile(this, null, "Select image");
    imageBytesValue.set(Files.readAllBytes(coverFile.toPath()));
  }

  private void removeCover() {
    imageBytesValue.set(null);
  }

  private static NavigableImagePanel createImagePanel() {
    final NavigableImagePanel panel = new NavigableImagePanel();
    panel.setZoomDevice(NavigableImagePanel.ZoomDevice.NONE);
    panel.setNavigationImageEnabled(false);
    panel.setMoveImageEnabled(false);
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    panel.setPreferredSize(new Dimension(200, 200));

    return panel;
  }

  private static BufferedImage readImage(final byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    try {
      return ImageIO.read(new ByteArrayInputStream(bytes));
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
