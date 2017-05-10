/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

/**
 *
 * @author jcsp0003
 */
/**
 * Almacena una posición(x,y) y las direcciones accesibles desde la misma. Esto
 * último, solo será válido si el nodo está marcado como explored.
 */
class mouseNode {

    public int x;
    public int y;

    public boolean up;
    public boolean down;
    public boolean left;
    public boolean right;

    public boolean explored;

    public mouseNode(int _x, int _y, boolean _up, boolean _down, boolean _left, boolean _right) {
        x = _x;
        y = _y;

        up = _up;
        down = _down;
        left = _left;
        right = _right;
        explored = true;
    }

    public mouseNode(Pair<Integer, Integer> pos, boolean _up, boolean _down, boolean _left, boolean _right) {
        this(pos.first, pos.second, _up, _down, _left, _right);
    }

    public mouseNode(int _x, int _y) {
        x = _x;
        y = _y;
        explored = false;
    }

    public mouseNode(Pair<Integer, Integer> pos) {
        this(pos.first, pos.second);
    }

    public Pair<Integer, Integer> getPos() {
        return new Pair(x, y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof mouseNode)) {
            return false;
        }
        mouseNode node = (mouseNode) o;
        return x == node.x && y == node.y;
    }

    @Override
    public int hashCode() {
        return x * 10000 + y;
    }

    @Override
    public String toString() {
        return "X: " + x + " Y: " + y;
    }
}
