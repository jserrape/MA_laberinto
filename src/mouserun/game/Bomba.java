package mouserun.game;

import java.io.IOException;

/**
 *
 * @author jcsp0003
 */
public class Bomba {

    private final int x;
    private final int y;
    private final ImagedPanel panel;
    private final int GRID_LENGTH = 30;
    private final String creador;

    /**
     * Constructor parametrizado del la clase Bomba
     * @param _x Valor de la bomba en el eje x
     * @param _y Valor de la bomba en el eje t
     * @param _creador Nombre de la rata que ha creado la bomba
     * @throws IOException An IOException can occur when the required game assets are missing.
     */
    public Bomba(int _x, int _y, String _creador) throws IOException {
        this.x = _x;
        this.y = _y;
        this.creador = _creador;
        panel = new ImagedPanel("assets/bomb.png", GRID_LENGTH-10, GRID_LENGTH-10);
        panel.setBounds(x * GRID_LENGTH, y * GRID_LENGTH, GRID_LENGTH * 2, 20);
        panel.setOpaque(false);
    }

    /**
     * Devuelve el objeto panel de la bomba
     * @return panel
     */
    public ImagedPanel getPanel() {
        return panel;
    }

    /**
     * Compara si dado un nombre de una rata, ésta ha creado la bomba
     * @param posibleCreador Nombre de la rata a comparar
     * @return true si es suya, false en caso contrario
     */
    public boolean getEsDe(String posibleCreador) {
        return (posibleCreador == null ? creador == null : posibleCreador.equals(creador));
    }

}
