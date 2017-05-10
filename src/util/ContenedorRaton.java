/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import juegos.elementos.Partida;
import juegos.elementos.Posicion;
import laberinto.OntologiaLaberinto;
import laberinto.elementos.EntornoLaberinto;
import laberinto.elementos.Laberinto;

/**
 *
 * @author jcsp0003
 */
public class ContenedorRaton {

    //Elementos de la partida
    private String id;
    private Partida partida;
    private Laberinto tablero;
    private Posicion posicion;
    private EntornoLaberinto entornoActual;

    //Atributos para control del juego
    private boolean muerto;
    private boolean nuevoQueso;

    private Posicion posicionQueso;

    //Elementos para el movimiento
    private HashMap<Pair<Integer, Integer>, mouseNode> maze;
    private Stack<Integer> camino;
    private int moveCount;  //Cuenta los movimientos. Se reinicia al colocar una bomba.
    private int bombsLeft;  //Cuenta las bombas que quedan por poner.

    private boolean informativeSearch;

    public ContenedorRaton(String _id, Partida _partida, Laberinto _tablero, Posicion _posicionQ, EntornoLaberinto _entornoActual, int _bombasRestantes) {
        this.id = _id;
        this.partida = _partida;
        this.tablero = _tablero;
        this.posicion = new Posicion(0, 0);
        this.entornoActual = _entornoActual;
        this.bombsLeft = _bombasRestantes;
        this.posicionQueso = _posicionQ;

        moveCount = 0;
        informativeSearch = false;
        camino = new Stack<>();
        maze = new HashMap<>();

        nuevoQueso = false;
        muerto = false;

    }

    /**
     *
     * @return true si no pongo bomba, false si la pongo
     */
    public boolean moverse() {
        
        if(nuevoQueso || muerto ){
            nuevoQueso=false;
            muerto=false;
            reinicio();
        }
        
        //Creamos un Pair, con la posición actual y una referancia a un mouseNode
        Pair<Integer, Integer> currentPos = new Pair<>(posicion.getCoorX(), posicion.getCoorY());
        mouseNode currentNode;

        //Buscamos en maze la posición actual. Si está, currentNode será el nodo almacenado
        //en caso contrario, se crea un nuevo nodo y se almacena.
        if (maze.containsKey(currentPos)) {
            currentNode = maze.get(currentPos);
        } else {
            currentNode = new mouseNode(
                    currentPos,
                    movimientoValido(1), movimientoValido(2),
                    movimientoValido(3), movimientoValido(4)
            );

            maze.put(currentPos, currentNode);
        }

        //En caso de que nos encontremos en la casilla del cheese,
        //abandonamos la casilla y volvemos a ella
        if (posicionQueso.getCoorX() == currentNode.x && posicionQueso.getCoorY() == currentNode.y && camino.isEmpty()) {
            if (movimientoValido(1)) {
                camino.add(2);
                camino.add(1);
            } else {
                if (movimientoValido(2)) {
                    camino.add(1);
                    camino.add(2);
                } else {
                    if (movimientoValido(3)) {
                        camino.add(4);
                        camino.add(3);
                    } else {
                        if (movimientoValido(4)) {
                            camino.add(3);
                            camino.add(4);
                        }
                    }
                }
            }
        }

        //Comprobamos si quedan bombas
        if (bombsLeft > 0) {
            int exitCount = 0;
            //Almacena la cantidad de direcciones por las que
            //se puede avanzar, desde el nodo actual.

            if (currentNode.up) {
                exitCount++;
            }
            if (currentNode.down) {
                exitCount++;
            }
            if (currentNode.left) {
                exitCount++;
            }
            if (currentNode.right) {
                exitCount++;
            }

            //Según el número de movimientos y el número de salidas, se decide
            //si colocar, o no, una bomba.
            if (moveCount > 30 && exitCount > 3) {
                moveCount = 0;
                bombsLeft--;
                return false;
            } else {
                if (moveCount > 100 && exitCount > 2) {
                    moveCount = 0;
                    bombsLeft--;
                    return false;
                } else {
                    moveCount++;
                }
            }
        }

        //Si no hay ningún camino, generamos uno.
        if (camino.isEmpty()) {
            Pair<Integer, Integer> target = new Pair<>(posicionQueso.getCoorX(), posicionQueso.getCoorY());

            if (maze.containsKey(target)) {
                informativeSearch = true;
                //Si sabemos donde está el objetivo,
                //usamos A* (puede fallar, esto se considerará
                //más adelante)
            } else {
                informativeSearch = false;
                //Exploramos con profundidadLimitada
            }

            getCamino(currentNode, target);
            //Obtenemos un camino al Cheese
            //o a una casilla no explorada.
        }

        aplicar(camino.pop());
        return true;
    }

