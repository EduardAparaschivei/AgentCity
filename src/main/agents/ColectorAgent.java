package main.agents;

import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import main.behaviours.ColectorMessageListener;

public class ColectorAgent extends BaseAgent {
    private int sumaCurentaDeColectat = 10;

    @Override
    protected void setup() {
        System.out.println("Colectorul " + getAID().getLocalName() + " este gata sa ia banii oamenilor.");

        // 1. Inregistrare in DF
        inregistreazaServiciu();

        // 2. Adaugare Comportament (Decuplat)
        addBehaviour(new ColectorMessageListener(this));
    }

    private void inregistreazaServiciu() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("tax-collector");
        sd.setName("ANAF-Local");
        dfd.addServices(sd);
        try { DFService.register(this, dfd); } catch (FIPAException e) {}
    }

    // --- METODE PUBLICE (Apelate din Behaviour) ---

    public void setSumaCurenta(int suma) {
        this.sumaCurentaDeColectat = suma;
    }

    public void executaOrdinColectare(int suma) {
        System.out.println("Colector: Am primit ordin sa colectez taxa de " + suma);

        // Trimitem notificari catre toate categoriile
        trimiteSomatii("food-producer", suma);
        trimiteSomatii("doctor", suma);
        trimiteSomatii("merchant", suma);
    }

    public void raporteazaIncasare(String numePlatitor) {
        System.out.println("Colector: Am luat bani de la " + numePlatitor);

        ACLMessage raport = new ACLMessage(ACLMessage.INFORM);
        raport.addReceiver(new jade.core.AID("lider", jade.core.AID.ISLOCALNAME));

        // Folosim suma stocata in agent
        raport.setContent("TAXA_COLECTATA:" + sumaCurentaDeColectat);

        send(raport);
    }

    // --- HELPER INTERN ---

    private void trimiteSomatii(String serviceType, int suma) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);

            if (result.length > 0) {
                ACLMessage taxRequest = new ACLMessage(ACLMessage.REQUEST);
                taxRequest.setContent("PLATVESTE_TAXA:" + suma);

                for (DFAgentDescription agent : result) {
                    taxRequest.addReceiver(agent.getName());
                }

                send(taxRequest);
                // System.out.println("Colector: Somatii trimise catre " + serviceType);
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (Exception e) {}
    }
}