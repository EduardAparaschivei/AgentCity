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
        if (pescar.isDead()) {
            block();
            return;
        }

        ACLMessage msg = pescar.receive();

        if (msg != null) {
            String content = msg.getContent();

            // --- DEBUG ---
            // Adauga linia asta sa vedem TOT ce primeste pescarul
            // System.out.println(pescar.getLocalName() + ": Am primit mesaj [" + msg.getPerformative() + "] -> " + content);
            // -------------

            // A. SCHIMBARE TIMP
            if (msg.getPerformative() == ACLMessage.PROPAGATE) {
                System.out.println("⏰ " + pescar.getLocalName() + ": Am primit ordin de timp: " + content);
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
        } else {
            block();
        }
    }
}