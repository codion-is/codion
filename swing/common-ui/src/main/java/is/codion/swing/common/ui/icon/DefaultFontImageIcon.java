/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icon;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.ImageIcon;
import java.awt.Color;

import static java.util.Objects.requireNonNull;

/**
 * A default FontImageIcon implementation.
 */
final class DefaultFontImageIcon implements FontImageIcon {

  private final FontIcon fontIcon;
  private final ImageIcon imageIcon;
  private final IconPainter iconPainter;

  private DefaultFontImageIcon(DefaultBuilder builder) {
    this.fontIcon = FontIcon.of(builder.ikon, builder.size, builder.color);
    this.imageIcon = builder.imageIconFactory.createImageIcon(fontIcon);
    this.iconPainter = builder.iconPainter;
    this.iconPainter.paintIcon(fontIcon, imageIcon);
  }

  @Override
  public ImageIcon imageIcon() {
    return imageIcon;
  }

  @Override
  public void setColor(Color color) {
    fontIcon.setIconColor(color);
    iconPainter.paintIcon(fontIcon, imageIcon);
  }

  static final class DefaultBuilder implements Builder {

    private static final IconPainter DEFAULT_ICON_PAINTER = new DefaultIconPainter();
    private static final ImageIconFactory DEFAULT_ICON_FACTORY = new DefaultImageIconFactory();

    private final Ikon ikon;

    private int size = Icons.ICON_SIZE.get();
    private Color color = Icons.ICON_COLOR.get();
    private IconPainter iconPainter = DEFAULT_ICON_PAINTER;
    private ImageIconFactory imageIconFactory = DEFAULT_ICON_FACTORY;

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
    public Builder iconPainter(IconPainter iconPainter) {
      this.iconPainter = requireNonNull(iconPainter);
      return this;
    }

    @Override
    public Builder imageIconFactory(ImageIconFactory imageIconFactory) {
      this.imageIconFactory = requireNonNull(imageIconFactory);
      return this;
    }

    @Override
    public FontImageIcon build() {
      return new DefaultFontImageIcon(this);
    }
  }

  private static final class DefaultIconPainter implements IconPainter {}

  private static final class DefaultImageIconFactory implements ImageIconFactory {}
}
