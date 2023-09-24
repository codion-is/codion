/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
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

import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.component.Components.buttonPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static javax.swing.BorderFactory.createTitledBorder;

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
    setBorder(createTitledBorder(BUNDLE.getString(COVER)));
    bindEvents();
  }

  private JPanel createPanel() {
    return borderLayoutPanel()
            .border(emptyBorder())
            .preferredSize(EMBEDDED_SIZE)
            .centerComponent(imagePanel)
            .southComponent(buttonPanel(Controls.builder()
                    .control(Control.builder(this::selectCover)
                            .name(BUNDLE.getString(SELECT_COVER)))
                    .control(Control.builder(this::removeCover)
                            .name(BUNDLE.getString(REMOVE_COVER))
                            .enabled(imageSelected)))
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