    public void reinicio() {
        camino.clear();
    }

    void getCamino(mouseNode rootNode, Pair<Integer, Integer> target) {
        List<Pair<Integer, mouseNode>> candidatos = new ArrayList<>(); //Guarda la profundidad del nodo y el nodo
        HashMap<Pair<Integer, Integer>, mouseNode> anteriores = new HashMap<>();
        mouseNode targetNode = null;

        //Llamadas a búsquedas
        if (informativeSearch) {
            busquedaAStar(rootNode, target, anteriores);
            targetNode = maze.get(target); //El nodo objetivo es el mismo queso.
        } else {
            //Comenzamos con límite 5. Si no hay casillas sin explorar,
            //incrementamos dicho límite en 5 unidades.

            int limite = 5;
            targetNode = null;
            while (targetNode == null) {
                targetNode = busquedaProfundidadLimitada(rootNode, target, anteriores, limite);
                limite += 5;
            }
        }

        //Si A* seleccionado, pero target inaccesible, empleamos la búsqueda de exploración.
        if (informativeSearch && !anteriores.containsKey(target)) {
            //Conocemos la posición del queso, pero es inaccesible.
            //El A* no llega a él.

            int limite = 5;
            targetNode = null;
            while (targetNode == null) {
                targetNode = busquedaProfundidadLimitada(rootNode, target, anteriores, limite);
                limite += 5;
            }
        }

        //Finalmente calculamos el camino al nodo objetivo          
        mouseNode curNode = anteriores.get(targetNode.getPos());
        camino.add(getDirection(curNode.getPos(), targetNode.getPos()));

        while (curNode != rootNode) {
            Pair<Integer, Integer> targetPos = curNode.getPos();
            curNode = anteriores.get(curNode.getPos());
            camino.add(getDirection(curNode.getPos(), targetPos));
        }

    }

    void busquedaAStar(mouseNode rootNode, Pair<Integer, Integer> target, HashMap<Pair<Integer, Integer>, mouseNode> anteriores) {
        List<Pair<Integer, mouseNode>> abiertos = new LinkedList<>();
        HashMap<Pair<Integer, Integer>, mouseNode> cerrados = new HashMap<>();

        abiertos.add(new Pair<>(0, rootNode));

        while (!abiertos.isEmpty()) {
            int min = 9999;
            int minIndex = 0;

            for (int i = 0; i < abiertos.size(); i++) {
                Pair<Integer, mouseNode> w = abiertos.get(i);
                if (w.second.getPos() == target) {
                    minIndex = i;
                    break;
                }

                int curValue = w.first + distanciaManhattam(w.second.getPos(), target);
                if (curValue < min) {
                    min = curValue;
                    minIndex = i;
                }
            }

            Pair<Integer, mouseNode> v = abiertos.get(minIndex);
            abiertos.remove(v);
            cerrados.put(v.second.getPos(), v.second);
            int nivel = v.first + 1;

            if (v.second.x == target.first && v.second.y == target.second) {
                break;
            }

            //DOWN
            if (v.second.down) {
                Pair<Integer, Integer> curPos = v.second.getPos();
                curPos.second--;

                if (maze.containsKey(curPos)) {
                    mouseNode w = maze.get(curPos);
                    Pair<Integer, mouseNode> insert = new Pair<>(nivel, w);
                    if (!cerrados.containsKey(insert.second.getPos())) {
                        abiertos.add(insert);
                        anteriores.put(w.getPos(), v.second);
                    }
                }
            }

            //LEFT
            if (v.second.left) {
                Pair<Integer, Integer> curPos = v.second.getPos();
                curPos.first--;

                if (maze.containsKey(curPos)) {
                    mouseNode w = maze.get(curPos);
                    Pair<Integer, mouseNode> insert = new Pair<>(nivel, w);
                    if (!cerrados.containsKey(insert.second.getPos())) {
                        abiertos.add(insert);
                        anteriores.put(w.getPos(), v.second);
                    }
                }
            }

            //RIGHT
            if (v.second.right) {
                Pair<Integer, Integer> curPos = v.second.getPos();
                curPos.first++;

                if (maze.containsKey(curPos)) {
                    mouseNode w = maze.get(curPos);
                    Pair<Integer, mouseNode> insert = new Pair<>(nivel, w);
                    if (!cerrados.containsKey(insert.second.getPos())) {
                        abiertos.add(insert);
                        anteriores.put(w.getPos(), v.second);
                    }
                }
            }

            //UP
            if (v.second.up) {
                Pair<Integer, Integer> curPos = v.second.getPos();
                curPos.second++;

                if (maze.containsKey(curPos)) {
                    mouseNode w = maze.get(curPos);
                    Pair<Integer, mouseNode> insert = new Pair<>(nivel, w);
                    if (!cerrados.containsKey(insert.second.getPos())) {
                        abiertos.add(insert);
                        anteriores.put(w.getPos(), v.second);
                    }
                }
            }
        }
    }

