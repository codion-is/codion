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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.icon;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URL;

import static java.awt.AlphaComposite.Clear;
import static java.awt.AlphaComposite.SrcOver;
import static java.awt.RenderingHints.*;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.util.Objects.requireNonNull;

final class DefaultSVGIcon implements SVGIcon {

	private final ImageIcon imageIcon;
	private final URL svgUrl;
	private final SVGDocument svgDocument;

	private Color color;

	DefaultSVGIcon(URL svgUrl, int size, Color color) {
		this.svgUrl = svgUrl;
		this.svgDocument = loadSvgDocument(svgUrl);
		this.color = color;
		this.imageIcon = createImageIcon(size);
		paintIcon();
	}

	@Override
	public ImageIcon imageIcon() {
		return imageIcon;
	}

	@Override
	public void color(Color color) {
		this.color = requireNonNull(color);
		paintIcon();
	}

	@Override
	public SVGIcon copy(int size, Color color) {
		return new DefaultSVGIcon(svgUrl, size, color);
	}

	private void paintIcon() {
		BufferedImage image = (BufferedImage) imageIcon.getImage();
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);
		graphics.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
		// Clear to transparent
		graphics.setComposite(Clear);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
		graphics.setComposite(SrcOver);
		svgDocument.render(null, graphics, new ViewBox(new FloatSize(image.getWidth(), image.getHeight())));
		graphics.dispose();
		colorize(image, color);
	}

	private static void colorize(BufferedImage image, Color targetColor) {
		int targetRGB = targetColor.getRGB() & 0x00FFFFFF; // RGB without alpha
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int pixel = image.getRGB(x, y);
				int alpha = pixel & 0xFF000000;
				if (alpha != 0) { // Only colorize non-transparent pixels
					image.setRGB(x, y, alpha | targetRGB);
				}
			}
		}
	}

	private static SVGDocument loadSvgDocument(URL svgUrl) {
		SVGLoader loader = new SVGLoader();
		SVGDocument document = loader.load(svgUrl);
		if (document == null) {
			throw new IllegalArgumentException("Unable to load SVG from url: " + svgUrl);
		}

		return document;
	}

	private static ImageIcon createImageIcon(int size) {
		return new ImageIcon(new BufferedImage(size, size, TYPE_INT_ARGB));
	}
}
