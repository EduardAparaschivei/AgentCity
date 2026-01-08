package main.agents;

import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;
import main.behaviours.ColectorMessageListener;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ColectorAgent extends BaseAgent {
    private int sumaCurentaDeColectat = 10;

    // --- CACHE CONTRIBUABILI ---
    // Set<AID> ne protejeaza automat de duplicate
    private Set<AID> listaPescari = new HashSet<>();
    private Set<AID> listaMedici = new HashSet<>();
    private Set<AID> listaNegustori = new HashSet<>();

    @Override
    protected void setup() {
        System.out.println("Colectorul " + getAID().getLocalName() + " a pornit.");

        // 1. Inregistrare in DF (Eu sunt Colectorul)
        inregistreazaServiciu();

        // 2. [FIX] Cautare initiala - Cine e deja online?
        populeazaListeleInitiale();

        // 3. Abonare la DF - Cine apare de acum incolo?
        pornesteMonitorizareAgenti();

        // 4. Ascultare mesaje
        addBehaviour(new ColectorMessageListener(this));
    }

    private void inregistreazaServiciu() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("tax-collector");
        sd.setName("Serviciu-Colectare-Taxe");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println("Colector: Inregistrat in DF.");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    // --- METODA NOUA: CAUTARE INITIALA (Manual Search) ---
    // Aceasta metoda repara problema cand agentii exista deja inainte de Subscribe
    private void populeazaListeleInitiale() {
        System.out.println("Colector: Fac recensamantul initial...");

        // Cautam tot ce misca (Template gol) sau putem cauta specific
        DFAgentDescription template = new DFAgentDescription();

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                for (DFAgentDescription agentDesc : result) {
                    proceseazaAgent(agentDesc.getName(), agentDesc.getAllServices());
                }
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    // --- MONITORIZARE (Subscribe) ---
    private void pornesteMonitorizareAgenti() {
        DFAgentDescription template = new DFAgentDescription();

        try {
            ACLMessage subscribeMsg = DFService.createSubscriptionMessage(this, getDefaultDF(), template, null);

            addBehaviour(new SubscriptionInitiator(this, subscribeMsg) {
                @Override
                protected void handleInform(ACLMessage inform) {
                    try {
                        DFAgentDescription[] results = DFService.decodeNotification(inform.getContent());
                        if (results.length > 0) {
                            for (DFAgentDescription dfd : results) {
                                proceseazaAgent(dfd.getName(), dfd.getAllServices());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            System.out.println("Colector: Monitorizare live activata.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- LOGICA COMUNA DE FILTRARE (Folosita si la Search, si la Subscribe) ---
    private void proceseazaAgent(AID agentIdentificat, Iterator servicii) {
        // Ignoram daca e chiar Colectorul
        if (agentIdentificat.equals(getAID())) return;

        while (servicii.hasNext()) {
            ServiceDescription sd = (ServiceDescription) servicii.next();
            String tip = sd.getType();

            // DEBUG: Vedem tot ce gasim
            // System.out.println("[DEBUG] Verific agent: " + agentIdentificat.getLocalName() + " -> " + tip);

            if (tip.equalsIgnoreCase("food-producer") || tip.equalsIgnoreCase("pescar")) {
                if (listaPescari.add(agentIdentificat)) {
                    System.out.println("   [+] Adaugat la PESCARI: " + agentIdentificat.getLocalName());
                }
            }
            else if (tip.equalsIgnoreCase("doctor") || tip.equalsIgnoreCase("medic")) {
                if (listaMedici.add(agentIdentificat)) {
                    System.out.println("   [+] Adaugat la MEDICI: " + agentIdentificat.getLocalName());
                }
            }
            else if (tip.equalsIgnoreCase("negustor") || tip.equalsIgnoreCase("merchant")) {
                if (listaNegustori.add(agentIdentificat)) {
                    System.out.println("   [+] Adaugat la NEGUSTORI: " + agentIdentificat.getLocalName());
                }
            }
        }
    }

    // --- METODE PUBLICE ---

    public void setSumaCurenta(int suma) {
        this.sumaCurentaDeColectat = suma;
    }

    public void executaOrdinColectare(int suma) {
        System.out.println("--- START COLECTARE (Taxa: " + suma + ") ---");
        this.sumaCurentaDeColectat = suma;

        if (!listaPescari.isEmpty()) trimiteLaGrup(listaPescari, suma, "Pescari");
        else System.out.println("! Nu am pescari in lista.");

        if (!listaMedici.isEmpty()) trimiteLaGrup(listaMedici, suma, "Medici");
        else System.out.println("! Nu am medici in lista.");

        if (!listaNegustori.isEmpty()) trimiteLaGrup(listaNegustori, suma, "Negustori");
        else System.out.println("! Nu am negustori in lista.");
    }

    private void trimiteLaGrup(Set<AID> grup, int suma, String numeGrup) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setContent("PLATVESTE_TAXA:" + suma);
        for (AID agent : grup) {
            msg.addReceiver(agent);
        }
        send(msg);
        System.out.println("-> Somatie trimisa catre " + grup.size() + " agenti (" + numeGrup + ")");
    }

    public void raporteazaIncasare(String numePlatitor) {
        System.out.println("$$$ INCASAT de la " + numePlatitor);
        ACLMessage raport = new ACLMessage(ACLMessage.INFORM);
        raport.addReceiver(new AID("lider", AID.ISLOCALNAME));
        raport.setContent("TAXA_COLECTATA:" + sumaCurentaDeColectat);
        send(raport);
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (Exception e) {}
    }
}