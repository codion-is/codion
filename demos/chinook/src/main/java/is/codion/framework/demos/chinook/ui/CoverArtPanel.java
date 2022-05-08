/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.plugin.imagepanel.NavigableImagePanel;
import is.codion.swing.common.ui.FileTransferHandler;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

/**
 * A panel for displaying a cover image, based on a byte array.
 */
final class CoverArtPanel extends JPanel {

  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(CoverArtPanel.class.getName());

  private static final Dimension EMBEDDED_SIZE = new Dimension(240, 240);
  private static final Dimension DIALOG_SIZE = new Dimension(400, 400);
  private static final String[] IMAGE_FILE_EXTENSIONS = new String[] {"jpg", "jpeg", "png", "bmp", "gif"};

  private static final String COVER = "cover";
  private static final String SELECT_COVER = "select_cover";
  private static final String REMOVE_COVER = "remove_cover";
  private static final String SELECT_IMAGE = "select_image";
  private static final String IMAGES = "images";

  private final JPanel basePanel;
  private final NavigableImagePanel imagePanel;
  private final Value<byte[]> imageBytesValue;
  private final State embeddedState = State.state(true);

  /**
   * @param imageBytesValue the image bytes value to base this panel on.
   */
  CoverArtPanel(Value<byte[]> imageBytesValue) {
    super(borderLayout());
    this.imageBytesValue = imageBytesValue;
    this.imagePanel = createImagePanel();
    this.basePanel = initializePanel();
    add(basePanel, BorderLayout.CENTER);
    setBorder(BorderFactory.createTitledBorder(BUNDLE.getString(COVER)));
    bindEvents();
  }

  private JPanel initializePanel() {
    return Components.panel(borderLayout())
            .border(BorderFactory.createEmptyBorder(5, 5, 5, 5))
            .preferredSize(EMBEDDED_SIZE)
            .add(imagePanel, BorderLayout.CENTER)
            .add(Controls.builder()
                    .control(Control.builder(this::selectCover)
                            .caption(BUNDLE.getString(SELECT_COVER)))
                    .control(Control.builder(this::removeCover)
                            .caption(BUNDLE.getString(REMOVE_COVER)))
                    .build()
                    .createHorizontalButtonPanel(), BorderLayout.SOUTH)
            .build();
  }

  private void bindEvents() {
    imageBytesValue.addDataListener(imageBytes -> imagePanel.setImage(readImage(imageBytes)));
    embeddedState.addDataListener(this::setEmbedded);
    imagePanel.addMouseListener(new EmbeddingMouseListener());
  }

  private void selectCover() throws IOException {
    File coverFile = Dialogs.fileSelectionDialog()
            .owner(this)
            .title(BUNDLE.getString(SELECT_IMAGE))
            .fileFilter(new FileNameExtensionFilter(BUNDLE.getString(IMAGES), IMAGE_FILE_EXTENSIONS))
            .selectFile();
    imageBytesValue.set(Files.readAllBytes(coverFile.toPath()));
  }

  private void removeCover() {
    imageBytesValue.set(null);
  }

  private void setEmbedded(boolean embedded) {
    configureImagePanel(embedded);
    if (embedded) {
      embed();
    }
    else {
      displayInDialog();
    }
  }

  private void embed() {
    Windows.getParentDialog(basePanel)
            .ifPresent(JDialog::dispose);
    basePanel.setSize(EMBEDDED_SIZE);
    imagePanel.resetView();
    add(basePanel, BorderLayout.CENTER);
    revalidate();
    repaint();
  }

  private void displayInDialog() {
    remove(basePanel);
    revalidate();
    repaint();
    Dialogs.componentDialog(basePanel)
            .owner(this)
            .modal(false)
            .title(BUNDLE.getString(COVER))
            .onClosed(windowEvent -> embeddedState.set(true))
            .onOpened(windowEvent -> imagePanel.resetView())
            .size(DIALOG_SIZE)
            .show();
  }

  private void configureImagePanel(boolean embedded) {
    imagePanel.setZoomDevice(embedded ? NavigableImagePanel.ZoomDevice.NONE : NavigableImagePanel.ZoomDevice.MOUSE_WHEEL);
    imagePanel.setMoveImageEnabled(!embedded);
  }

  private NavigableImagePanel createImagePanel() {
    NavigableImagePanel panel = new NavigableImagePanel();
    panel.setZoomDevice(NavigableImagePanel.ZoomDevice.NONE);
    panel.setNavigationImageEnabled(false);
    panel.setMoveImageEnabled(false);
    panel.setTransferHandler(new CoverTransferHandler());

    return panel;
  }

  private static BufferedImage readImage(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    try {
      return ImageIO.read(new ByteArrayInputStream(bytes));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private final class EmbeddingMouseListener extends MouseAdapter {

    @Override
    public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
        embeddedState.set(!embeddedState.get());
      }
    }
  }

  private final class CoverTransferHandler extends FileTransferHandler {

    @Override
    protected boolean importFiles(Component component, List<File> files) {
      File importedFile = files.get(0);
      boolean isImage = Arrays.stream(IMAGE_FILE_EXTENSIONS)
              .anyMatch(extension -> importedFile.getName().endsWith(extension));
      try {
        imageBytesValue.set(isImage ? Files.readAllBytes(importedFile.toPath()) : null);

        return isImage;
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
