//20000 milisegundos no son 2 segundos
//Hay que hacer la transformacion de la x, para que corresponda a la del laberinto
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import GUI.LaberintoJFrame;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import mouserun.game.*;
import java.util.Iterator;
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
import jade.proto.ContractNetInitiator;
import jade.proto.ProposeInitiator;
import jade.proto.SubscriptionResponder;
import jade.proto.SubscriptionResponder.Subscription;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegos.elementos.DetalleInforme;
import juegos.elementos.InformarPartida;
import juegos.elementos.Jugador;
import juegos.elementos.Partida;
import juegos.elementos.PartidaAceptada;
import juegos.elementos.Posicion;
import juegos.elementos.Tablero;
import laberinto.OntologiaLaberinto;
import laberinto.elementos.EntornoLaberinto;
import laberinto.elementos.JugadaEntregada;
import laberinto.elementos.Laberinto;
import laberinto.elementos.PosicionQueso;
import laberinto.elementos.ProponerPartida;
import laberinto.elementos.ResultadoJugada;
import util.ResultadoRaton;

/**
 *
 * @author jcsp0003
 */
public class AgenteLaberinto extends Agent {

    //Variables del para la consola
    private AID[] agentesConsola;
    private AID[] agentesRaton = null;
    private ArrayList<String> mensajesPendientes;

    //Variables del laberinto
    private LaberintoJFrame myGUI;
    private GameUI laberintoGUI;
    public int ancho = 10;
    public int alto = 10;
    public int tiempo = 120000;
    public int quesosMax = 5;
    public int maxTrampas = 3;
    private Posicion posicionInicio;

    //Elementos de control de la partida
    private int numPartida;
    private Partida partidaActual;
    private boolean partidaIniciada;
    private ArrayList<ResultadoRaton> ratonesPartida;

    //Variables para la ontologia
    private ContentManager manager = (ContentManager) getContentManager();
    private Codec codec = new SLCodec();
    private Ontology ontology;

    //Control de subscripciones
    private Set<Subscription> suscripcionesJugadores;

    // Valores por defecto
    private final long TIME_OUT = 2000; // 2seg

    @Override
    protected void setup() {
        //Inicializar variables del agente
        myGUI = new LaberintoJFrame(this);
        myGUI.setVisible(true);
        mensajesPendientes = new ArrayList();
        suscripcionesJugadores = new HashSet();
        numPartida = 0;
        partidaIniciada = false;

        //REGISTRO DE LA ONTOLOGIA
        try {
            ontology = OntologiaLaberinto.getInstance();
        } catch (BeanOntologyException ex) {
            Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
        }
        manager.registerLanguage(codec);
        manager.registerOntology(ontology);
        // Anadimos la tarea para las suscripciones
        // Primero creamos el gestor de las suscripciones
        SubscriptionResponder.SubscriptionManager gestorSuscripciones = new SubscriptionResponder.SubscriptionManager() {
            @Override
            public boolean register(SubscriptionResponder.Subscription s) throws RefuseException, NotUnderstoodException {
                suscripcionesJugadores.add(s);
                return true;
            }

            @Override
            public boolean deregister(SubscriptionResponder.Subscription s) throws FailureException {
                suscripcionesJugadores.remove(s);
                return true;
            }

        };
        // Plantilla del mensaje de suscripción
        MessageTemplate plantilla = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);

        addBehaviour(new TareaInformarPartida(this, plantilla, gestorSuscripciones));
        addBehaviour(new TareaBuscarAgentes(this, 5000));
        addBehaviour(new TareaEnvioConsola(this, 500));
        mensajesPendientes.add("Inicializacion del laberinto acabada");
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

    public void empezarSistema(int t, int mq, int mt, int alt, int anc) throws IOException, InterruptedException {
        this.alto = alt;
        this.ancho = anc;
        this.tiempo = t;
        this.quesosMax = mq;
        this.maxTrampas = mt;
        laberintoGUI = new GameUI(ancho, alto);
        laberintoGUI.setVisible(true);
        addBehaviour(new TareaNuevaPartida());
    }

    public class TareaInformarPartida extends SubscriptionResponder {

        private SubscriptionResponder.Subscription suscripcionJugador;

        public TareaInformarPartida(Agent a, MessageTemplate mt, SubscriptionResponder.SubscriptionManager sm) {
            super(a, mt, sm);
        }

