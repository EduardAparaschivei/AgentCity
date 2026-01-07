package main.behaviours;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import main.agents.ColectorAgent;

public class ColectorMessageListener extends CyclicBehaviour {
    private ColectorAgent colector;

    public ColectorMessageListener(ColectorAgent agent) {
        super(agent);
        this.colector = agent;
    }

    @Override
    public void action() {
        ACLMessage msg = colector.receive();

        if (msg != null) {
            String content = msg.getContent();

            // Cazul 1: Mesaj de la Lider (REQUEST) - Ordin de colectare
            if (msg.getPerformative() == ACLMessage.REQUEST && content.startsWith("COLECTEAZA_AUTOMAT:")) {
                try {
                    // 1. Citim suma ceruta de Lider
                    int suma = Integer.parseInt(content.split(":")[1]);

                    // Actualizam starea agentului si declansam actiunea
                    colector.setSumaCurenta(suma);
                    colector.executaOrdinColectare(suma);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // Cazul 2: Mesaj de la Cetatean (INFORM) - Confirmare plata
            else if (msg.getPerformative() == ACLMessage.INFORM && content.equals("AM_PLATIT")) {
                // Agentul gestioneaza raportarea catre lider
                colector.raporteazaIncasare(msg.getSender().getLocalName());
            }
        } else {
            block();
        }
    }
}