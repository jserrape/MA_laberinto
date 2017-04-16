/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import dilemaPrisionero.OntologiaDilemaPrisionero;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SubscriptionInitiator;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jcsp0003
 *
 */
public class AgenteRaton extends Agent {

    //Variables del agente
    private AID[] agentesConsola;
    private ArrayList<String> mensajesPendientes;

    private ContentManager manager = (ContentManager) getContentManager();

    // El lenguaje utilizado por el agente para la comunicación es SL 
    private Codec codec = new SLCodec();

    // La ontología que utilizará el agente
    private Ontology ontology;

    @Override
    protected void setup() {
        //Inicialización de las variables del agente
        mensajesPendientes = new ArrayList();

        //Obtenemos la instancia de la ontología y registramos el lenguaje
        //y la ontología para poder completar el contenido de los mensajes
        try {
            ontology = OntologiaDilemaPrisionero.getInstance();
        } catch (BeanOntologyException ex) {
            Logger.getLogger(AgenteRaton.class.getName()).log(Level.SEVERE, null, ex);
        }

        manager.registerLanguage(codec);
        manager.registerOntology(ontology);

        System.out.println("El agente " + getName() + " esperando...");

        //Registro del agente en las páginas amarillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setName(getLocalName());
        sd.setType(LaberintoOntologia.REGISTRO_RATON);
        // Agents that want to use this service need to "know" the weather-forecast-ontology
        sd.addOntologies(OntologiaDilemaPrisionero.ONTOLOGY_NAME);
        // Agents that want to use this service need to "speak" the FIPA-SL language
        sd.addLanguages(FIPANames.ContentLanguage.FIPA_SL);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException ex) {
            Logger.getLogger(AgenteRaton.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Mensaje para el protocolo Subscribe
        ACLMessage mensaje = new ACLMessage(ACLMessage.SUBSCRIBE);
        mensaje.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
        mensaje.setContent("Apuntame a las partidas de busqueda de quesos.");
        
        //Se añade el destinatario del mensaje
        AID id = new AID();
        id.setLocalName(LaberintoOntologia.REGISTRO_LABERINTO);
        mensaje.addReceiver(id);

        addBehaviour(new InformarPartida(this, mensaje));
        addBehaviour(new AgenteRaton.TareaBuscarConsola(this, 5000));
        addBehaviour(new AgenteRaton.TareaEnvioConsola());
    }

    @Override
    protected void takeDown() {
        //Desregristo del agente de las Páginas Amarillas

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
    
    private class InformarPartida extends SubscriptionInitiator {

        public InformarPartida(Agent agente, ACLMessage mensaje) {
            super(agente, mensaje);
        }

        @Override
        protected void handleAgree(ACLMessage inform) {
            System.out.println("Solicitud aceptada");
        }

        @Override
        protected void handleRefuse(ACLMessage inform) {
            System.out.println("Solicitud rechazada");
        }

    }
}
