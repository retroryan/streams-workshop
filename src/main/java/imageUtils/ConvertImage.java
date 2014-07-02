package imageUtils;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;

public class ConvertImage {

    public static BufferedImage invertImage(BufferedImage inputImage) {

        for (int x = 0; x < inputImage.getWidth(); x++) {
            for (int y = 0; y < inputImage.getHeight(); y++) {
                int rgba = inputImage.getRGB(x, y);
                Color col = new Color(rgba, true);
                col = new Color(255 - col.getRed(),
                        255 - col.getGreen(),
                        255 - col.getBlue());
                inputImage.setRGB(x, y, col.getRGB());
            }
        }

        return inputImage;
    }

    public static BufferedImage addWaterMark(BufferedImage inputImage) throws MalformedURLException {
        Graphics2D g2d = inputImage.createGraphics();

        //Create an alpha composite of 50%
        AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f);
        g2d.setComposite(alpha);

        g2d.setColor(Color.white);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setFont(new Font("Arial", Font.BOLD, 30));

        String watermark = "Copyright Typesafe © 2014";

        FontMetrics fontMetrics = g2d.getFontMetrics();
        Rectangle2D rect = fontMetrics.getStringBounds(watermark, g2d);

        g2d.drawString(watermark,
                inputImage.getWidth() - ((int) rect.getWidth() + 5),
                inputImage.getHeight() - ((int) rect.getHeight() + 5));

        //Free graphic resources
        g2d.dispose();

        return inputImage;
    }
}
