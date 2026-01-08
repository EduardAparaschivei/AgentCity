package main.agents;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour; // <--- Import nou necesar
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import main.behaviours.PescarLifecycleBehaviour;
import main.behaviours.PescarMessageListener;
import org.json.JSONObject;

public class PescarAgent extends BaseAgent {

    // Stare Interna
    private int pestiPrinsi = 0;
    private int bani = 50;
    private String stareSpirit = "Neutru";
    private String locatie = "Acasa";
    private int energie = 100;

    private boolean isDead = false;
    private final int PRET_MEDIC = 50;

    private AID agentMedicCache = null;    // Cache pentru Doctor
    private AID agentNegustorCache = null; // Cache pentru Negustor

    @Override
    protected void setup() {
        System.out.println("Pescarul " + getAID().getLocalName() + " se trezeste.");

        // 1. INREGISTRARE DF
        inregistreazaServiciu();

        // 2. [FIX UI] Trimite status imediat ce apare (sa apara pe harta instant)
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                trimiteStatusLiderului();
            }
        });

        // 3. ADAUGARE COMPORTAMENTE
        addBehaviour(new PescarLifecycleBehaviour(this, 1000));
        addBehaviour(new PescarMessageListener(this));
    }

    private void inregistreazaServiciu() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("food-producer"); // Sau "pescar", important e sa te stie Liderul
        sd.setName("JADE-Fishing");
        dfd.addServices(sd);
        try { DFService.register(this, dfd); } catch (FIPAException e) {}
    }

    // --- GETTERS ---
    public String getLocatie() { return locatie; }
    public boolean isDead() { return isDead; }

    // --- METODE PUBLICE ---

    public void pescuieste() {
        if (energie > 10) {
            pestiPrinsi++;
            energie -= 5;
            stareSpirit = "Concentrat";
            // Optional: Putem trimite status si aici, ca sa vedem pestii crescand live
            trimiteStatusLiderului();
        } else {
            stareSpirit = "Epuizat";
            trimiteStatusLiderului();
        }
    }

    public void odihneste() {
        if (energie < 20) {
            boolean tratamentReusit = false;
            if (bani >= PRET_MEDIC) {
                tratamentReusit = mergiLaMedic();
            } else {
                stareSpirit = "Muribund (Sărac)";
            }

            if (!tratamentReusit) {
                energie -= 5;
                if (stareSpirit.equals("Bolnav")) stareSpirit = "Bolnav (Netratat)";
                System.out.println(getLocalName() + ": Sunt bolnav... Energie scade la " + energie);
            }

            if (energie <= 0) moarte();
        }
        else if (energie < 100) {
            energie += 5;
            locatie = "Acasa";
            if (bani > 100) stareSpirit = "Bucuros";
            else if (bani < 20) stareSpirit = "Nervos";
            else stareSpirit = "Neutru";

            // Doar notificam ca ne odihnim
            trimiteStatusLiderului();
        }
    }

    public void trimiteStatusLiderului() {
        if (isDead) return;

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        // Ne asiguram ca numele liderului e corect (daca liderul are alt nume, modifica aici)
        msg.addReceiver(new AID("lider", AID.ISLOCALNAME));
        msg.setOntology("update-status");

        JSONObject json = new JSONObject();
        json.put("agent", getLocalName());

        // E important sa trimitem tipul, ca GUI sa stie ce iconita sa puna
        json.put("tip", "Pescar");

        json.put("locatie", locatie);
        json.put("pesti", pestiPrinsi);
        json.put("bani", bani);
        json.put("stare", stareSpirit);
        json.put("energie", energie);

        msg.setContent(json.toString());
        send(msg);
    }

    public void gestioneazaTimpul(String comanda) {
        boolean statusChanged = false;

        if (comanda.equals("START_WORK") || comanda.equals("CONTINUE_WORK")) {
            if (energie > 10 && !stareSpirit.equals("Muribund")) {
                if (!locatie.equals("Lac")) {
                    locatie = "Lac";
                    statusChanged = true; // S-a schimbat locatia
                }
            }
        }
        else if (comanda.equals("END_WORK")) {
            if (!locatie.equals("Acasa")) {
                locatie = "Acasa";
                vindePeste();
                statusChanged = true;
            }
        }
        else if (comanda.equals("PARTY_TIME")) {
            stareSpirit = "Extaz";
            energie = 100;
            statusChanged = true;
        }

        // [FIX UI] Daca s-a schimbat locatia sau starea, anunta Liderul ACUM!
        if (statusChanged) {
            trimiteStatusLiderului();
        }
    }

    public void platesteTaxa(ACLMessage msg) {
        int suma = 10;
        if (msg.getContent().contains(":")) {
            try { suma = Integer.parseInt(msg.getContent().split(":")[1]); } catch (Exception e) {}
        }

        if (bani >= suma) {
            bani -= suma;
            stareSpirit = "Nervos";

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent("AM_PLATIT");
            send(reply);

            trimiteStatusLiderului(); // Update ca au scazut banii
        } else {
            System.out.println(getLocalName() + ": Nu am bani de taxe (" + suma + ")!");
            // E bine sa raspundem cu REFUSE ca sa nu astepte Colectorul degeaba
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.REFUSE);
            reply.setContent("NU_AM_BANI");
            send(reply);
        }
    }

    public void moarte() {
        if (isDead) return;
        isDead = true;
        System.out.println("❌ " + getLocalName() + " a murit.");

        ACLMessage deadMsg = new ACLMessage(ACLMessage.INFORM);
        deadMsg.addReceiver(new AID("lider", AID.ISLOCALNAME));
        deadMsg.setOntology("update-status");
        deadMsg.setContent("AGENT_DEAD");
        send(deadMsg);

        try { DFService.deregister(this); } catch (Exception e) {}
        doDelete();
    }

    // --- METODE INTERNE ---

    private boolean mergiLaMedic() {
        // Cautam "doctor" (cum e definit in MedicAgent)
        AID medic = getServiceAgent("doctor");

        if (medic != null) {
            locatie = "Cabinet"; // Sau "Doctor" ca sa apara in cladire
            trimiteStatusLiderului(); // Update UI ca suntem la medic

            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.addReceiver(medic);
            req.setContent("VINDECA_MA");
            send(req);

            ACLMessage reply = blockingReceive(MessageTemplate.MatchSender(medic), 2000);
            if (reply != null && reply.getPerformative() == ACLMessage.AGREE) {
                bani -= PRET_MEDIC;
                energie = 100;
                stareSpirit = "Vindecat";
                trimiteStatusLiderului(); // Update dupa vindecare
                return true;
            }
        }
        return false;
    }

    private void vindePeste() {
        if (pestiPrinsi > 0) {
            // [FIX] Cautam "negustor", nu "merchant" (conform NegustorAgent.java)
            AID negustor = getServiceAgent("negustor");

            if (negustor != null) {
                ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
                msg.addReceiver(negustor);
                msg.setContent("VAND_PESTE:" + pestiPrinsi);
                send(msg);

                ACLMessage reply = blockingReceive(MessageTemplate.MatchSender(negustor), 2000);
                if (reply != null && reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    try {
                        int castig = Integer.parseInt(reply.getContent());
                        bani += castig;
                        pestiPrinsi = 0;
                        trimiteStatusLiderului(); // Update ca avem bani
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("Pescar: Nu gasesc negustorul sa vand pestele!");
            }
        }
    }

    private AID getServiceAgent(String serviceType) {
        // 1. Verificam Cache-ul specific
        if (serviceType.equals("doctor") && agentMedicCache != null) {
            return agentMedicCache;
        }
        if (serviceType.equals("negustor") && agentNegustorCache != null) {
            return agentNegustorCache;
        }

        // 2. Daca nu stim cine e, cautam in DF (doar acum!)
        // System.out.println(getLocalName() + ": Nu stiu cine e " + serviceType + ". Caut in DF...");
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                AID foundAgent = result[0].getName();

                // 3. Salvam in Cache pentru data viitoare
                if (serviceType.equals("doctor")) agentMedicCache = foundAgent;
                if (serviceType.equals("negustor")) agentNegustorCache = foundAgent;

                return foundAgent;
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        return null;
    }
}