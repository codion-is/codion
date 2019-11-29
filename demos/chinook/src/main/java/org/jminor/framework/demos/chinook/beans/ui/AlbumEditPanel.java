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
import java.awt.FlowLayout;
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

    final NavigableImagePanel coverArtImagePanel = new NavigableImagePanel();
    coverArtImagePanel.setZoomDevice(NavigableImagePanel.ZoomDevice.NONE);
    coverArtImagePanel.setNavigationImageEnabled(false);
    coverArtImagePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    coverArtImagePanel.setPreferredSize(new Dimension(200, 200));
    final JPanel coverArtPanel = new JPanel(new BorderLayout());
    coverArtPanel.setBorder(BorderFactory.createTitledBorder("Cover"));
    coverArtPanel.add(coverArtImagePanel, BorderLayout.CENTER);

    final JPanel coverArtBasePanel = new JPanel(new BorderLayout(5, 5));
    coverArtBasePanel.add(coverArtPanel, BorderLayout.CENTER);
    final JPanel coverButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    coverButtonPanel.add(new JButton(Controls.control(this::setCoverArt, "Select cover...",
            getEditModel().getEntityNewObserver().getReversedObserver())));
    coverArtBasePanel.add(coverButtonPanel, BorderLayout.SOUTH);

    final SwingEntityEditModel editModel = getEditModel();
    editModel.addEntitySetListener(album ->
            coverArtImagePanel.setImage(album == null ? null : (BufferedImage) album.get(ALBUM_COVERART_IMAGE)));
    editModel.addValueSetListener(ALBUM_COVERART, valueChange ->
            coverArtImagePanel.setImage(valueChange.getCurrentValue() == null ? null : (BufferedImage) editModel.get(ALBUM_COVERART_IMAGE)));
    final JPanel inputBasePanel = new JPanel(new BorderLayout(5, 5));
    inputBasePanel.add(inputPanel, BorderLayout.NORTH);

    add(inputBasePanel, BorderLayout.WEST);
    add(coverArtBasePanel, BorderLayout.CENTER);
  }

  private void setCoverArt() throws IOException {
    final File coverFile = UiUtil.selectFile(this, null, "Select image");
    getEditModel().put(ALBUM_COVERART, Files.readAllBytes(coverFile.toPath()));
  }
}
