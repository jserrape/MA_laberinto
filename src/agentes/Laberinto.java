/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.core.AID;
import mouserun.game.*;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SubscriptionResponder;
import jade.proto.SubscriptionResponder.Subscription;
import jade.proto.SubscriptionResponder.SubscriptionManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import laberinto.OntologiaLaberinto;

/**
 *
 * @author jcsp0003
 */
public class Laberinto extends Agent {

    private Set<Subscription> suscripciones = new HashSet<>();

    //Variables del agente
    private AID[] agentesConsola;
    private ArrayList<String> mensajesPendientes;
    private GameUI laberinto;
    private int width = 10;
    private int height = 10;

    private ContentManager manager = (ContentManager) getContentManager();

    // El lenguaje utilizado por el agente para la comunicación es SL 
    private Codec codec = new SLCodec();

    // La ontología que utilizará el agente
    private Ontology ontology;

    @Override
    protected void setup() {
        //Inicializar variables del agente
        mensajesPendientes = new ArrayList();
        String argumentos;
        argumentos = Arrays.toString(this.getArguments());
        argumentos = argumentos.replace("[", "");
        argumentos = argumentos.replace("]", "");
        String[] arg = argumentos.split(" ");
        if (arg.length >= 1) {
            if (!"".equals(arg[0])) {
                width = Integer.parseInt(arg[0]);
            }
        }
        if (arg.length >= 2) {
            height = Integer.parseInt(arg[1]);
        }

        //Configuración del GUI
        try {
            laberinto = new GameUI(width, height);
        } catch (IOException ex) {
            Logger.getLogger(Laberinto.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Laberinto.class.getName()).log(Level.SEVERE, null, ex);
        }
        laberinto.setVisible(true);
        ///////////////////////////////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////////
        //Obtenemos la instancia de la ontología y registramos el lenguaje
        //y la ontología para poder completar el contenido de los mensajes
        try {
            ontology = OntologiaLaberinto.getInstance();
        } catch (BeanOntologyException ex) {
            Logger.getLogger(Laberinto.class.getName()).log(Level.SEVERE, null, ex);
        }

        manager.registerLanguage(codec);
        manager.registerOntology(ontology);

        System.out.println("El agente " + getName() + " esperando para CFP...");

        //Registro del agente en las páginas amarillas
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setName(getLocalName());
            sd.setType(OntologiaLaberinto.REGISTRO_LABERINTO);
            // Agents that want to use this service need to "know" the weather-forecast-ontology
            sd.addOntologies(OntologiaLaberinto.ONTOLOGY_NAME);
            // Agents that want to use this service need to "speak" the FIPA-SL language
            sd.addLanguages(FIPANames.ContentLanguage.FIPA_SL);
            dfd.addServices(sd);

            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println(this.getLocalName() + ": Esperando suscripciones...");
        MessageTemplate template = SubscriptionResponder.createMessageTemplate(ACLMessage.SUBSCRIBE);
        //Se añade un comportamiento que cada 5 segundos envía un mensaje a todos los suscriptores.
        //this.addBehaviour(new EnviarSemanal(this, (long) 5000));

        //Se añade un comportamiento que maneja los mensajes recibidos para suscribirse.
        //Habrá que crear primero el SubscriptionManager que registrará y eliminará las suscripciones.
        SubscriptionManager gestor = new SubscriptionManager() {

            public boolean register(Subscription suscripcion) {
                suscripciones.add(suscripcion);
                return true;
            }

            public boolean deregister(Subscription suscripcion) {
                suscripciones.remove(suscripcion);
                return true;
            }
        };
        this.addBehaviour(new HacerSuscripcion(this, template, gestor));

        //Añadir las tareas principales
        addBehaviour(new TareaBuscarConsola(this, 5000));
        addBehaviour(new TareaEnvioConsola());
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
    public ArrayList<String> getMensajesPendientes() {
        return mensajesPendientes;
    }

    //Clases internas que representan las tareas del agente
    public class TareaEnvioConsola extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage mensaje;
            if (agentesConsola != null) {
                if (!mensajesPendientes.isEmpty()) {
                    mensaje = new ACLMessage(ACLMessage.INFORM);
                    mensaje.setSender(myAgent.getAID());
                    mensaje.addReceiver(agentesConsola[0]);
                    mensaje.setContent(mensajesPendientes.remove(0));

                    myAgent.send(mensaje);
                } else {
                    block();
                }
            }
        }
    }

    public class TareaBuscarConsola extends TickerBehaviour {

        //Se buscarán consolas 
        public TareaBuscarConsola(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            //Busca agentes consola
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setName("Consola");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                if (result.length > 0) {
                    agentesConsola = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        agentesConsola[i] = result[i].getName();
                    }
                } else {
                    //No se han encontrado agentes consola
                    agentesConsola = null;
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }

    private class HacerSuscripcion extends SubscriptionResponder {

        private Subscription suscripcion;

        public HacerSuscripcion(Agent agente, MessageTemplate plantilla, SubscriptionManager gestor) {
            super(agente, plantilla, gestor);
        }

        //Método que maneja la suscripcion
        @Override
        protected ACLMessage handleSubscription(ACLMessage propuesta) throws NotUnderstoodException, RefuseException {
            System.out.printf("%s: SUSCRIBE recibido de %s.\n", Laberinto.this.getLocalName(), propuesta.getSender().getLocalName());
            System.out.printf("%s: La propuesta es: %s.\n", Laberinto.this.getLocalName(), propuesta.getContent());
            //Crea la suscripcion
            this.suscripcion = this.createSubscription(propuesta);

            //El SubscriptionManager registra la suscripcion
            this.mySubscriptionManager.register(suscripcion);

            //Acepta la propuesta y la envía
            ACLMessage agree = propuesta.createReply();
            agree.setPerformative(ACLMessage.AGREE);
            return agree;
        }
    }

    private class PosicionQueso extends OneShotBehaviour {

        @Override
        public void action() {
            //Se crea y rellena el mensaje con la información que desea enviar.
            ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);
            mensaje.setContent("Simulacion de mensaje");

            //Se envía un mensaje a cada suscriptor
            for (Subscription suscripcion : Laberinto.this.suscripciones) {
                suscripcion.notify(mensaje);
            }
        }
    }
    
    private class GanadorPartida extends OneShotBehaviour {

        @Override
        public void action() {
            //Se crea y rellena el mensaje con la información que desea enviar.
            ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);
            mensaje.setContent("Simulacion de mensaje");

            //Se envía un mensaje a cada suscriptor
            for (Subscription suscripcion : Laberinto.this.suscripciones) {
                suscripcion.notify(mensaje);
            }
        }
    }

}
