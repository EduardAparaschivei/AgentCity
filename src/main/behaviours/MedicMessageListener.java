package main.behaviours;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import main.agents.MedicAgent;

public class MedicMessageListener extends CyclicBehaviour {
    private MedicAgent medic;

    public MedicMessageListener(MedicAgent agent) {
        super(agent);
        this.medic = agent;
    }

    @Override
    public void action() {
        // Ascultam doar cereri (REQUEST)
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
        ACLMessage msg = medic.receive(mt);

        if (msg != null) {
            String content = msg.getContent();

            // CAZ 1: Cerere de vindecare
            if (content.equals("VINDECA_MA")) {
                medic.trateazaPacient(msg);
            }
            // CAZ 2: Cerere de taxe
            else if (content.startsWith("PLATVESTE_TAXA")) {
                medic.platesteTaxa(msg);
            }
        } else {
            block();
        }
    }
}