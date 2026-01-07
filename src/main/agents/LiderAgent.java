package main.agents;

import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import main.behaviours.LiderTaxListener;
import main.behaviours.LiderTimeTicker;
import main.behaviours.LiderUpdateListener;
import main.gui.LiderGui;

import javax.swing.*;
import java.io.*;
import java.util.Properties;

public class LiderAgent extends BaseAgent {
    private LiderGui myGui;

    // --- STATE VARIABLES ---
    private int buget = 0;
    private int contorPescari = 1;

    // Configurare Timp
    private boolean regimMuncaDublu = false;
    private int fazaZilei = 0;
    private int valoareTaxa = 10;

    @Override
    protected void setup() {
        // 1. Meniul de Start
        int choice = meniuStartJoc();

        // 2. CREARE AGENTI AUXILIARI (Medic, Negustor, Colector)
        // Liderul îi pornește programatic acum!
        creeazaAgentiAuxiliari();

        // 3. LOGICA JOC NOU vs CONTINUARE
        if (choice == 1) {
            incarcaSalvare();
            restaureazaPescarii();
        } else {
            buget = 100;
            contorPescari = 1;
            creeazaAgent("pescar1", "main.agents.PescarAgent");
            System.out.println("--- JOC NOU INITIAT ---");
        }

        // 4. GUI INIT
        myGui = new LiderGui(this);
        myGui.updateLiderStats(buget);

        // 5. ATAȘARE COMPORTAMENTE EXTERNE (Decuplare)
        addBehaviour(new LiderTimeTicker(this, 2500));
        addBehaviour(new LiderUpdateListener(this));
        addBehaviour(new LiderTaxListener(this));
    }

    // --- LOGICA DE CREARE AGENTI (Factory) ---

    private void creeazaAgentiAuxiliari() {
        System.out.println("Lider: Pornesc serviciile publice...");
        creeazaAgent("medic", "main.agents.MedicAgent");
        creeazaAgent("negustor", "main.agents.NegustorAgent");
        creeazaAgent("colector", "main.agents.ColectorAgent");
    }

    private void restaureazaPescarii() {
        System.out.println("Restaurare sesiune: Aveam " + contorPescari + " pescari.");
        for (int i = 1; i <= contorPescari; i++) {
            creeazaAgent("pescar" + i, "main.agents.PescarAgent");
        }
    }

    // Metoda generica pentru a porni orice agent
    private void creeazaAgent(String nickname, String className) {
        try {
            ContainerController container = getContainerController();
            AgentController ac = container.createNewAgent(nickname, className, null);
            ac.start();
            System.out.println(" -> Creat agent: " + nickname);
        } catch (Exception e) {
            System.err.println("Eroare creare " + nickname + ": " + e.getMessage());
        }
    }