        @Override
        protected ACLMessage handleSubscription(ACLMessage subscription) throws NotUnderstoodException, RefuseException {
            InformarPartida Infpartida = null;

            Action ac;
            try {
                ac = (Action) manager.extractContent(subscription);
                Infpartida = (InformarPartida) ac.getAction();
            } catch (Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Registra la suscripción del Jugador
            suscripcionJugador = createSubscription(subscription);
            mySubscriptionManager.register(suscripcionJugador);

            // Responde afirmativamente con la operación
            ACLMessage agree = subscription.createReply();
            agree.setPerformative(ACLMessage.AGREE);

            mensajesPendientes.add("Suscripción registrada al agente: " + Infpartida.getJugador().getNombre());
            return agree;
        }

        @Override
        protected ACLMessage handleCancel(ACLMessage cancel) throws FailureException {
            // Eliminamos la suscripción
            mySubscriptionManager.deregister(suscripcionJugador);

            // Informe de la cancelación
            ACLMessage cancelado = cancel.createReply();
            cancelado.setPerformative(ACLMessage.INFORM);

            mensajesPendientes.add("Suscripción cancelada del agente: " + cancel.getSender().getLocalName());
            return cancelado;
        }
    }

    private class EnviarDetalleInforme extends OneShotBehaviour {

        @Override
        public void action() {

        }

    }

    public class TareaNuevaPartida extends OneShotBehaviour {

        @Override
        public void action() {
            partidaIniciada = !partidaIniciada;
            ++numPartida;
            String idPartida = myAgent.getName() + "-" + numPartida;
            partidaActual = new Partida(idPartida, OntologiaLaberinto.TIPO_JUEGO);
            Tablero tablero = new Tablero(alto, ancho);
            int xInicio = 0;
            int yInicio = 0;
            posicionInicio = new Posicion(xInicio, yInicio);
            int numCapturasQueso = quesosMax;
            int numTrampasActivas = maxTrampas;
            long maximoJuegoSeg = tiempo;
            EntornoLaberinto entInicio = laberintoGUI.getEntorno(0, 0);
            Laberinto laberinto = new Laberinto(tablero, posicionInicio, entInicio, numCapturasQueso, numTrampasActivas, maximoJuegoSeg);
            ProponerPartida propPartida = new ProponerPartida(partidaActual, laberinto);

            ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
            msg.setSender(myAgent.getAID());
            msg.setLanguage(codec.getName());
            msg.setOntology(ontology.getName());
            for (AID agentesRaton1 : agentesRaton) {
                msg.addReceiver(agentesRaton1);
            }
            msg.setReplyByDate(new Date(System.currentTimeMillis() + TIME_OUT));

            try {
                Action action = new Action(myAgent.getAID(), propPartida);
                manager.fillContent(msg, action);
            } catch (Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Creamos la tarea de ProponerPartida
            addBehaviour(new TareaProponerPartida(myAgent, msg));

            mensajesPendientes.add("Nueva Partida:\n"
                    + "    -ID de la partida: " + idPartida + "\n"
                    + "    -Posicion de inicio: " + xInicio + "-" + yInicio + "\n"
                    + "    -Numero de capturas de queso: " + numCapturasQueso + "\n"
                    + "    -Numero maximo de trampas: " + numTrampasActivas + "\n"
                    + "    -Duracion maxima: " + maximoJuegoSeg);
        }
    }

    public class TareaProponerPartida extends ProposeInitiator {

        public TareaProponerPartida(Agent agente, ACLMessage msg) {
            super(agente, msg);
        }

        @Override
        protected void handleAllResponses(Vector responses) {
            String rechazos = "Agentes que han rechazado:\n";
            int numRechazos = 0;
            ACLMessage msg;
            PartidaAceptada partida = null;
            Jugador jugador;
            ratonesPartida = new ArrayList();
            Iterator it = responses.iterator();

            // Recorremos todas las respuestas recibidas
            while (it.hasNext()) {
                msg = (ACLMessage) it.next();
                if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    try {
                        partida = (PartidaAceptada) manager.extractContent(msg);
                        jugador = partida.getJugador();
                        ratonesPartida.add(new ResultadoRaton(jugador.getAgenteJugador(), jugador.getNombre(), 0));
                    } catch (Codec.CodecException | OntologyException ex) {
                        Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    ++numRechazos;
                    rechazos = rechazos + "     El agente: " + msg.getSender().getLocalName() + " ha rechazado el juego\n";
                }
            }
            mensajesPendientes.add("Han aceptado " + ratonesPartida.size() + " ratones.");
            mensajesPendientes.add("Han rechazado " + numRechazos + " ratones.");
            if (numRechazos > 0) {
                mensajesPendientes.add(rechazos);
            }

            //GENERO EL QUESO
            try {
                laberintoGUI.nuevoQueso();
            } catch (IOException ex) {
                Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
            }

            //Genero los ratones
            try {
                laberintoGUI.generarRatones(posicionInicio, ratonesPartida);
            } catch (IOException ex) {
                Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
            }

            myAgent.addBehaviour(new TareaInicioRonda(partida.getPartida().getIdPartida()));
        }

