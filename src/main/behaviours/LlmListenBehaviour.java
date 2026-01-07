package main.behaviours;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import main.utils.LlmConnector;

public class LlmListenBehaviour extends CyclicBehaviour {

    public LlmListenBehaviour(Agent a) {
        super(a);
    }

    @Override
    public void action() {
        // Ascultam doar mesaje REQUEST
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
        ACLMessage msg = myAgent.receive(mt);

        if (msg != null) {
            proceseazaCerere(msg);
        } else {
            block();
        }
    }

    private void proceseazaCerere(ACLMessage msg) {
        String content = msg.getContent();
        // Formatul asteptat: "Rol|||Context|||Instructiune"
        String[] parts = content.split("\\|\\|\\|");

        String raspunsText = "";

        if (parts.length == 3) {
            String rol = parts[0];
            String context = parts[1];
            String instructiune = parts[2];

            System.out.println("LlmAgent: Procesez cerere pentru " + msg.getSender().getLocalName());

            // Apelam serverul Python prin clasa utilitara
            raspunsText = LlmConnector.getResponse(rol, context, instructiune);
        } else {
            raspunsText = "Eroare: Format mesaj incorect. Foloseste Rol|||Context|||Instructiune";
            System.err.println("LlmAgent: Format gresit primit de la " + msg.getSender().getLocalName());
        }

        // Trimitem raspunsul inapoi (INFORM)
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(raspunsText);
        myAgent.send(reply);
    }
}