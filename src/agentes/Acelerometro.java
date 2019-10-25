package com.example.sensoresmovil;

import android.hardware.Sensor;

public class Acelerometro {
    private double ejeX;
    private double ejeY;
    private double ejeZ;
    private String fecha;

    public Acelerometro(){
        setEjeX(0.0);
        setEjeY(0.0);
        setEjeZ(0.0);
        setFecha("");
    }

    public Acelerometro(double x, double y, double z, String f){
        setEjeX(x);
        setEjeY(y);
        setEjeZ(z);
        setFecha(f);
    }

    public double getEjeX() {
        return ejeX;
    }

    public void setEjeX(double ejeX) {
        this.ejeX = ejeX;
    }

    public double getEjeY() {
        return ejeY;
    }

    public void setEjeY(double ejeY) {
        this.ejeY = ejeY;
    }

    public double getEjeZ() {
        return ejeZ;
    }

    public void setEjeZ(double ejeZ) {
        this.ejeZ = ejeZ;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}
