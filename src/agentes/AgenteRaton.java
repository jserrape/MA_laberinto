/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//-container -host 192.168.37.159 -agents SerranoPerez:agentes.AgenteRaton;SerranoPerezC:agentes.AgenteConsola;
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
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegos.elementos.DetalleInforme;
import juegos.elementos.GanadorPartida;
import juegos.elementos.InformarPartida;
import juegos.elementos.Jugador;
import juegos.elementos.Partida;
import juegos.elementos.PartidaAceptada;
import juegos.elementos.Posicion;
import laberinto.OntologiaLaberinto;
import laberinto.elementos.EntornoLaberinto;
import laberinto.elementos.EntregarJugada;
import laberinto.elementos.Jugada;
import laberinto.elementos.JugadaEntregada;
import laberinto.elementos.Laberinto;
import laberinto.elementos.PosicionQueso;
import laberinto.elementos.ProponerPartida;
import laberinto.elementos.ResultadoJugada;
import util.ContenedorRaton;

/**
 *
 * @author jcsp0003
 *
 */
public class AgenteRaton extends Agent {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Map<String, ContenedorRaton> partidasIniciadas;

    private Jugador jugador;

    private AID[] agentesConsola;
    private ArrayList<String> mensajesPendientes;

    //Elementos para la ontologia
    private final Codec codec = new SLCodec();
    private Ontology ontologia;
    private final ContentManager manager = (ContentManager) getContentManager();

    private Map<String, InformarPartidaSubscribe> subscribes;

    /**
     * Inicializacion de las variables e inicio de las tareas basicas
     */
    @Override
    protected void setup() {
        mensajesPendientes = new ArrayList();
        partidasIniciadas = new HashMap<>();
        subscribes = new HashMap<>();

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

    /**
     * Finalizacion el agente
     */
    @Override
    protected void takeDown() {
        //Desregristo del agente de las Páginas Amarillas
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
        }

        Iterator<Map.Entry<String, InformarPartidaSubscribe>> entries = subscribes.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, InformarPartidaSubscribe> entry = entries.next();
            entry.getValue().desRegistrarse();
        }

