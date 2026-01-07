package main.behaviours;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import main.agents.LiderAgent;

public class LiderUpdateListener extends CyclicBehaviour {
    private LiderAgent lider;

    public LiderUpdateListener(LiderAgent agent) {
        super(agent);
        this.lider = agent;
    }

    @Override
    public void action() {
        MessageTemplate mt = MessageTemplate.MatchOntology("update-status");
        ACLMessage msg = lider.receive(mt);

        if (msg != null) {
            if (msg.getContent().equals("AGENT_DEAD")) {
                String numeMort = msg.getSender().getLocalName();
                if (lider.getGui() != null) {
                    lider.getGui().removeAgent(numeMort);
                    System.out.println("✝️ " + numeMort + " a decedat.");
                }
            } else {
                if (lider.getGui() != null) {
                    lider.getGui().updateFishermanData(msg.getContent());
                }
            }
        } else {
            block();
        }
    }
}