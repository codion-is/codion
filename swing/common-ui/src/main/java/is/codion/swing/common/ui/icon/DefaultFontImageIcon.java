/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icon;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.image.BufferedImage;

import static java.util.Objects.requireNonNull;

/**
 * A default FontImageIcon implementation.
 */
final class DefaultFontImageIcon implements FontImageIcon {

  private final FontIcon fontIcon;
  private final ImageIcon imageIcon;

  private DefaultFontImageIcon(DefaultBuilder builder) {
    this.fontIcon = FontIcon.of(builder.ikon, builder.size, builder.color);
    this.imageIcon = createImageIcon();
    paintIcon();
  }

  @Override
  public ImageIcon imageIcon() {
    return imageIcon;
  }

  @Override
  public void setColor(Color color) {
    fontIcon.setIconColor(color);
    paintIcon();
  }

  private void paintIcon() {
    fontIcon.paintIcon(null, imageIcon.getImage().getGraphics(), 0, 0);
  }

  private ImageIcon createImageIcon() {
    return new ImageIcon(new BufferedImage(fontIcon.getIconWidth(), fontIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB));
  }

  static final class DefaultBuilder implements Builder {

    private final Ikon ikon;

    private int size = Icons.ICON_SIZE.get();
    private Color color = Icons.ICON_COLOR.get();

    DefaultBuilder(Ikon ikon) {
      this.ikon = requireNonNull(ikon);
    }

    @Override
    public Builder size(int size) {
      this.size = size;
      return this;
    }

    @Override
    public Builder color(Color color) {
      this.color = requireNonNull(color);
      return this;
    }

    @Override
    public FontImageIcon build() {
      return new DefaultFontImageIcon(this);
    }
  }
}
