package mouserun.game;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;

/**
 * Class ImagedPanel simplifies the transposition of the background images onto
 * the JPanel. It also stores image buffers to lower the IO cost if the image is
 * used again.
 */
public class ImagedPanel extends JPanel {

    private int width;
    private int height;
    private Hashtable<String, BufferedImage> cache;

    private BufferedImage bi;

    /**
     * Creates a new instance of ImagedPanel
     *
     * @param assetAddress The initial asset that is used for the background
     * @param width The width (in pixel) of the ImagedPanel
     * @param height The height (in pixel) of the ImagedPanel
     * @throws IOException IOException can occur if the asset specified is
     * missing.
     */
    public ImagedPanel(String assetAddress, int width, int height) throws IOException {
        this.width = width;
        this.height = height;
        this.cache = new Hashtable<String, BufferedImage>();

        Color black = new Color(0, 0, 0);
        this.setBackground(black);

        bi = ImageIO.read(new File(assetAddress));
    }

    /**
     * Sets a new asset as an image for the ImagedPanel
     *
     * @param assetAddress The asset address
     * @throws IOException IOException can occur if the asset specified is
     * missing
     */
    public void setImage(String assetAddress)
            throws IOException {
        if (cache.containsKey(assetAddress)) {
            bi = cache.get(assetAddress);
        } else {
            bi = ImageIO.read(new File(assetAddress));
            cache.put(assetAddress, bi);
        }

        revalidate();
    }

    /*
	 * (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics g2 = g.create();
        g2.drawImage(bi, 0, 0, width, height, null);

        g2.dispose();
    }

    /*
	 * (non-Javadoc)
	 * @see javax.swing.JComponent#getPreferredSize()
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(bi.getWidth(), bi.getHeight());
    }

}