        System.out.println("Finaliza la ejecución del agente: " + this.getName());
    }

    /**
     * Clase para responder proposiciones de partidas
     */
    private class ResponderProposicionPartida extends ProposeResponder {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
         * Constructor parametrizado
         * @param agente Agente padre
         * @param plantilla Mensaje plantilla
         */
        public ResponderProposicionPartida(Agent agente, MessageTemplate plantilla) {
            super(agente, plantilla);
        }

        /**
         * Metodo para recibir una proposicion de partida y contestar a esta
         * @param propuesta Propuesta de partida
         * @return Mensaje de aceptacion de la partdia
         * @throws NotUnderstoodException 
         */
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

            mensajesPendientes.add("ACEPTO una proposicion de partida con id " + partida.getIdPartida() + "\n"
                    + "El entorno inicial es:\n    N:" + entornoActual.getNorte() + " S:" + entornoActual.getSur()
                    + " O:" + entornoActual.getOeste() + " E:" + entornoActual.getEste());


            if (!subscribes.containsKey(propuesta.getSender().getName())) {
                InformarPartida inf = new InformarPartida(jugador);
                ACLMessage mensaje = new ACLMessage(ACLMessage.SUBSCRIBE);
                mensaje.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
                mensaje.setSender(this.myAgent.getAID());
                mensaje.setLanguage(codec.getName());
                mensaje.setOntology(ontologia.getName());
                mensaje.addReceiver(propuesta.getSender());

                try {
                    Action action = new Action(getAID(), inf);
                    manager.fillContent(mensaje, action);
                } catch (Codec.CodecException | OntologyException ex) {
                    Logger.getLogger(AgenteRaton.class.getName()).log(Level.SEVERE, null, ex);
                }

                InformarPartidaSubscribe tarea = new InformarPartidaSubscribe(this.myAgent, mensaje);
                subscribes.put(propuesta.getSender().getName(), tarea);

                addBehaviour(tarea);
            }

            return agree;
        }
    }

    /**
     * Tarea de gestion de suscripciones
     */
    private class InformarPartidaSubscribe extends SubscriptionInitiator {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private AID sender;

        /**
         * Constructoir parametrizado
         * @param agente Agente padre
         * @param mensaje  Mensaje plantilla
         */
        public InformarPartidaSubscribe(Agent agente, ACLMessage mensaje) {
            super(agente, mensaje);
        }

        /**
         * Maneja la respuesta en caso que acepte: AGREE
         * @param inform Mensaje INFORM
         */
        @Override
        protected void handleAgree(ACLMessage inform) {
            mensajesPendientes.add("Mi subscripcion a la plataforma ha sido aceptada");
            this.sender = inform.getSender();
        }


        /**
         * Maneja la respuesta en caso que rechace: REFUSE
         * @param inform Mensaje REFUSE
         */
        @Override
        protected void handleRefuse(ACLMessage inform) {
            mensajesPendientes.add("Mi subscripcion a la plataforma ha sido rechazada");
        }

        
        /**
         * Se ha recibido un DetalleInforme
         * @param inform Mensaje INFORM
         */
        @Override
        protected void handleInform(ACLMessage inform) {
            try {
                DetalleInforme detalle = (DetalleInforme) manager.extractContent(inform);
                if (detalle.getDetalle() instanceof PosicionQueso) {
                    PosicionQueso pos = (PosicionQueso) detalle.getDetalle();
                    ContenedorRaton contenedor = partidasIniciadas.get(pos.getPartida().getIdPartida());
                    contenedor.cambiarQueso(pos.getPosicion());
                } else {
                    if (detalle.getDetalle() instanceof juegos.elementos.Error) {
                        juegos.elementos.Error err = (juegos.elementos.Error) detalle.getDetalle();
                        mensajesPendientes.add("Ha habido un error:\n  " + err.getDetalle());
                    } else {
                        if (detalle.getDetalle() instanceof GanadorPartida) {
                            GanadorPartida ganador = (GanadorPartida) detalle.getDetalle();
                            mensajesPendientes.add("Ha llegado un ganador de partida:\n     ID: " + detalle.getPartida().getIdPartida() + "\n     Ganador: " + ganador.getJugador().getNombre());
                        } else {
                            mensajesPendientes.add("No se ha podido identificar el tipo de objeto del subscribe");
                        }
                    }
                }
            } catch (Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgenteRaton.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        /**
         * Funcion para el desRegistro de la suscripcion del laberinto
         */
        public void desRegistrarse() {
            //Enviamos la cancelación de la suscripcion
            this.cancel(sender, false);

            //Comprobamos que se ha cancelado correctamente
            this.cancellationCompleted(sender);
        }

        
        /**
         * Maneja la respuesta en caso de fallo: FAILURE
         * @param failure 
         */
        @Override
        protected void handleFailure(ACLMessage failure) {

        }

        @Override
        public void cancellationCompleted(AID agente) {
        }
    }

    /**
     * Tarea de negociacion de la partida
     */
    private class TareaJugarPartida extends ContractNetResponder {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private ContenedorRaton contenedor;

        /**
         * Constructor parametrizado
         * @param agente Agente padre
         * @param plantilla Mensaje plantilla
         */
        public TareaJugarPartida(Agent agente, MessageTemplate plantilla) {
            super(agente, plantilla);
        }

        /**
         * Llegada de un cfp para pedir una jugada 
         * @param cfp Mensaje cfp
         * @return Aceptacion de realizar una jugada
         * @throws NotUnderstoodException
         * @throws RefuseException 
         */
        @Override
        protected ACLMessage prepareResponse(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
            Action ac;
            Partida p = null;
            EntregarJugada jug = null;
            try {
                ac = (Action) manager.extractContent(cfp);
                jug = (EntregarJugada) ac.getAction();
                p = jug.getPartida();
            } catch (Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgenteRaton.class.getName()).log(Level.SEVERE, null, ex);
            }
            // mensajesPendientes.add("Me ha llegado una peticion de ronda para la partida con id=" + p.getIdPartida());

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

        /**
         * Resultado de la jugada realizada
         * @param cfp Mensaje cfp
         * @param propose Propose realizado
         * @param accept Mensaje con el contenido de mi resultado de jugada
         * @return Mensaje INFORM
         * @throws FailureException 
         */
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

    /**
     * Tarea que busca agentes consola por donde se mostrarán los mensajes de mensajesPendientes
     */
    public class TareaBuscarConsolas extends TickerBehaviour {


        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
         * Constructor parametrizado
         * @param a Agente padre
         * @param period Periodo de repeticion
         */
        public TareaBuscarConsolas(Agent a, long period) {
            super(a, period);
        }

        /**
         * Busca los agente consola y los almacena
         */
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

    /**
     * Tarea que se encarga de enviar los mensajes de mensajesPendientes a las consolas encontradas
     */
    public class TareaEnvioConsola extends TickerBehaviour {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public TareaEnvioConsola(Agent a, long period) {
            super(a, period);
        }

        /**
         * Realiza un envio de mensajes a la consola
         */
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
