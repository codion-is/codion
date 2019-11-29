/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.plugin.imagepanel.NavigableImagePanel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public class AlbumEditPanel extends EntityEditPanel {

  public AlbumEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(ALBUM_ARTIST_FK);

    createForeignKeyLookupField(ALBUM_ARTIST_FK).setColumns(18);
    createTextField(ALBUM_TITLE).setColumns(18);

    final JPanel inputPanel = new JPanel(new GridLayout(2, 1, 5, 5));
    inputPanel.add(createPropertyPanel(ALBUM_ARTIST_FK));
    inputPanel.add(createPropertyPanel(ALBUM_TITLE));

    setLayout(new BorderLayout(5, 5));

    final JPanel coverArtPanel = createCoverPanel();

    final JPanel inputBasePanel = new JPanel(new BorderLayout(5, 5));
    inputBasePanel.add(inputPanel, BorderLayout.NORTH);

    add(inputBasePanel, BorderLayout.WEST);
    add(coverArtPanel, BorderLayout.CENTER);
  }

  private JPanel createCoverPanel() {
    final NavigableImagePanel imagePanel = new NavigableImagePanel();
    imagePanel.setZoomDevice(NavigableImagePanel.ZoomDevice.NONE);
    imagePanel.setNavigationImageEnabled(false);
    imagePanel.setMoveImageEnabled(false);
    imagePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    imagePanel.setPreferredSize(new Dimension(200, 200));

    final JPanel coverPanel = new JPanel(new BorderLayout());
    coverPanel.setBorder(BorderFactory.createTitledBorder("Cover"));
    coverPanel.add(imagePanel, BorderLayout.CENTER);

    final JPanel coverBasePanel = new JPanel(new BorderLayout(5, 5));
    coverBasePanel.add(coverPanel, BorderLayout.CENTER);

    final JPanel coverButtonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
    coverButtonPanel.add(new JButton(Controls.control(this::setCover, "Select cover...")));
    coverButtonPanel.add(new JButton(Controls.control(this::removeCover, "Remove cover")));
    coverBasePanel.add(coverButtonPanel, BorderLayout.SOUTH);

    final SwingEntityEditModel editModel = getEditModel();
    editModel.addEntitySetListener(album ->
            imagePanel.setImage(album == null ? null : (BufferedImage) album.get(ALBUM_COVER_IMAGE)));
    editModel.addValueSetListener(ALBUM_COVER, valueChange ->
            imagePanel.setImage(valueChange.getCurrentValue() == null ? null : (BufferedImage) editModel.get(ALBUM_COVER_IMAGE)));

    return coverBasePanel;
  }

  private void setCover() throws IOException {
    final File coverFile = UiUtil.selectFile(this, null, "Select image");
    getEditModel().put(ALBUM_COVER, Files.readAllBytes(coverFile.toPath()));
  }

  private void removeCover() {
    getEditModel().put(ALBUM_COVER, null);
  }
}
