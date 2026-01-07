package main.behaviours;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import main.agents.NegustorAgent;

public class NegustorMessageListener extends CyclicBehaviour {
    private NegustorAgent negustor;

    public NegustorMessageListener(NegustorAgent agent) {
        super(agent);
        this.negustor = agent;
    }

    @Override
    public void action() {
        ACLMessage msg = negustor.receive();

        if (msg != null) {
            String content = msg.getContent();

            // CAZ 1: Vine Pescarul sa vanda (PROPOSE)
            if (msg.getPerformative() == ACLMessage.PROPOSE && content.startsWith("VAND_PESTE:")) {
                negustor.proceseazaVanzare(msg);
            }
            // CAZ 2: Vine Colectorul (REQUEST)
            else if (msg.getPerformative() == ACLMessage.REQUEST && content.startsWith("PLATVESTE_TAXA")) {
                negustor.proceseazaTaxa(msg);
            }
        } else {
            block();
        }
    }
}