    private mouseNode busquedaProfundidadLimitada(mouseNode rootNode, Pair<Integer, Integer> target, HashMap<Pair<Integer, Integer>, mouseNode> anteriores, int limite) {
        Stack<Pair<Integer, mouseNode>> abiertos = new Stack<>();
        HashMap<Pair<Integer, Integer>, mouseNode> cerrados = new HashMap<>();
        List<Pair<Integer, mouseNode>> candidatos = new LinkedList<>();

        abiertos.add(new Pair<>(0, rootNode));

        while (!abiertos.isEmpty()) {
            Pair<Integer, mouseNode> v = abiertos.pop();
            cerrados.put(v.second.getPos(), v.second);

            int nivel = v.first + 1;

            if (v.second.x == target.first && v.second.y == target.second) {
                candidatos.add(v);
                break;
            }

            if (v.second.explored) {
                //DOWN
                if (v.second.down) {
                    Pair<Integer, Integer> curPos = v.second.getPos();
                    curPos.second--;

                    if (maze.containsKey(curPos)) {
                        mouseNode w = maze.get(curPos);
                        Pair<Integer, mouseNode> insert = new Pair<>(nivel, w);
                        if (nivel <= limite && !cerrados.containsKey(insert.second.getPos())) {
                            abiertos.add(insert);
                            anteriores.put(w.getPos(), v.second);
                        }
                    } else {
                        mouseNode w = new mouseNode(curPos);
                        Pair<Integer, mouseNode> insert = new Pair<>(nivel, w);
                        if (nivel <= limite && !cerrados.containsKey(insert.second.getPos())) {
                            abiertos.add(insert);
                            anteriores.put(w.getPos(), v.second);
                            candidatos.add(insert);
                        }
                    }
                }

                //LEFT
                if (v.second.left) {
                    Pair<Integer, Integer> curPos = v.second.getPos();
                    curPos.first--;

                    if (maze.containsKey(curPos)) {
                        mouseNode w = maze.get(curPos);
                        Pair<Integer, mouseNode> insert = new Pair<>(nivel, w);
                        if (nivel <= limite && !cerrados.containsKey(insert.second.getPos())) {
                            abiertos.add(insert);
                            anteriores.put(w.getPos(), v.second);
                        }
                    } else {
                        mouseNode w = new mouseNode(curPos);
                        Pair<Integer, mouseNode> insert = new Pair<>(nivel, w);
                        if (nivel <= limite && !cerrados.containsKey(insert.second.getPos())) {
                            abiertos.add(insert);
                            anteriores.put(w.getPos(), v.second);
                            candidatos.add(insert);
                        }
                    }
                }
            }

            //RIGHT
            if (v.second.right) {
                Pair<Integer, Integer> curPos = v.second.getPos();
                curPos.first++;

                if (maze.containsKey(curPos)) {
                    mouseNode w = maze.get(curPos);
                    Pair<Integer, mouseNode> insert = new Pair<>(nivel, w);
                    if (nivel <= limite && !cerrados.containsKey(insert.second.getPos())) {
                        abiertos.add(insert);
                        anteriores.put(w.getPos(), v.second);
                    }
                } else {
                    mouseNode w = new mouseNode(curPos);
                    Pair<Integer, mouseNode> insert = new Pair<>(nivel, w);
                    if (nivel <= limite && !cerrados.containsKey(insert.second.getPos())) {
                        abiertos.add(insert);
                        anteriores.put(w.getPos(), v.second);
                        candidatos.add(insert);
                    }
                }
            }

            //UP
            if (v.second.up) {
                Pair<Integer, Integer> curPos = v.second.getPos();
                curPos.second++;

                if (maze.containsKey(curPos)) {
                    mouseNode w = maze.get(curPos);
                    Pair<Integer, mouseNode> insert = new Pair<>(nivel, w);
                    if (nivel <= limite && !cerrados.containsKey(insert.second.getPos())) {
                        abiertos.add(insert);
                        anteriores.put(w.getPos(), v.second);
                    }
                } else {
                    mouseNode w = new mouseNode(curPos);
                    Pair<Integer, mouseNode> insert = new Pair<>(nivel, w);
                    if (nivel <= limite && !cerrados.containsKey(insert.second.getPos())) {
                        abiertos.add(insert);
                        anteriores.put(w.getPos(), v.second);
                        candidatos.add(insert);
                    }
                }
            }
        }

        int targetIndex = getMinIndex(candidatos, target, rootNode);
        if (targetIndex == -1) {
            return null;
        }
        return candidatos.get(targetIndex).second;
    }

