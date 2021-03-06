package mouserun.game;

import java.io.IOException;

/**
 *
 * @author jcsp0003
 */
public class Queso {

    private int x;
    private int y;
    private ImagedPanel panel;
    private int GRID_LENGTH = 30;

    /**
     * Constructor parametrizado de la clase Queso
     *
     * @param _x Valor del queso en el eje x
     * @param _y Valor del queso en el eje y
     * @throws IOException An IOException can occur when the required game
     * assets are missing.
     */
    public Queso(int _x, int _y) throws IOException {
        this.x = _x;
        this.y = _y;
        panel = new ImagedPanel("assets/cheese.png", GRID_LENGTH - 10, GRID_LENGTH - 10);
        panel.setBounds(x * GRID_LENGTH, y * GRID_LENGTH, GRID_LENGTH * 2, 20);
        panel.setOpaque(false);
    }

    /**
     * @return panel Imagen del queso
     */
    public ImagedPanel getPanel() {
        return panel;
    }

    /**
     * Modifica la posicion del queso
     *
     * @param _x nuevo valor del queso en el eje x
     * @param _y nuevo valor del queso en el eje y
     */
    public void setPosicion(int _x, int _y) {
        this.x = _x;
        this.y = _y;
        panel.setBounds(getX() * GRID_LENGTH, getY() * GRID_LENGTH, GRID_LENGTH * 2, 20);
    }

    /**
     * @return the x
     */
    public int getX() {
        return x;
    }

    /**
     * @return the y
     */
    public int getY() {
        return y;
    }

}
