/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;

/**
 *
 * @author pedroj Esqueleto de agente para la estructura general que deben tener
 * todos los agentes
 */
public class AgenteRaton extends Agent {

    //Variables del agente
    private AID[] agentesConsola;
    private ArrayList<String> mensajesPendientes;

    @Override
    protected void setup() {
        //Inicialización de las variables del agente
        mensajesPendientes = new ArrayList();

        //Configuración del GUI
        //Registro del agente en las Páginas Amarrillas
        //Registro de la Ontología
        System.out.println("Se inicia la ejecución del agente: " + this.getName());
        //Añadir las tareas principales
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
}
