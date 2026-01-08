package main.agents;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import main.behaviours.MedicMessageListener;
import main.behaviours.MedicStatusTicker;
import org.json.JSONObject;

public class MedicAgent extends BaseAgent {
    private int bani = 250;
    private final int PRET_TRATAMENT = 50; // Cat castiga medicul

    @Override
    protected void setup() {
        // 1. Inregistrare DF
        inregistreazaServiciu();

        System.out.println("Medic: Cabinet deschis.");

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                trimiteStatus();
            }
        });

        // 2. Adaugare Comportamente (Decuplate)
        addBehaviour(new MedicStatusTicker(this, 2000));
        addBehaviour(new MedicMessageListener(this));
    }

    private void inregistreazaServiciu() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("doctor");
        sd.setName("Serviciu-Medical");
        dfd.addServices(sd);
        try { DFService.register(this, dfd); } catch (Exception e) {}
    }

    // --- METODE PUBLICE (Apelate din Behaviours) ---

    public void trateazaPacient(ACLMessage msg) {
        // Incaseaza banii
        bani += PRET_TRATAMENT;

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.AGREE);
        reply.setContent("TRATAMENT_COMPLET");
        send(reply);

        System.out.println("Medic: Pacient tratat (" + msg.getSender().getLocalName() + "). Incasat " + PRET_TRATAMENT);
        trimiteStatus(); // Update UI
    }

    public void platesteTaxa(ACLMessage msg) {
        try {
            int suma = 10;
            if (msg.getContent().contains(":")) {
                suma = Integer.parseInt(msg.getContent().split(":")[1]);
            }

            if (bani >= suma) {
                bani -= suma;
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("AM_PLATIT");
                send(reply);

                System.out.println("Medic: Platit taxa " + suma);
                trimiteStatus();
            } else {
                System.out.println("Medic: Nu am bani de taxe!");
                // Optional: Trimite REFUSE
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void trimiteStatus() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("lider", AID.ISLOCALNAME));
        msg.setOntology("update-status");

        JSONObject json = new JSONObject();
        json.put("agent", getLocalName());
        json.put("tip", "Medic");
        json.put("locatie", "Cabinet");
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