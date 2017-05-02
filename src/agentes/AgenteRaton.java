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
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import jade.proto.ProposeResponder;
import jade.proto.SubscriptionInitiator;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegos.elementos.DetalleInforme;
import juegos.elementos.InformarPartida;
import juegos.elementos.Jugador;
import juegos.elementos.Partida;
import juegos.elementos.PartidaAceptada;
import juegos.elementos.Posicion;
import laberinto.OntologiaLaberinto;
import laberinto.elementos.EntornoLaberinto;
import laberinto.elementos.Jugada;
import laberinto.elementos.JugadaEntregada;
import laberinto.elementos.Laberinto;
import laberinto.elementos.ProponerPartida;
import laberinto.elementos.ResultadoJugada;
import util.ResultadoRaton;

/**
 *
 * @author jcsp0003
 *
 */
public class AgenteRaton extends Agent {

    private Jugador jugador;

    //Elementos de la partida
    private Partida partida;
    private Laberinto tablero;
    private Posicion posicion;
    private EntornoLaberinto entornoActual;

    private AID[] agentesConsola;
    private ArrayList<String> mensajesPendientes;

    //Elementos para la ontologia
    private Codec codec = new SLCodec();
    private Ontology ontologia;
    private ContentManager manager = (ContentManager) getContentManager();

    //Atributos para control del juego
    private boolean jugando;

    @Override
    protected void setup() {
        mensajesPendientes = new ArrayList();
        jugando = false;

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
        jugador = new Jugador(this.getName(), this.getAID());
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

        //BUSCO LA CONSULA Y LE MANDO LOS MENSAJES
        addBehaviour(new TareaBuscarConsolas(this, 5000));
        addBehaviour(new TareaEnvioConsola(this, 500));

        //LEO LAS PROPOSICIONES DE PARTIDA
        MessageTemplate plantilla = ProposeResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        addBehaviour(new ResponderProposicionPartida(this, plantilla));
        mensajesPendientes.add("Inicializacion del raton " + this.getLocalName() + " acabada.");

        //Leo las rondas
        MessageTemplate template = ContractNetResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        addBehaviour(new TareaJugarPartida(this, template));
    }

