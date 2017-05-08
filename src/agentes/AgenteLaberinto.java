/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import util.ContenedorLaberinto;
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
        ContenedorLaberinto cont = new ContenedorLaberinto(t, mq, mt, alt, anc, iid);
        partidasIniciadas.put(iid, cont);

        addBehaviour(new TareaNuevaPartida(iid));
    }

    public class TareaNuevaPartida extends OneShotBehaviour {

        private String id;

        public TareaNuevaPartida(String _id) {
            this.id = _id;
        }

        @Override
        public void action() {
            ContenedorLaberinto contenedor = partidasIniciadas.get(id);
            Partida partida = contenedor.getPartida();
            Tablero tablero = new Tablero(contenedor.getAlto(), contenedor.getAncho());

            Posicion posicionInicio = new Posicion(0, 0);
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

            //GENERO EL QUESO
            try {
                contenedor.getLaberintoGUI().nuevoQueso();
            } catch (IOException ex) {
                Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
            }

            //Genero los ratones
            try {
                contenedor.getLaberintoGUI().generarRatones(new Posicion(0,0), contenedor.getRatonesPartida());
            } catch (IOException ex) {
                Logger.getLogger(AgenteLaberinto.class.getName()).log(Level.SEVERE, null, ex);
            }

            //TareaInicioRonda tarea = new TareaInicioRonda(this.getAgent(), 200, partida.getPartida().getIdPartida());//<--------------------------------------------200
            //myAgent.addBehaviour(new acabarPartida(this.getAgent(), contenedor.getTiempo() * 1000, tarea));
            //myAgent.addBehaviour(tarea);
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

        private final String idPartida;
        private final Partida partida;

        public TareaInicioRonda(Agent a, long period, String idPartida) {
            super(a, period);
            this.idPartida = idPartida;
            this.partida = new Partida(idPartida, OntologiaLaberinto.TIPO_JUEGO);
        }

        @Override
        public void onTick() {

        }
    }

    class TareaJugarPartida extends ContractNetInitiator {

        public TareaJugarPartida(Agent a, ACLMessage cfp) {
            super(a, cfp);
        }

        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances) {

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

        public acabarPartida(Agent a, long period, TareaInicioRonda t) {
            super(a, period);
            this.tarea = t;
        }

        @Override
        protected void onTick() {

        }
    }

}
