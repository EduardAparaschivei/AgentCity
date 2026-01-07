package main.agents;

import jade.core.AID;
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

    @Override
    protected void setup() {
        System.out.println("Pescarul " + getAID().getLocalName() + " se trezeste.");

        // 1. INREGISTRARE DF
        inregistreazaServiciu();

        // 2. ADAUGARE COMPORTAMENTE (Curat!)
        // Importate din main.behaviours
        addBehaviour(new PescarLifecycleBehaviour(this, 1000));
        addBehaviour(new PescarMessageListener(this));
    }

    private void inregistreazaServiciu() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("food-producer");
        sd.setName("JADE-Fishing");
        dfd.addServices(sd);
        try { DFService.register(this, dfd); } catch (FIPAException e) {}
    }

    // --- GETTERS (Necesare pentru behaviours) ---
    public String getLocatie() { return locatie; }
    public boolean isDead() { return isDead; }

    // --- METODE PUBLICE (Apelate din Behaviours) ---

    public void pescuieste() {
        if (energie > 10) {
            pestiPrinsi++;
            energie -= 5;
            stareSpirit = "Concentrat";
        } else {
            stareSpirit = "Epuizat";
        }
    }

    public void odihneste() {
        // Logica complexa de odihna/medic ramane aici, incapsulata
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
            energie += 5; // Regenerare un pic mai rapida pentru gameplay
            locatie = "Acasa";
            if (bani > 100) stareSpirit = "Bucuros";
            else if (bani < 20) stareSpirit = "Nervos";
            else stareSpirit = "Neutru";
        }
    }

    public void trimiteStatusLiderului() {
        if (isDead) return;

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("lider", AID.ISLOCALNAME));
        msg.setOntology("update-status");

        JSONObject json = new JSONObject();
        json.put("agent", getLocalName());
        json.put("locatie", locatie);
        json.put("pesti", pestiPrinsi);
        json.put("bani", bani);
        json.put("stare", stareSpirit);
        json.put("energie", energie);

        msg.setContent(json.toString());
        send(msg);
    }

    public void gestioneazaTimpul(String comanda) {
        if (comanda.equals("START_WORK") || comanda.equals("CONTINUE_WORK")) {
            if (energie > 10 && !stareSpirit.equals("Muribund")) {
                locatie = "Lac";
            }
        }
        else if (comanda.equals("END_WORK")) {
            locatie = "Acasa";
            vindePeste();
        }
        else if (comanda.equals("PARTY_TIME")) {
            stareSpirit = "Extaz";
            energie = 100;
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
            trimiteStatusLiderului();
        } else {
            System.out.println(getLocalName() + ": Nu am bani de taxe (" + suma + ")!");
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

    // --- METODE INTERNE (Helper) ---

    private boolean mergiLaMedic() {
        AID medic = gasesteAgentDupaServiciu("doctor");
        if (medic != null) {
            locatie = "Cabinet";
            trimiteStatusLiderului();

            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.addReceiver(medic);
            req.setContent("VINDECA_MA");
            send(req);

            ACLMessage reply = blockingReceive(MessageTemplate.MatchSender(medic), 2000);
            if (reply != null && reply.getPerformative() == ACLMessage.AGREE) {
                bani -= PRET_MEDIC;
                energie = 100;
                stareSpirit = "Vindecat";
                return true;
            }
        }
        return false;
    }

    private void vindePeste() {
        if (pestiPrinsi > 0) {
            AID negustor = gasesteAgentDupaServiciu("merchant");
            if (negustor != null) {
                ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
                msg.addReceiver(negustor);
                msg.setContent("VAND_PESTE:" + pestiPrinsi);
                send(msg);

                ACLMessage reply = blockingReceive(MessageTemplate.MatchSender(negustor), 2000);
                if (reply != null && reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    int castig = Integer.parseInt(reply.getContent());
                    bani += castig;
                    pestiPrinsi = 0;
                }
            }
        }
    }
}