/*
* Practica 1: Simulador de agentes
* @author Juan Carlos Serrano Perez: jcsp0003
* @author Juan Carlos Gil Morales:   jcgm0012
 */
package mouserun.mouse;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import mouserun.game.Cheese;
import mouserun.game.Grid;
import mouserun.game.Mouse;

public class M16B06 extends Mouse {

    //Casillas visitadas durante la aparicion de este queso
    private final Map<Integer, Grid> casillasVisitadasQueso;
    //Casillas visitadas en toda la partida
    private final Map<Integer, Grid> casillasVisitadasJuego;
    //Pila para volver hacia atras
    private final Stack<Integer> pila;
    //Clave del raton de la poscion actual
    private int claveActual;
    //Clave del posible movimiento a realizar
    private int auxClave;
    //Clave de la posicion del queso
    private int claveQueso;
    //Contador de pasos para colocar bombas
    private int bombas;
    //Contador de bambas que le quedan por colocar
    private int bombasRestantes;
    //Variable para comprobar si reiniciar las estructuras destinadas a la busqueda del queso
    private boolean comprobarPosicion;

    /*
    * Constructor por defecto del raton
     */
    public M16B06() {
        super("Cervecillo");
        casillasVisitadasQueso = new HashMap<>();
        casillasVisitadasJuego = new HashMap<>();
        pila = new Stack<>();
        claveQueso = 0;
        bombas = 0;
        bombasRestantes = 5;
        comprobarPosicion = false;
    }

    /*
    * @brief Accion que va a realizar el raton 
    * @brief Lleva la cuenta de los pasos y las casillas visitadas
    * @brief Cada vez que aparece un nuevo queso, toma la decision de si limpiar las estructuras destinadas a su busqueda
    * @param currentGrid Casilla en la que se encuentra en raton
    * @param queso Queso objetivo del raton
    * @return Devolvera un numero del 1 al 4 para moverse a una casilla accesible o 5 para colocar una bomba
     */
    @Override
    public int move(Grid currentGrid, Cheese queso) {

        claveActual = funcionDeDispersion(currentGrid.getX(), currentGrid.getY(), 0);
        claveQueso = funcionDeDispersion(queso.getX(), queso.getY(), 0);

        //Reinicio de las estructuras en caso de haber muerto
        if (comprobarPosicion) {
            if (casillasVisitadasQueso.get(claveQueso) != null) {
                casillasVisitadasQueso.clear();
                pila.clear();
            }
            comprobarPosicion = false;
        }

        //Colocacion de las bombas
        bombas++;
        if (bombas == 60 && bombasRestantes != 0) {
            bombas = 0;
            --bombasRestantes;
            return 5;
        }

        //Comprobacion de casilla no visitada
        if (casillasVisitadasQueso.get(claveActual) == null) {
            casillasVisitadasQueso.put(claveActual, currentGrid);
        }

        //Comprobacion de que la casilla no esta en la memoria del raton
        if (casillasVisitadasJuego.get(claveActual) == null) {
            casillasVisitadasJuego.put(claveActual, currentGrid);
            incExploredGrids();
        }

        //Elegir un movimiento con prioridad
        for (int i = 1; i < 5; i++) {
            auxClave = funcionDeDispersion(currentGrid.getX(), currentGrid.getY(), i);
            if (meAcerco(i, currentGrid, queso) && movimientoValido(currentGrid, i) && casillasVisitadasQueso.get(auxClave) == null) {
                pila.add(retrocede(i));
                return i;
            }
        }
        //Elegir un movimiento sin prioridad
        for (int i = 1; i < 5; i++) {
            auxClave = funcionDeDispersion(currentGrid.getX(), currentGrid.getY(), i);
            if (movimientoValido(currentGrid, i) && casillasVisitadasQueso.get(auxClave) == null) {
                pila.add(retrocede(i));
                return i;
            }
        }
        
        
        return pila.pop();
    }

    /*
    * @brief Funcion que se activa cuando un raton coje el queso.
    * @brief Se pone la variable comprobarPosicion, para decidir si limpiar las estructuras destinadas a la busqueda del queso en move(...)
     */
    @Override
    public void newCheese() {
        comprobarPosicion = true;
    }

    /*
    * @brief Funcion que se activa cuando nuestro raton pisa una bomba.
    * @brief Se reiniciaran las estructuras destinadas al movimiento.
     */
    @Override
    public void respawned() {
        casillasVisitadasQueso.clear();
        pila.clear();
    }

    /*
    * @brief Calculo del movimiento inverso 
    * @param movimiento Movimiento que va a realizar el raton
    * @return Devolvera el inverso de este movimiento
     */
    public int retrocede(int movimiento) {
        switch (movimiento) {
            case 1: //Arriba
                return 2;
            case 2: //Abajo
                return 1;
            case 3: //Izquierda
                return 4;
            case 4: //Derecha
                return 3;
        }
        return 0;
    }

    /*
    * @brief Calculo si una casilla es accesible
    * @param currentGrid Casilla en la que se encuentra en raton
    * @param direccion Direccion a comprobar si la casilla es accesible
    * @return Devolvera true si la casilla es accesible, false en caso contrario
     */
    public boolean movimientoValido(Grid currentGrid, int direccion) {
        switch (direccion) {
            case 1: //Arriba
                return currentGrid.canGoUp();
            case 2: //Abajo
                return currentGrid.canGoDown();
            case 3: //Izquierda
                return currentGrid.canGoLeft();
            case 4: //Derecha
                return currentGrid.canGoRight();
        }
        return false;
    }

    /*
    * @brief Calculo de si con un determinado movimiento el raton se acerca al queso
    * @param currentGrid Casilla en la que se encuentra en raton
    * @param direccion Direccion a comprobar si se esta mas cerca del queso
    * @param queso Queso objetivo del raton
    * @return Devolvera true si con este movimiento el raton se acerca al queso, false en casi contrario.
     */
    public boolean meAcerco(int direccion, Grid currentGrid, Cheese queso) {
        switch (direccion) {
            case 1: //Arriba
                return queso.getY() > currentGrid.getY();
            case 2: //Abajo
                return queso.getY() < currentGrid.getY();
            case 3: //Izquierda
                return queso.getX() < currentGrid.getX();
            case 4: //Derecha
                return queso.getX() > currentGrid.getX();
        }
        return true;
    }

    /*
    * @brief Calculo de la clave de dispersion de una casilla mediante desplazamiento de bits.
    * @param x Coordenada X de eje de coordenadas de la casilla en la que se encuentra el raton
    * @param y Coordenada Y de eje de coordenadas de la casilla en la que se encuentra el raton
    * @param direccion Casilla para la que se va a calcular la clave: 0 si es la casilla actual, del 1 al 4 si es una adyacente
    * @return Devolvera la clave de deispersion de la casilla.
     */
    public int funcionDeDispersion(int x, int y, int direccion) {
        switch (direccion) {
            case 1: //Casilla de arriba
                y = y + 1;
                break;
            case 2: //Casilla de abajo
                y = y - 1;
                break;
            case 3: //Casilla de la izquierda
                x = x - 1;
                break;
            case 4: //Casilla de la derecha
                x = x + 1;
                break;
        }
        return (y << 3) + (x << 2) + (y << 5) + (x << 7);
    }

}
