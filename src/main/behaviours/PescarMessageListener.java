package main.behaviours;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import main.agents.PescarAgent;

public class PescarMessageListener extends CyclicBehaviour {
    private PescarAgent pescar;

    public PescarMessageListener(PescarAgent agent) {
        super(agent);
        this.pescar = agent;
    }

    @Override
    public void action() {
        // Daca e mort, blocam threadul
        if (pescar.isDead()) {
            block();
            return;
        }

        ACLMessage msg = pescar.receive();

        if (msg != null) {
            String content = msg.getContent();

            // A. SCHIMBARE TIMP
            if (msg.getPerformative() == ACLMessage.PROPAGATE) {
                pescar.gestioneazaTimpul(content);
            }
            // B. TAXE
            else if (msg.getPerformative() == ACLMessage.REQUEST && content.startsWith("PLATVESTE_TAXA")) {
                pescar.platesteTaxa(msg);
            }
            // C. MOARTE
            else if (content.equals("DIE")) {
                pescar.moarte();
            }
            // D. Alte requesturi
            else if (msg.getPerformative() == ACLMessage.REQUEST) {
                // Ignore sau logica custom
            }
        } else {
            block();
        }
    }
}