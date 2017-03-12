/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import mouserun.game.*;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jcsp0003
 */
public class Laberinto extends Agent {

    //Variables del agente
    private GameUI ui;
    private int width = 10;
    private int height = 10;
    private int numberOfCheese = 20;
    private int duration = 0;

    @Override
    protected void setup() {
        //Inicializar variables del agente
        String argumentos;
        argumentos = Arrays.toString(this.getArguments());
        argumentos = argumentos.replace("[", "");
        argumentos = argumentos.replace("]", "");
        String[] arg = argumentos.split(" ");
        if (arg.length >= 1) {
            width = Integer.parseInt(arg[0]);
        }
        if (arg.length >= 2) {
            height = Integer.parseInt(arg[1]);
        }
        if (arg.length >= 3) {
            numberOfCheese = Integer.parseInt(arg[2]);
        }
        if (arg.length >= 4) {
            duration = Integer.parseInt(arg[3]);
        }
        
        //Configuración del GUI
        try {
            ui = new GameUI(width, height, numberOfCheese, duration);
        } catch (IOException ex) {
            Logger.getLogger(Laberinto.class.getName()).log(Level.SEVERE, null, ex);
        }
        ui.setVisible(true);

        //Registro del agente en las Páginas Amarrillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("GUI");
        sd.setName("Laberinto");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        //Registro de la Ontología
        //
        //
        System.out.println("Se inicia la ejecución del agente: " + this.getName());
        //Añadir las tareas principales
    }

    @Override
    protected void takeDown() {
        //Desregristo del agente de las Páginas Amarillas
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        //Liberación de recursos, incluido el GUI

        //Despedida
        System.out.println("Finaliza la ejecución del agente: " + this.getName());
    }

    //Métodos de trabajo del agente
    //Clases internas que representan las tareas del agente
}
