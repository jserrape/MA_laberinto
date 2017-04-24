/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import GUI.ConsolaJFrame;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import java.util.Iterator;
import laberinto.OntologiaLaberinto;
import util.MensajeConsola;

/**
 *
 * @author pedroj
 */
public class AgenteConsola extends Agent {
    private ArrayList<ConsolaJFrame> myGui;
    private ArrayList<MensajeConsola> mensajesPendientes;
    
    /**
     * Se ejecuta cuando se inicia el agente
     */
    @Override
    protected void setup() {
        //Incialización de variables
        myGui = new ArrayList();
        mensajesPendientes = new ArrayList();
        
        //Regisro de la Ontología
        
        //Registro en Página Amarillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
	ServiceDescription sd = new ServiceDescription();
	sd.setType(OntologiaLaberinto.REGISTRO_CONSOLA);
	sd.setName(OntologiaLaberinto.REGISTRO_CONSOLA);
	dfd.addServices(sd);
	try {
            DFService.register(this, dfd);
	}
	catch (FIPAException fe) {
            fe.printStackTrace();
	}
        
        // Se añaden las tareas principales
       addBehaviour(new TareaRecepcionMensajes());
    }
    
    /**
     * Se ejecuta al finalizar el agente
     */
    @Override
    protected void takeDown() {
        //Desregistro de las Páginas Amarillas
        try {
            DFService.deregister(this);
	}
            catch (FIPAException fe) {
            fe.printStackTrace();
	}
        
        //Se liberan los recuros y se despide
        
        cerrarConsolas();
        System.out.println("Finaliza la ejecución de " + this.getName());
    }

    //Métodos de utilidad para el agente consola
    private ConsolaJFrame buscarConsola(String nombreAgente) {
        // Obtenemos la consola donde se presentarán los mensajes
        Iterator<ConsolaJFrame> it = myGui.iterator();
        while (it.hasNext()) {
            ConsolaJFrame gui = it.next();
            if (gui.getNombreAgente().compareTo(nombreAgente) == 0)
                return gui;
        }
        
        return null;
    }
    
    private void cerrarConsolas() {
        //Se eliminan las consolas que están abiertas
        Iterator<ConsolaJFrame> it = myGui.iterator();
        while (it.hasNext()) {
            ConsolaJFrame gui = it.next();
            gui.dispose();
        }
    }
    
    //Tareas del agente consola
    public class TareaRecepcionMensajes extends CyclicBehaviour {

        @Override
        public void action() {
            //Solo se atenderán mensajes INFORM
            MessageTemplate plantilla = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage mensaje = myAgent.receive(plantilla);
            if (mensaje != null) {
                //procesamos el mensaje
                MensajeConsola mensajeConsola = new MensajeConsola(mensaje.getSender().getName(),
                                    mensaje.getContent());
                mensajesPendientes.add(mensajeConsola);
                addBehaviour(new TareaPresentarMensaje());
            } 
            else
                block();
            
        }
    
    }
    
    public class TareaPresentarMensaje extends OneShotBehaviour {

        @Override
        public void action() {
            //Se coge el primer mensaje
            MensajeConsola mensajeConsola = mensajesPendientes.remove(0);
            
            //Se busca la ventana de consola o se crea una nueva
            ConsolaJFrame gui = buscarConsola(mensajeConsola.getNombreAgente());
            if (gui == null) {
                gui = new ConsolaJFrame(mensajeConsola.getNombreAgente());
                myGui.add(gui);
            } 
            
            gui.presentarSalida(mensajeConsola);
        }
        
    }
}
