/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import jade.content.ContentManager;
import jade.content.Predicate;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegos.elementos.DetalleInforme;
import juegos.elementos.InformarPartida;
import juegos.elementos.Jugador;
import juegos.elementos.Partida;
import laberinto.OntologiaLaberinto;

/**
 *
 * @author jcsp0003
 *
 */
public class AgenteRaton extends Agent {

    private AID[] agentesConsola;
    private ArrayList<String> mensajesPendientes;

    //Elementos para la ontologia
    private Codec codec = new SLCodec();
    private Ontology ontologia;
    private ContentManager manager = (ContentManager) getContentManager();

    @Override
    protected void setup() {
        mensajesPendientes = new ArrayList();

        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        try {
            ontologia = OntologiaLaberinto.getInstance();
            manager.registerLanguage(codec);
            manager.registerOntology(ontologia);
        } catch (BeanOntologyException ex) {
            Logger.getLogger(AgenteRaton.class.getName()).log(Level.SEVERE, null, ex);
        }
        sd.addOntologies(OntologiaLaberinto.ONTOLOGY_NAME);
        //registro paginas amarillas
        try {
            sd.setName(OntologiaLaberinto.REGISTRO_RATON);
            sd.setType(OntologiaLaberinto.REGISTRO_RATON);
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
        }

        //Se crea un mensaje de tipo SUBSCRIBE y se asocia al protocolo FIPA-Subscribe.
        //Partida p = new Partida(this.getLocalName(), "Base");
        Jugador jugador = new Jugador(this.getLocalName(), this.getAID());
        InformarPartida inf = new InformarPartida(jugador);

        ACLMessage mensaje = new ACLMessage(ACLMessage.SUBSCRIBE);
        mensaje.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
        mensaje.setSender(this.getAID());
        mensaje.setLanguage(codec.getName());
        mensaje.setOntology(ontologia.getName());

        try {
            Action action = new Action(getAID(), inf);
            manager.fillContent(mensaje, action);
        } catch (Codec.CodecException | OntologyException ex) {
            Logger.getLogger(AgenteRaton.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Se añade el destinatario del mensaje
        AID id = new AID();
        id.setLocalName(OntologiaLaberinto.REGISTRO_LABERINTO);
        mensaje.addReceiver(id);

        //ME REGISTRO AL SUBSCRIBE
        addBehaviour(new InformarPartidaSubscribe(this, mensaje));

        addBehaviour(new TareaBuscarConsolas(this, 5000));
        addBehaviour(new TareaEnvioConsola(this, 500));
        mensajesPendientes.add("Inicializacion del raton " + this.getLocalName() + " acabada.");
    }

    @Override
    protected void takeDown() {
        //Desregristo del agente de las Páginas Amarillas

        //Liberación de recursos, incluido el GUI
        //Despedida
        System.out.println("Finaliza la ejecución del agente: " + this.getName());

    }

    private class InformarPartidaSubscribe extends SubscriptionInitiator {

        public InformarPartidaSubscribe(Agent agente, ACLMessage mensaje) {
            super(agente, mensaje);
        }

        //Maneja la respuesta en caso que acepte: AGREE
        @Override
        protected void handleAgree(ACLMessage inform) {
            mensajesPendientes.add("Mi subscripcion al laberinto ha sido aceptada");
        }

        // Maneja la respuesta en caso que rechace: REFUSE
        @Override
        protected void handleRefuse(ACLMessage inform) {
            mensajesPendientes.add("Mi subscripcion al laberinto ha sido rechazada");
        }

        //Maneja la informacion enviada: INFORM
        @Override
        protected void handleInform(ACLMessage detalle) {
            mensajesPendientes.add("Me ha llegado algo por el subscribe");
        }

        //Maneja la respuesta en caso de fallo: FAILURE
        @Override
        protected void handleFailure(ACLMessage failure) {

        }

        @Override
        public void cancellationCompleted(AID agente) {
        }
    }

    public class TareaBuscarConsolas extends TickerBehaviour {

        //Se buscarán agentes consola y operación
        public TareaBuscarConsolas(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            DFAgentDescription template;
            ServiceDescription sd;
            DFAgentDescription[] result;

            //Busca agentes consola
            template = new DFAgentDescription();
            sd = new ServiceDescription();
            sd.setName(OntologiaLaberinto.REGISTRO_CONSOLA);
            template.addServices(sd);

            try {
                result = DFService.search(myAgent, template);
                if (result.length > 0) {
                    agentesConsola = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        agentesConsola[i] = result[i].getName();
                    }
                } else {
                    System.out.println("No se han encontrado consolas:");
                    agentesConsola = null;
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }

    public class TareaEnvioConsola extends TickerBehaviour {

        public TareaEnvioConsola(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            ACLMessage mensaje;
            if (agentesConsola != null) {
                if (!mensajesPendientes.isEmpty()) {
                    System.out.println("Empieza el envío");
                    mensaje = new ACLMessage(ACLMessage.INFORM);
                    mensaje.setSender(myAgent.getAID());
                    mensaje.addReceiver(agentesConsola[0]);
                    mensaje.setContent(mensajesPendientes.remove(0));

                    // 
                    System.out.println("Enviado a: " + agentesConsola[0].getName());
                    System.out.println("Contenido: " + mensaje.getContent());

                    myAgent.send(mensaje);
                } else {
                    mensaje = new ACLMessage(ACLMessage.INFORM);
                    mensaje.setSender(myAgent.getAID());
                    mensaje.addReceiver(agentesConsola[0]);
                    mensaje.setContent("No hay mensajes pendientes");
                }
            }
        }
    }

}