    private int getMinIndex(List<Pair<Integer, mouseNode>> nodes, Pair<Integer, Integer> target, mouseNode init) {
        if (nodes.isEmpty()) {
            return -1;
        }

        int minValue = 99999;
        int minPos = 0;

        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).second == init) {
                continue;
            }
            if (nodes.get(i).second.getPos() == target) {
                return i;
            }

            int curValue = getValue(nodes.get(i), target);

            if (curValue < minValue) {
                minPos = i;
                minValue = curValue;
            }
        }

        return minPos;
    }

    int distanciaManhattam(Pair<Integer, Integer> init, Pair<Integer, Integer> target) {
        return (Math.abs(target.first - init.first)) + (Math.abs(target.second - init.second));
    }

    private int getValue(Pair<Integer, mouseNode> init, Pair<Integer, Integer> target) {

        int distTarget = distanciaManhattam(init.second.getPos(), target);
        int costeCasilla = init.first;

        return costeCasilla * 2 + distTarget;
    }

    private int getDirection(Pair<Integer, Integer> init, Pair<Integer, Integer> target) {
        if (target.second - 1 == init.second) {
            return 1;
        } else if (target.second + 1 == init.second) {
            return 2;
        } else if (target.first - 1 == init.first) {
            return 4;
        } else {
            return 3;
        }
    }

    public boolean movimientoValido(int direccion) {
        switch (direccion) {
            case 1: //Arriba
                return getEntornoActual().getNorte().equals(OntologiaLaberinto.LIBRE);
            case 2: //Abajo
                return getEntornoActual().getSur().equals(OntologiaLaberinto.LIBRE);
            case 3: //Izquierda
                return getEntornoActual().getOeste().equals(OntologiaLaberinto.LIBRE);
            case 4: //Derecha
                return getEntornoActual().getEste().equals(OntologiaLaberinto.LIBRE);
        }
        return false;
    }

    public void aplicar(int m) {
        int x = getPosicion().getCoorX();
        int y = getPosicion().getCoorY();
        switch (m) {
            case 1:
                getPosicion().setCoorY(y + 1);
                break;
            case 2:
                getPosicion().setCoorY(y - 1);
                break;
            case 3:
                getPosicion().setCoorX(x - 1);
                break;
            case 4:
                getPosicion().setCoorX(x + 1);
                break;
        }
    }

    public Posicion getPosicion() {
        return posicion;
    }

    public EntornoLaberinto getEntornoActual() {
        return entornoActual;
    }

    public void setEntorno(EntornoLaberinto ent) {
        this.entornoActual = ent;
    }

    public void matar() {
        muerto = true;
    }

    public void setPosicion(Posicion p) {
        this.posicion = p;
    }

    public void cambiarQueso(Posicion p) {
        this.posicionQueso = p;
        nuevoQueso = true;
    }
}