    @Override
    protected void takeDown() {
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

    private class ResponderProposicionPartida extends ProposeResponder {

        public ResponderProposicionPartida(Agent agente, MessageTemplate plantilla) {
            super(agente, plantilla);
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage propuesta) throws NotUnderstoodException {
            if (!jugando) {
                jugando = !jugando;
                ProponerPartida proposicionPartida = null;
                Action ac;

                try {
                    ac = (Action) manager.extractContent(propuesta);
                    proposicionPartida = (ProponerPartida) ac.getAction();
                } catch (Codec.CodecException | OntologyException ex) {
                    Logger.getLogger(AgenteRaton.class.getName()).log(Level.SEVERE, null, ex);
                }

                //AQUI TENGO EL TABLERO y LA INFO DE LA PARTIDA
                partida = proposicionPartida.getPartida();
                tablero = proposicionPartida.getLaberinto();
                posicion = tablero.getPosicionInicio();
                entornoActual = tablero.getEntornoInicio();

                Jugador j = new Jugador(this.myAgent.getName(), this.myAgent.getAID());
                PartidaAceptada pa = new PartidaAceptada(partida, j);

                ACLMessage agree = propuesta.createReply();
                agree.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                agree.setLanguage(codec.getName());
                agree.setOntology(ontologia.getName());

                try {
                    manager.fillContent(agree, pa);
                } catch (Codec.CodecException | OntologyException ex) {
                    Logger.getLogger(AgenteRaton.class.getName()).log(Level.SEVERE, null, ex);
                }

                mensajesPendientes.add("ACEPTO una proposicion de partida con id " + partida.getIdPartida());
                mensajesPendientes.add("El entorno inicial es:\n    N:" + entornoActual.getNorte() + " S:" + entornoActual.getSur()
                        + " O:" + entornoActual.getOeste() + " E:" + entornoActual.getEste());

                return agree;
            } else {
                ProponerPartida proposicionPartida = null;
                Action ac;

                try {
                    ac = (Action) manager.extractContent(propuesta);
                    proposicionPartida = (ProponerPartida) ac.getAction();
                } catch (Codec.CodecException | OntologyException ex) {
                    Logger.getLogger(AgenteRaton.class.getName()).log(Level.SEVERE, null, ex);
                }

                Partida partida = proposicionPartida.getPartida();
                mensajesPendientes.add("RECHAZO una proposicion de partida con id " + partida.getIdPartida());

                ACLMessage desagree = propuesta.createReply();
                desagree.setPerformative(ACLMessage.REJECT_PROPOSAL);
                desagree.setLanguage(codec.getName());
                desagree.setOntology(ontologia.getName());

                return desagree;
            }
        }
    }

    private class TareaJugarPartida extends ContractNetResponder {

        public TareaJugarPartida(Agent agente, MessageTemplate plantilla) {
            super(agente, plantilla);
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
            Action ac;
            Partida p = null;
            try {
                ac = (Action) manager.extractContent(cfp);
                p = (Partida) ac.getAction();
            } catch (Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgenteRaton.class.getName()).log(Level.SEVERE, null, ex);
            }
            mensajesPendientes.add("Me ha llegado una peticion de ronda para la partida con id=" + p.getIdPartida());

            //////////////////
            /////////////////   AQUI DECIDO EL MOVIMIENTO QUE VOY A HACER
            ////////////////
            String puedo = "Puedo:";
            ArrayList<String> mov = new ArrayList();
            if (entornoActual.getNorte().equals(OntologiaLaberinto.LIBRE)) {
                mov.add("Subir");
                puedo += " Subir";
            }
            if (entornoActual.getSur().equals(OntologiaLaberinto.LIBRE)) {
                mov.add("Bajar");
                puedo += " Bajar";
            }
            if (entornoActual.getOeste().equals(OntologiaLaberinto.LIBRE)) {
                mov.add("Izquierda");
                puedo += " Izquierda";
            }
            if (entornoActual.getEste().equals(OntologiaLaberinto.LIBRE)) {
                mov.add("Derecha");
                puedo += " Derecha";
            }
            mensajesPendientes.add(puedo);
            int n = (int) (mov.size() * Math.random());
            String acc = mov.get(n);
            int x = posicion.getCoorX();
            int y = posicion.getCoorY();
            mensajesPendientes.add("Mi posicion es la " + posicion.toString());
            switch (acc) {
                case "Subir":
                    posicion.setCoorY(y + 1);
                    break;
                case "Bajar":
                    posicion.setCoorY(y - 1);
                    break;
                case "Izquierda":
                    posicion.setCoorX(x - 1);
                    break;
                case "Derecha":
                    posicion.setCoorX(x + 1);
                    break;
            }
            mensajesPendientes.add("Me muevo a la posicion " + posicion.toString());
            mensajesPendientes.add("Selecciono " + acc);

            Jugada jugada = new Jugada(OntologiaLaberinto.MOVIMIENTO, posicion);
            JugadaEntregada jugEntregada = new JugadaEntregada(p, jugador, jugada);

            ACLMessage respuesta = cfp.createReply();
            respuesta.setPerformative(ACLMessage.PROPOSE);
            respuesta.setSender(myAgent.getAID());
            respuesta.setLanguage(codec.getName());
            respuesta.setOntology(ontologia.getName());

            try {
                manager.fillContent(respuesta, jugEntregada);
            } catch (Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgenteRaton.class.getName()).log(Level.SEVERE, null, ex);
            }

            return respuesta;
        }

        @Override
        protected ACLMessage prepareResultNotification(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
            mensajesPendientes.add("Me ha llegado un ResultadoJugada");
            ResultadoJugada resultado = null;

            try {
                resultado = (ResultadoJugada) manager.extractContent(accept);
            } catch (Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgenteRaton.class.getName()).log(Level.SEVERE, null, ex);
            }

            entornoActual = resultado.getEntorno();
            posicion = resultado.getNuevaPosicion();
            mensajesPendientes.add("Me confirman que stoy en la posicion " + posicion.toString());
            //mensajesPendientes.add(resultado.toString());

            ACLMessage inform = accept.createReply();
            inform.setPerformative(ACLMessage.INFORM);
            return inform;
        }

        @Override
        protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
            mensajesPendientes.add("Me ha llegado algo al handleRejectProposal");
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
                    //System.out.println("No se han encontrado consolas:");
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
                    //System.out.println("Empieza el envío");
                    mensaje = new ACLMessage(ACLMessage.INFORM);
                    mensaje.setSender(myAgent.getAID());
                    mensaje.addReceiver(agentesConsola[0]);
                    mensaje.setContent(mensajesPendientes.remove(0));

                    //System.out.println("Enviado a: " + agentesConsola[0].getName());
                    //System.out.println("Contenido: " + mensaje.getContent());

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
