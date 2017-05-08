/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.proto.SubscriptionResponder;
import jade.proto.SubscriptionResponder.Subscription;
import jade.proto.SubscriptionResponder.SubscriptionManager;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author pedroj
 */
public class GestorSuscripciones implements SubscriptionManager {

    private final Map<String, Subscription> suscripciones;

    public GestorSuscripciones() {
        suscripciones = new HashMap();
    }

    @Override
    public boolean register(SubscriptionResponder.Subscription s) throws RefuseException, NotUnderstoodException {
        // Guardamos la suscripción asociada al agente que la solita
        String nombreAgente = s.getMessage().getSender().getName();
        suscripciones.put(nombreAgente, s);
        return true;
    }

    @Override
    public boolean deregister(SubscriptionResponder.Subscription s) throws FailureException {
        // Eliminamos la suscripción asociada a un agente
        String nombreAgente = s.getMessage().getSender().getName();
        suscripciones.remove(nombreAgente);
        s.close(); // queda cerrada la suscripción
        return true;
    }

    public Subscription getSuscripcion(String nombreAgente) {
        return suscripciones.get(nombreAgente);
    }

    public boolean haySuscripcion(String nombreAgente) {
        return suscripciones.get(nombreAgente) != null;
    }

    public int numSuscripciones() {
        return suscripciones.size();
    }
}
