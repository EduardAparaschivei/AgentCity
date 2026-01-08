package main.agents;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import main.behaviours.NegustorMessageListener;
import main.behaviours.NegustorStatusTicker;
import org.json.JSONObject;

public class NegustorAgent extends BaseAgent {
    private int bani = 500; // Capital initial

    @Override
    protected void setup() {
        // 1. Inregistrare DF
        inregistreazaServiciu();

        System.out.println("Negustor: Deschis pentru afaceri!");

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                trimiteStatus();
            }
        });

        // 2. Adaugare Comportamente (Decuplate)
        addBehaviour(new NegustorStatusTicker(this, 2000));
        addBehaviour(new NegustorMessageListener(this));
    }

    private void inregistreazaServiciu() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("negustor");
        sd.setName("Serviciu-Comercial");
        dfd.addServices(sd);
        try { DFService.register(this, dfd); } catch (Exception e) {}
    }

    // --- METODE PUBLICE (Apelate din Behaviours) ---

    public void proceseazaVanzare(ACLMessage msg) {
        try {
            int cantitate = Integer.parseInt(msg.getContent().split(":")[1]);
            int pretPerBucata = 5;
            int pretPerBucataProfit = 7;
            int totalPlata = cantitate * pretPerBucata;

            if (bani >= totalPlata) {
                bani -= totalPlata; // Platim pescarul
                bani += pretPerBucataProfit * cantitate; // Simulam revanzarea cu profit

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                reply.setContent(String.valueOf(totalPlata));
                send(reply);
                System.out.println("Negustor: Cumparat " + cantitate + " pesti. Profit marcat.");

                trimiteStatus(); // Update rapid UI
            } else {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                send(reply);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void proceseazaTaxa(ACLMessage msg) {
        try {
            int suma = 10;
            if (msg.getContent().contains(":")) {
                suma = Integer.parseInt(msg.getContent().split(":")[1]);
            }

            if (bani >= suma) {
                bani -= suma;
                System.out.println("Negustor: Platit taxa " + suma);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("AM_PLATIT");
                send(reply);

                trimiteStatus();
            } else {
                System.out.println("Negustor: Faliment! Nu am bani de taxe.");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void trimiteStatus() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("lider", AID.ISLOCALNAME));
        msg.setOntology("update-status");

        JSONObject json = new JSONObject();
        json.put("agent", getLocalName());
        json.put("tip", "Negustor");
        json.put("locatie", "Piata");
        json.put("bani", bani);
        json.put("stare", "Activ");
        json.put("pesti", 0);
        json.put("energie", 100);

        msg.setContent(json.toString());
        send(msg);
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (Exception e) {}
    }
}