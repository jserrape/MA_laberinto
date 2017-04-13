/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

/**
 *
 * @author jcsp0003
 */
public class LaberintoOntologia extends BeanOntology {

    private static final long serialVersionUID = 1L;

    // NOMBRE
    public static final String ONTOLOGY_NAME = "Ontologia_Cuatro_en_Raya";

    //VOCABULARIO
    public static final String REGISTRO_LABERINTO = "Laberinto";
    public static final String REGISTRO_RATON = "Raton del Laberinto";
    public static final String TIPO_JUEGO = "Cuatro en Raya";

    // The singleton instance of this ontology
    private static Ontology INSTANCE;

    public synchronized final static Ontology getInstance() throws BeanOntologyException {
        if (INSTANCE == null) {
            INSTANCE = new LaberintoOntologia();
        }
        return INSTANCE;
    }

    /**
     * Constructor
     *
     * @throws BeanOntologyException
     */
    private LaberintoOntologia() throws BeanOntologyException {

        super(ONTOLOGY_NAME);

        add("laberinto.elementos");
    }
}
