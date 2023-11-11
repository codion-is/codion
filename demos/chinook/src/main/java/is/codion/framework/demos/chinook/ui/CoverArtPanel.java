/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.plugin.imagepanel.NavigableImagePanel;
import is.codion.swing.common.ui.FileTransferHandler;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.kordamp.ikonli.foundation.Foundation;

import javax.imageio.ImageIO;
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

import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.component.Components.buttonPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createEtchedBorder;

/**
 * A panel for displaying a cover image, based on a byte array.
 */
final class CoverArtPanel extends JPanel {

  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(CoverArtPanel.class.getName());

  private static final Dimension EMBEDDED_SIZE = new Dimension(200, 200);
  private static final Dimension DIALOG_SIZE = new Dimension(400, 400);
  private static final String[] IMAGE_FILE_EXTENSIONS = new String[] {"jpg", "jpeg", "png", "bmp", "gif"};

  private static final String COVER = "cover";
  private static final String SELECT_IMAGE = "select_image";
  private static final String IMAGES = "images";

  private final JPanel basePanel;
  private final NavigableImagePanel imagePanel;
  private final Value<byte[]> imageBytes;
  private final State imageSelected;
  private final State embedded = State.state(true);

  /**
   * @param imageBytes the image bytes value to base this panel on.
   */
  CoverArtPanel(Value<byte[]> imageBytes) {
    super(borderLayout());
    this.imageBytes = imageBytes;
    this.imageSelected = State.state(imageBytes.isNotNull());
    this.imagePanel = createImagePanel();
    this.basePanel = createPanel();
    add(basePanel, BorderLayout.CENTER);
    bindEvents();
  }

  private JPanel createPanel() {
    return borderLayoutPanel()
            .preferredSize(EMBEDDED_SIZE)
            .centerComponent(imagePanel)
            .southComponent(borderLayoutPanel()
                    .eastComponent(buttonPanel(Controls.builder()
                            .control(Control.builder(this::selectCover)
                                    .smallIcon(FrameworkIcons.instance().icon(Foundation.PLUS)))
                            .control(Control.builder(this::removeCover)
                                    .smallIcon(FrameworkIcons.instance().icon(Foundation.MINUS))
                                    .enabled(imageSelected)))
                            .buttonGap(0)
                            .border(createEmptyBorder(0, 0, Layouts.GAP.get(), 0))
                            .build())
                    .build())
            .build();
  }

  private void bindEvents() {
    imageBytes.addDataListener(imageBytes -> imagePanel.setImage(readImage(imageBytes)));
    imageBytes.addDataListener(imageBytes -> imageSelected.set(imageBytes != null));
    embedded.addDataListener(this::setEmbedded);
    imagePanel.addMouseListener(new EmbeddingMouseListener());
  }

  private void selectCover() throws IOException {
    File coverFile = Dialogs.fileSelectionDialog()
            .owner(this)
            .title(BUNDLE.getString(SELECT_IMAGE))
            .fileFilter(new FileNameExtensionFilter(BUNDLE.getString(IMAGES), IMAGE_FILE_EXTENSIONS))
            .selectFile();
    imageBytes.set(Files.readAllBytes(coverFile.toPath()));
  }

  private void removeCover() {
    imageBytes.set(null);
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
    Utilities.disposeParentWindow(basePanel);
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
            .onClosed(windowEvent -> embedded.set(true))
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
    panel.setBorder(createEtchedBorder());

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
        embedded.set(!embedded.get());
      }
    }
  }

  private final class CoverTransferHandler extends FileTransferHandler {

    @Override
    protected boolean importFiles(Component component, List<File> files) {
      File importedFile = files.get(0);
      boolean isImage = Arrays.stream(IMAGE_FILE_EXTENSIONS)
              .anyMatch(extension -> importedFile.getName().toLowerCase().endsWith(extension));
      try {
        imageBytes.set(isImage ? Files.readAllBytes(importedFile.toPath()) : null);

        return isImage;
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
