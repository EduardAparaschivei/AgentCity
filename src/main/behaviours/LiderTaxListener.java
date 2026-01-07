package main.behaviours;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import main.agents.LiderAgent;

public class LiderTaxListener extends CyclicBehaviour {
    private LiderAgent lider;

    public LiderTaxListener(LiderAgent agent) {
        super(agent);
        this.lider = agent;
    }

    @Override
    public void action() {
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
        ACLMessage msg = lider.receive(mt);

        if (msg != null && msg.getContent().startsWith("TAXA_COLECTATA:")) {
            try {
                int suma = Integer.parseInt(msg.getContent().split(":")[1]);

                // Apelam metoda din agent care actualizeaza bugetul
                lider.adaugaLaBuget(suma);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (msg != null) {
            // Ignoram alte mesaje INFORM care nu sunt taxe
            // (pentru ca cele de update sunt prinse de celalalt behavior prin ontologie)
        } else {
            block();
        }
    }
}