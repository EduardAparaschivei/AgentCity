package main.agents;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import main.behaviours.LlmListenBehaviour; // Importam comportamentul nou

public class LlmAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("LlmAgent " + getAID().getLocalName() + " este online.");

        // 1. Inregistrare in DF (Yellow Pages)
        inregistreazaServiciu();

        // 2. Adaugare Comportament (Decuplat)
        addBehaviour(new LlmListenBehaviour(this));
    }

    private void inregistreazaServiciu() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("llm-service");
        sd.setName("Gemini-Assistant");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        System.out.println("LlmAgent " + getAID().getLocalName() + " se inchide.");
    }
}