        @Override
        protected void handleAcceptProposal(ACLMessage aceptacion) {
            mensajesPendientes.add("El agente " + aceptacion.getSender().getLocalName() + " ha ACEPTADO la proposicion de jugar");
        }

        @Override
        protected void handleRejectProposal(ACLMessage rechazo) {
            mensajesPendientes.add("El agente " + rechazo.getSender().getLocalName() + " ha RECHAZADO la proposicion de jugar");
        }

    }

    class TareaInicioRonda extends OneShotBehaviour {

        private final String idPartida;
        private final Partida partida;

        public TareaInicioRonda(String idPartida) {
            this.idPartida = idPartida;
            this.partida = new Partida(idPartida, OntologiaLaberinto.TIPO_JUEGO);
        }

        @Override
        public void action() {
            //ratonesPartida
            ACLMessage msg = new ACLMessage(ACLMessage.CFP);
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            msg.setSender(getAID());
            msg.setLanguage(codec.getName());
            msg.setOntology(ontology.getName());

            for (int i = 0; i < ratonesPartida.size(); i++) {
                msg.addReceiver(ratonesPartida.get(i).getAidRaton());
            }
            msg.setReplyByDate(new Date(System.currentTimeMillis() + TIME_OUT));

            Action ac = new Action(this.myAgent.getAID(), partida);

            try {
                manager.fillContent(msg, ac);
            } catch (Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
            }

            String mensaj = "Se ha pedido una jugada a los agentes:\n";
            for (int i = 0; i < ratonesPartida.size(); i++) {
                mensaj += "   " + ratonesPartida.get(i).getNombre() + "\n";
            }
            mensajesPendientes.add(mensaj);
            addBehaviour(new TareaJugarPartida(this.myAgent, msg));
        }
    }

    class TareaJugarPartida extends ContractNetInitiator {

        public TareaJugarPartida(Agent a, ACLMessage cfp) {
            super(a, cfp);
        }

        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances) {
            String resultado = "Recividos los siguientes movimiento:";
            JugadaEntregada jugada = null;
            List<JugadaEntregada> jugadas = new ArrayList();
            ACLMessage respuesta;
            Iterator it = responses.iterator();
            while (it.hasNext()) {
                ACLMessage msg = (ACLMessage) it.next();

                try {
                    jugada = (JugadaEntregada) manager.extractContent(msg);
                } catch (Codec.CodecException | OntologyException ex) {
                    Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
                }
                respuesta = msg.createReply();
                respuesta.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                acceptances.add(respuesta);
                jugadas.add(jugada);
                resultado += "\n    Jugador: " + jugada.getJugador().getNombre() + " Accion: " + jugada.getAccion().getJugada();
            }
            mensajesPendientes.add(resultado);
            System.out.println("---------------------------------");
            List<ResultadoJugada> resultados = laberintoGUI.hacerJugadas(jugadas, partidaActual);
            mensajesPendientes.add("Se han generado " + resultados.size() + " resultados de las " + jugadas.size() + " jugadas.");

            ACLMessage resp;
            ACLMessage msgg;
            int i = -1;
            while (it.hasNext()) {
                ++i;
                msgg = (ACLMessage) acceptances.get(i);

                try {
                    manager.fillContent(msgg, resultados.get(i));
                } catch (Codec.CodecException | OntologyException ex) {
                    Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
                }

                acceptances.set(i, msgg);

            }
        }
    }

    public class TareaBuscarAgentes extends TickerBehaviour {

        //Se buscarán agentes consola y operación
        public TareaBuscarAgentes(Agent a, long period) {
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

            template = new DFAgentDescription();
            sd = new ServiceDescription();
            sd.setName(OntologiaLaberinto.REGISTRO_RATON);
            template.addServices(sd);

            try {
                result = DFService.search(myAgent, template);
                myGUI.setNumeroRatas(String.valueOf(result.length));
                if (result.length >= 1) {
                    myGUI.activarBoton();
                    System.out.println("Se han encontrado las siguientes agentes rata:");
                    agentesRaton = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        agentesRaton[i] = result[i].getName();
                        System.out.println(agentesRaton[i].getName());
                    }
                } else {
                    myGUI.desactivarBoton();
                    System.out.println("No se han encontrado ratas");
                    agentesRaton = null;
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
