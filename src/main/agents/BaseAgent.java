package main.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class BaseAgent extends Agent {
    // Metoda ajutatoare pentru a cauta in DF
    protected AID gasesteAgentDupaServiciu(String tipServiciu) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(tipServiciu);
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                return result[0].getName(); // Returnam primul gasit
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        return null;
    }
}
