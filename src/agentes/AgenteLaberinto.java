/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
// -gui -agents rata1:agentes.AgenteRaton;rata2:agentes.AgenteRaton;rata3:agentes.AgenteRaton;rata4:agentes.AgenteRaton;rata5:agentes.AgenteRaton;rata6:agentes.AgenteRaton;rata7:agentes.AgenteRaton;rata8:agentes.AgenteRaton;rata9:agentes.AgenteRaton;;laberinto:agentes.AgenteLaberinto;
// -gui -agents rata1:agentes.AgenteRaton;laberinto:agentes.AgenteLaberinto;consola:agentes.AgenteConsola;
// -gui -agents rata1:agentes.AgenteRaton;rata2:agentes.AgenteRaton;rata3:agentes.AgenteRaton;laberinto:agentes.AgenteLaberinto;consola:agentes.AgenteConsola;
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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegos.elementos.Jugador;
import juegos.elementos.Partida;
import juegos.elementos.PartidaAceptada;
import juegos.elementos.Posicion;
import juegos.elementos.Tablero;
import laberinto.OntologiaLaberinto;
import laberinto.elementos.EntornoLaberinto;
import laberinto.elementos.EntregarJugada;
import laberinto.elementos.JugadaEntregada;
import laberinto.elementos.Laberinto;
import laberinto.elementos.ProponerPartida;
import laberinto.elementos.ResultadoJugada;
import util.ContenedorLaberinto;
import util.GestorSuscripciones;
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

    //Elementos de control de la partida
    private int numPartida;
    private Map<String, ContenedorLaberinto> partidasIniciadas;

    //Variables para la ontologia
    private ContentManager manager = (ContentManager) getContentManager();
    private Codec codec = new SLCodec();
    private Ontology ontology;

    private TareaInformarPartida eventosLaberinto;
    private GestorSuscripciones gestor;

    // Valores por defecto
    private final long TIME_OUT = 300; // 2seg

    @Override
    protected void setup() {
        //Inicializar variables del agente
        myGUI = new LaberintoJFrame(this);
        myGUI.setVisible(true);
        mensajesPendientes = new ArrayList();
        numPartida = 0;

        gestor = new GestorSuscripciones();

        partidasIniciadas = new HashMap<>();

        //REGISTRO DE LA ONTOLOGIA
        try {
            ontology = OntologiaLaberinto.getInstance();
        } catch (BeanOntologyException ex) {
            Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
        }
        manager.registerLanguage(codec);
        manager.registerOntology(ontology);

        addBehaviour(new TareaBuscarAgentes(this, 5000));
        addBehaviour(new TareaEnvioConsola(this, 500));
        mensajesPendientes.add("Inicializacion del laberinto acabada");

        // Anadimos la tarea para las suscripciones
        // Plantilla del mensaje de suscripción
        MessageTemplate plantilla = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
        eventosLaberinto = new TareaInformarPartida(this, plantilla, gestor);
        addBehaviour(eventosLaberinto);
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
        ++numPartida;
        String iid = this.getName() + numPartida;
        ContenedorLaberinto cont = new ContenedorLaberinto(t, mq, mt, alt, anc, iid,gestor,codec,ontology,manager);
        partidasIniciadas.put(iid, cont);

        addBehaviour(new TareaNuevaPartida(iid));
    }

    class TareaInformarPartida extends SubscriptionResponder {

        private Subscription suscripcionJugador;

        public TareaInformarPartida(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        public TareaInformarPartida(Agent a, MessageTemplate mt, SubscriptionManager sm) {
            super(a, mt, sm);
        }

        @Override
        protected ACLMessage handleSubscription(ACLMessage subscription) throws NotUnderstoodException, RefuseException {

            String nombreAgente = subscription.getSender().getName();

            // Registra la suscripción del Jugador si no hay una previa
            suscripcionJugador = createSubscription(subscription);
            if (!gestor.haySuscripcion(nombreAgente)) {
                mySubscriptionManager.register(suscripcionJugador);
                mensajesPendientes.add("Suscripción registrada al agente: "
                        + nombreAgente + "\nnúmero de suscripciones: "
                        + gestor.numSuscripciones());
            } else {
                // Ya tenemos una suscripción anterior del jugador y no 
                // volvemos a registrarlo.
                mensajesPendientes.add("Suscripción ya registrada al agente: "
                        + nombreAgente);
            }

            // Responde afirmativamente con la operación
            ACLMessage agree = subscription.createReply();
            agree.setPerformative(ACLMessage.AGREE);
            return agree;
        }

        @Override
        protected ACLMessage handleCancel(ACLMessage cancel) throws FailureException {

            // Eliminamos la suscripción del agente jugador
            String nombreAgente = cancel.getSender().getName();
            suscripcionJugador = gestor.getSuscripcion(nombreAgente);
            mySubscriptionManager.deregister(suscripcionJugador);

            mensajesPendientes.add("Suscripción cancelada del agente: "
                    + cancel.getSender().getLocalName()
                    + "\nsuscripciones restantes: " + gestor.numSuscripciones());
            return null; // no hay que enviar mensaje de confirmación
        }
    }

    public class TareaNuevaPartida extends OneShotBehaviour {

        private String id;

        public TareaNuevaPartida(String _id) {
            this.id = _id;
        }

        @Override
        public void action() {
            ContenedorLaberinto contenedor = partidasIniciadas.get(id);
            
            //GENERO EL QUESO
            try {
                contenedor.getLaberintoGUI().nuevoQueso();
            } catch (IOException ex) {
                Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            Partida partida = contenedor.getPartida();
            Tablero tablero = new Tablero(contenedor.getAlto(), contenedor.getAncho());

            Posicion posicionInicio = new Posicion(contenedor.getLaberintoGUI().getQuesito().getX(),contenedor.getAncho()-1-contenedor.getLaberintoGUI().getQuesito().getY());
            int numCapturasQueso = contenedor.getQuesosMax();
            int numTrampasActivas = contenedor.getMaxTrampas();
            long maximoJuegoSeg = contenedor.getTiempo();
            EntornoLaberinto entInicio = contenedor.getLaberintoGUI().getEntorno(0, 0);
            Laberinto laberinto = new Laberinto(tablero, posicionInicio, entInicio, numCapturasQueso, numTrampasActivas, maximoJuegoSeg);
            ProponerPartida propPartida = new ProponerPartida(partida, laberinto);

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
            addBehaviour(new TareaProponerPartida(myAgent, msg, id));

            mensajesPendientes.add("Nueva Partida:\n"
                    + "    -ID de la partida: " + id + "\n"
                    + "    -Numero de capturas de queso: " + numCapturasQueso + "\n"
                    + "    -Numero maximo de trampas: " + numTrampasActivas + "\n"
                    + "    -Duracion maxima: " + maximoJuegoSeg);
        }
    }

    public class TareaProponerPartida extends ProposeInitiator {

        private String id;

        public TareaProponerPartida(Agent agente, ACLMessage msg, String _id) {
            super(agente, msg);
            this.id = _id;
        }

        @Override
        protected void handleAllResponses(Vector responses) {
            ContenedorLaberinto contenedor = partidasIniciadas.get(id);
            String rechazos = "Agentes que han rechazado:\n";
            int numRechazos = 0;
            ACLMessage msg;
            PartidaAceptada partida = null;
            Jugador jugador;
            Iterator it = responses.iterator();

            while (it.hasNext()) {
                msg = (ACLMessage) it.next();
                if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    try {
                        partida = (PartidaAceptada) manager.extractContent(msg);
                        jugador = partida.getJugador();
                        contenedor.insertarRaton(new ResultadoRaton(jugador.getAgenteJugador(), jugador.getNombre()));
                    } catch (Codec.CodecException | OntologyException ex) {
                        Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    ++numRechazos;
                    rechazos = rechazos + "     El agente: " + msg.getSender().getLocalName() + " ha rechazado el juego\n";
                }
            }
            mensajesPendientes.add("Han aceptado " + contenedor.getRatonesPartida().size() + " ratones.");
            mensajesPendientes.add("Han rechazado " + numRechazos + " ratones.");
            if (numRechazos > 0) {
                mensajesPendientes.add(rechazos);
            }

            //Genero los ratones
            try {
                contenedor.getLaberintoGUI().generarRatones(contenedor.getRatonesPartida());
            } catch (IOException ex) {
                Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
            }

            TareaInicioRonda tarea = new TareaInicioRonda(this.getAgent(), 400, id);//<--------------------------------------------200
            myAgent.addBehaviour(new acabarPartida(this.getAgent(), contenedor.getTiempo() * 1000, tarea, contenedor.getLaberintoGUI(),contenedor.getPartida()));
            myAgent.addBehaviour(tarea);
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

    class TareaInicioRonda extends TickerBehaviour {

        private ContenedorLaberinto contenedor;

        public TareaInicioRonda(Agent a, long period, String id) {
            super(a, period);
            this.contenedor = partidasIniciadas.get(id);
        }

        @Override
        public void onTick() {
            if (!contenedor.isObjetivoQuesos()) {
                ACLMessage msg = new ACLMessage(ACLMessage.CFP);
                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                msg.setSender(getAID());
                msg.setLanguage(codec.getName());
                msg.setOntology(ontology.getName());

                for (int i = 0; i < contenedor.getRatonesPartida().size(); i++) {
                    msg.addReceiver(contenedor.getRatonesPartida().get(i).getAidRaton());
                }
                msg.setReplyByDate(new Date(System.currentTimeMillis() + TIME_OUT));

                EntregarJugada pedirJugada = new EntregarJugada ( contenedor.getPartida());
                Action ac = new Action(this.myAgent.getAID(), pedirJugada);

                try {
                    manager.fillContent(msg, ac);
                } catch (Codec.CodecException | OntologyException ex) {
                    Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
                }

                String mensaj = "Se ha pedido una jugada a los agentes:\n";
                for (int i = 0; i < contenedor.getRatonesPartida().size(); i++) {
                    mensaj += "   " + contenedor.getRatonesPartida().get(i).getNombre() + "\n";
                }
                mensajesPendientes.add(mensaj);
                addBehaviour(new TareaJugarPartida(this.myAgent, msg, contenedor.getIdPartida()));
            } else {
                try {
                    contenedor.getLaberintoGUI().mostrarFIN(contenedor.getPartida());
                } catch (Codec.CodecException | OntologyException ex) {
                    Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
                }
                this.stop();
            }
        }
    }

    class TareaJugarPartida extends ContractNetInitiator {

        private ContenedorLaberinto contenedor;

        public TareaJugarPartida(Agent a, ACLMessage cfp, String id) {
            super(a, cfp);
            this.contenedor = partidasIniciadas.get(id);
        }

        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances) {
            String resultado = "Recibidos los siguientes movimiento:";
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

            List<ResultadoJugada> resultados = null;
            try {
                resultados = contenedor.getLaberintoGUI().hacerJugadas(jugadas, new Partida(contenedor.getIdPartida(), OntologiaLaberinto.TIPO_JUEGO));
            } catch (IOException | Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
            }
            mensajesPendientes.add("Se han generado " + resultados.size() + " resultados de las " + jugadas.size() + " jugadas.");
            ACLMessage msgg;
            for (int i = 0; i < jugadas.size(); i++) {
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
                    //System.out.println("No se han encontrado consolas:");
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
                    //System.out.println("Se han encontrado las siguientes agentes rata:");
                    agentesRaton = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        agentesRaton[i] = result[i].getName();
                        //System.out.println(agentesRaton[i].getName());
                    }
                } else {
                    myGUI.desactivarBoton();
                    //System.out.println("No se han encontrado ratas");
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
                    //System.out.println("Empieza el envío");
                    mensaje = new ACLMessage(ACLMessage.INFORM);
                    mensaje.setSender(myAgent.getAID());
                    mensaje.addReceiver(agentesConsola[0]);
                    mensaje.setContent(mensajesPendientes.remove(0));

                    // 
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

    public class acabarPartida extends TickerBehaviour {

        private TareaInicioRonda tarea;
        private GameUI labe;
        private Partida partida;

        public acabarPartida(Agent a, long period, TareaInicioRonda t, GameUI laberinto,Partida part) {
            super(a, period);
            this.tarea = t;
            this.labe = laberinto;
            this.partida=part;
        }

        @Override
        protected void onTick() {
            try {
                labe.mostrarFIN(partida);
            } catch (Codec.CodecException | OntologyException ex) {
                Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
            }
            tarea.stop();
            this.stop();
        }
    }

}
