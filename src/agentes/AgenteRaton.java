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
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegos.elementos.InformarPartida;
import juegos.elementos.Jugador;
import juegos.elementos.Partida;
import juegos.elementos.PartidaAceptada;
import juegos.elementos.Posicion;
import juegos.elementos.Tablero;
import laberinto.OntologiaLaberinto;
import laberinto.elementos.EntornoLaberinto;
import laberinto.elementos.Jugada;
import laberinto.elementos.JugadaEntregada;
import laberinto.elementos.Laberinto;
import laberinto.elementos.ProponerPartida;
import laberinto.elementos.ResultadoJugada;
import util.ContenedorRaton;

/**
 *
 * @author jcsp0003
 *
 */
public class AgenteRaton extends Agent {

    private Map<String, ContenedorRaton> partidasIniciadas;

    private Jugador jugador;

    private AID[] agentesConsola;
    private ArrayList<String> mensajesPendientes;

    //Elementos para la ontologia
    private Codec codec = new SLCodec();
    private Ontology ontologia;
    private ContentManager manager = (ContentManager) getContentManager();

    @Override
    protected void setup() {
        mensajesPendientes = new ArrayList();
        partidasIniciadas = new HashMap<>();

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

        jugador = new Jugador(this.getName(), this.getAID());

        //BUSCO LA CONSOLA Y LE MANDO LOS MENSAJES
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
        //Desregristo del agente de las Páginas Amarillas
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Finaliza la ejecución del agente: " + this.getName());
    }

    private class ResponderProposicionPartida extends ProposeResponder {

        public ResponderProposicionPartida(Agent agente, MessageTemplate plantilla) {
            super(agente, plantilla);
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage propuesta) throws NotUnderstoodException {
            ProponerPartida proposicionPartida = null;
            Action ac;

            try {
                ac = (Action) manager.extractContent(propuesta);
                proposicionPartida = (ProponerPartida) ac.getAction();
            } catch (Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgenteRaton.class.getName()).log(Level.SEVERE, null, ex);
            }

            //AQUI TENGO EL TABLERO y LA INFO DE LA PARTIDA
            Partida partida = proposicionPartida.getPartida();
            Laberinto tablero = proposicionPartida.getLaberinto();
            int bombasRestantes = tablero.getNumTrampasActivas();
            Posicion posicion = tablero.getPosicionInicio();
            EntornoLaberinto entornoActual = tablero.getEntornoInicio();

            PartidaAceptada pa = new PartidaAceptada(partida, jugador);

            ContenedorRaton contenedor = new ContenedorRaton(partida.getIdPartida(), partida, tablero, posicion, entornoActual, bombasRestantes);
            partidasIniciadas.put(partida.getIdPartida(), contenedor);

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
        }
    }

    private class TareaJugarPartida extends ContractNetResponder {

        private ContenedorRaton contenedor;
        
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

            contenedor = partidasIniciadas.get(p.getIdPartida());

            Jugada jugada;
            if (contenedor.moverse()) {
                jugada = new Jugada(OntologiaLaberinto.MOVIMIENTO, contenedor.getPosicion());
            } else {
                jugada = new Jugada(OntologiaLaberinto.TRAMPA, contenedor.getPosicion());
            }

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
            ResultadoJugada resultado = null;

            try {
                resultado = (ResultadoJugada) manager.extractContent(accept);
            } catch (Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgenteRaton.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            contenedor.setEntorno(resultado.getEntorno());
            Posicion posicionAux = resultado.getNuevaPosicion();
            if (posicionAux.getCoorX() != contenedor.getPosicion().getCoorX() || posicionAux.getCoorY() != contenedor.getPosicion().getCoorY()) {
                contenedor.matar();
            }
            contenedor.setPosicion(posicionAux);

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