    public void angajeazaPescar() {
        if (buget >= 100) {
            buget -= 100;
            if(myGui != null) myGui.updateLiderStats(buget);

            contorPescari++;
            String numeNou = "pescar" + contorPescari;

            creeazaAgent(numeNou, "main.agents.PescarAgent");

            // Sincronizare imediata (Welcome Packet)
            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    try { Thread.sleep(500); } catch (Exception e) {}
                    String comanda = (fazaZilei == 0 || (fazaZilei == 1 && regimMuncaDublu)) ? "START_WORK" : "END_WORK";
                    ACLMessage msg = new ACLMessage(ACLMessage.PROPAGATE);
                    msg.addReceiver(new jade.core.AID(numeNou, jade.core.AID.ISLOCALNAME));
                    msg.setContent(comanda);
                    send(msg);
                }
            });
        }
    }

    // --- METODE PUBLICE (Apelate din Behaviours) ---

    public void avanseazaTimpul() {
        fazaZilei++;
        if (fazaZilei > 3) fazaZilei = 0;

        String comanda = "";
        String textTimp = "";
        boolean isDay = false;

        switch (fazaZilei) {
            case 0:
                comanda = "START_WORK"; textTimp = "DIMINEAȚA (Muncă)"; isDay = true; break;
            case 1:
                if (regimMuncaDublu) { comanda = "CONTINUE_WORK"; textTimp = "DUPĂ-AMIAZĂ (Muncă Extra!)"; }
                else { comanda = "END_WORK"; textTimp = "DUPĂ-AMIAZĂ (Liber)"; }
                isDay = true; break;
            case 2:
                comanda = "END_WORK"; textTimp = "SEARA (Colectare Taxe)"; isDay = false;
                addBehaviour(new OneShotBehaviour() { @Override public void action() { startColectareAutomata(); }});
                break;
            case 3:
                comanda = "SLEEP"; textTimp = "NOAPTEA (Somn)"; isDay = false; break;
        }

        if (myGui != null) myGui.setTimeText(textTimp, isDay);
        if (!comanda.equals("SLEEP") && !comanda.equals("CONTINUE_WORK")) {
            trimiteSemnalTuturor(comanda);
        }
    }

    public void gestioneazaDeces(String numeMort) {
        if (myGui != null) {
            myGui.removeAgent(numeMort);
            System.out.println("✝️ " + numeMort + " a decedat.");
        }
    }

    public void gestioneazaUpdate(String jsonContent) {
        if (myGui != null) myGui.updateFishermanData(jsonContent);
    }

    public void adaugaLaBuget(int suma) {
        this.buget += suma;
        if (myGui != null) {
            myGui.updateLiderStats(buget);
            System.out.println("Am primit " + suma + " galbeni taxe.");
        }
    }

    public LiderGui getGui() { return myGui; }

    // --- METODE PENTRU LLM / UI ---

    public void cereAnalizaAI() {
        if (myGui != null) myGui.setAnalizaButtonEnabled(false);

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                String dateAgenti = (myGui != null) ? myGui.getRaportDate() : "Fara date.";
                String contextGlobal = "Buget: " + buget + ". Taxa: " + valoareTaxa + ". Date: " + dateAgenti;
                String instructiune = "Analizeaza situatia orasului scurt si amuzant.";

                String raspuns = cereRaspunsDirect(contextGlobal, instructiune);
                if (myGui != null) myGui.afiseazaRaportAI(raspuns);
            }
        });

        addBehaviour(new WakerBehaviour(this, 10000) {
            @Override
            protected void onWake() { if (myGui != null) myGui.setAnalizaButtonEnabled(true); }
        });
    }

    // --- HELPERE PRIVATE ---

    private int meniuStartJoc() {
        String[] options = {"Joc Nou (Reset)", "Continuă Salvarea"};
        return JOptionPane.showOptionDialog(null, "Bine ai venit în CitySim!", "Startup Menu",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
    }

    private void startColectareAutomata() {
        trimiteOrdin("tax-collector", "COLECTEAZA_AUTOMAT:" + valoareTaxa, ACLMessage.REQUEST);
    }

    private void trimiteSemnalTuturor(String comanda) {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("food-producer");
            template.addServices(sd);
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                ACLMessage msg = new ACLMessage(ACLMessage.PROPAGATE);
                msg.setContent(comanda);
                for (DFAgentDescription agent : result) if (agent.getName() != null) msg.addReceiver(agent.getName());
                send(msg);
            }
        } catch (Exception e) { System.out.println("Eroare semnal: " + e.getMessage()); }
    }

    private void trimiteOrdin(String serviceType, String content, int performative) {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(serviceType);
            template.addServices(sd);
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                ACLMessage msg = new ACLMessage(performative);
                msg.addReceiver(result[0].getName());
                msg.setContent(content);
                send(msg);
            }
        } catch (Exception e) {}
    }

    private String cereRaspunsDirect(String context, String instructiune) {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("llm-service");
            template.addServices(sd);
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                req.addReceiver(result[0].getName());
                req.setContent("Analist|||" + context + "|||" + instructiune);
                req.setConversationId(String.valueOf(System.currentTimeMillis()));
                send(req);
                ACLMessage reply = blockingReceive(MessageTemplate.MatchSender(result[0].getName()), 5000);
                return (reply != null) ? reply.getContent() : "Timeout LLM";
            }
        } catch (Exception e) {}
        return "Eroare LLM";
    }

    // --- SAVE / LOAD / UI CONTROLS ---

    private void incarcaSalvare() {
        Properties p = new Properties();
        try (InputStream input = new FileInputStream("oras_data.properties")) {
            p.load(input);
            buget = Integer.parseInt(p.getProperty("buget", "100"));
            contorPescari = Integer.parseInt(p.getProperty("nr_pescari", "1"));
        } catch (IOException e) { buget = 100; contorPescari = 1; }
    }

    private void salveazaJoc() {
        Properties p = new Properties();
        p.setProperty("buget", String.valueOf(buget));
        p.setProperty("nr_pescari", String.valueOf(contorPescari));
        try (OutputStream output = new FileOutputStream("oras_data.properties")) { p.store(output, "Save"); } catch (IOException e) {}
    }

    public void setTaxa(int val) { this.valoareTaxa = val; }
    public void comandaMunca() { regimMuncaDublu = !regimMuncaDublu; if(myGui != null) myGui.updateWorkMode(regimMuncaDublu); }
    public void comandaColectareTaxe() { addBehaviour(new OneShotBehaviour() { @Override public void action() { trimiteOrdin("tax-collector", "colecteaza", ACLMessage.REQUEST); }}); }
    public void comandaFestival() { addBehaviour(new OneShotBehaviour() { @Override public void action() { trimiteSemnalTuturor("PARTY_TIME"); }}); }
    public void inchideTot() {
        trimiteSemnalTuturor("DIE");
        new Thread(() -> { try { Thread.sleep(1000); getContainerController().kill(); } catch (Exception e) {} }).start();
    }
    @Override protected void takeDown() { salveazaJoc(); if (myGui != null) myGui.dispose(); }
}