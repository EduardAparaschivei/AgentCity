package main.gui;

import main.agents.LiderAgent;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class LiderGui extends JFrame {
    private LiderAgent myAgent;

    // Componente vizuale
    private GameMapPanel mapPanel;
    private JLabel timeLabel;
    private JLabel liderResourcesLabel;
    private JTextArea statsArea;
    private JTextField taxInput;
    private JButton btnHire;
    private JButton btnKillAll;
    private JButton btnAnaliza;
    private JLabel workModeLabel;

    private Map<String, FishermanData> agentsData = new HashMap<>();

    public LiderGui(LiderAgent agent) {
        super("CitySim - Panou de Control");
        this.myAgent = agent;
        initialize();
    }

    private void initialize() {
        setSize(1000, 700); // Am marit putin fereastra
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- 1. HEADER (Timp, Bani Lider, Titlu) ---
        JPanel topPanel = new JPanel(new GridLayout(1, 3));
        topPanel.setBackground(new Color(40, 40, 40));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("PRIMĂRIA JADE", SwingConstants.LEFT);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));

        timeLabel = new JLabel("STATUS: ...", SwingConstants.CENTER);
        timeLabel.setForeground(Color.YELLOW);
        timeLabel.setFont(new Font("Monospaced", Font.BOLD, 14));

        liderResourcesLabel = new JLabel("Trezorerie: 0 Galbeni", SwingConstants.RIGHT);
        liderResourcesLabel.setForeground(new Color(144, 238, 144));
        liderResourcesLabel.setFont(new Font("Arial", Font.BOLD, 14));

        topPanel.add(titleLabel);
        topPanel.add(timeLabel);
        topPanel.add(liderResourcesLabel);
        add(topPanel, BorderLayout.NORTH);

        // --- 2. CENTER (Harta Jocului) ---
        mapPanel = new GameMapPanel();
        add(mapPanel, BorderLayout.CENTER);

        // --- 3. EAST (Statistici Detaliate) ---
        statsArea = new JTextArea(20, 30);
        statsArea.setEditable(false);
        statsArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statsArea.setBackground(new Color(230, 230, 230));
        JScrollPane scrollPane = new JScrollPane(statsArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Live Feed"));
        add(scrollPane, BorderLayout.EAST);

        // --- 4. SOUTH (Butoane de Control - REORGANIZAT) ---
        // Folosim un BoxLayout vertical pentru a stivui randurile de butoane
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        // Randul A: Status Regim
        JPanel rowA = new JPanel(new FlowLayout(FlowLayout.CENTER));
        workModeLabel = new JLabel("Regim Muncă: NORMAL (Jumătate de normă)");
        workModeLabel.setFont(new Font("Arial", Font.BOLD, 12));
        rowA.add(workModeLabel);

        // Randul B: Actiuni Zilnice
        JPanel rowB = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton btnMunca = new JButton("⚒️ Schimbă Regim Muncă");
        btnAnaliza = new JButton("🤖 Analiză AI Oraș");
        btnAnaliza.setBackground(new Color(70, 130, 180)); // Un albastru mai inchis
        btnAnaliza.setForeground(Color.WHITE);


        btnMunca.addActionListener(e -> myAgent.comandaMunca());
        btnAnaliza.addActionListener(e -> myAgent.cereAnalizaAI());



        rowB.add(btnMunca);
        rowB.add(btnAnaliza);


        // Randul C: Administrare (Taxe variabile, Angajare, Kill)
        JPanel rowC = new JPanel(new FlowLayout(FlowLayout.CENTER));
        rowC.add(new JLabel("Setează Taxa:"));
        taxInput = new JTextField("10", 3);
        rowC.add(taxInput);
        JButton btnSetTax = new JButton("OK");
        btnSetTax.addActionListener(e -> {
            try {
                int val = Integer.parseInt(taxInput.getText());
                myAgent.setTaxa(val);
                JOptionPane.showMessageDialog(this, "Taxa actualizată la " + val);
            } catch(Exception ex) {}
        });
        rowC.add(btnSetTax);

        rowC.add(Box.createHorizontalStrut(20)); // Spatiu gol

        btnHire = new JButton("Angajează (100g)");
        btnHire.addActionListener(e -> myAgent.angajeazaPescar());
        rowC.add(btnHire);

        btnKillAll = new JButton("⚠️ ÎNCHIDE TOT");
        btnKillAll.setBackground(Color.RED);
        btnKillAll.setForeground(Color.WHITE);
        btnKillAll.addActionListener(e -> myAgent.inchideTot());
        rowC.add(btnKillAll);

        // Adaugam randurile in panoul principal de jos
        bottomPanel.add(rowA);
        bottomPanel.add(rowB);
        bottomPanel.add(rowC);

        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    // --- Metode Update ---

    public void updateLiderStats(int buget) {
        liderResourcesLabel.setText("Trezorerie: " + buget + " Galbeni");
    }

    public void updateWorkMode(boolean isDouble) {
        if (isDouble) {
            workModeLabel.setText("Regim Muncă: DUBLU (De dimineața până seara!)");
            workModeLabel.setForeground(Color.RED);
        } else {
            workModeLabel.setText("Regim Muncă: NORMAL (Jumătate de normă)");
            workModeLabel.setForeground(new Color(0, 100, 0));
        }
    }

    public void setTimeText(String text, boolean isDay) {
        timeLabel.setText(text);
        timeLabel.setForeground(isDay ? Color.ORANGE : Color.CYAN);
        mapPanel.repaint();
    }

    public void updateFishermanData(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            String name = json.getString("agent");

            FishermanData data = agentsData.getOrDefault(name, new FishermanData());
            data.name = name;

            // Verificam daca JSON-ul are campul "tip", altfel e Pescar default
            data.type = json.has("tip") ? json.getString("tip") : "Pescar";

            data.location = json.getString("locatie");
            data.money = json.getInt("bani");
            data.mood = json.has("stare") ? json.getString("stare") : "Unknown";

            // Pescarii au pesti si energie, ceilalti nu neaparat
            data.fishCount = json.has("pesti") ? json.getInt("pesti") : 0;
            data.energy = json.has("energie") ? json.getInt("energie") : 100;

            agentsData.put(name, data);
            refreshStatsText();
            mapPanel.repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshStatsText() {
        StringBuilder sb = new StringBuilder();

        // Sortam un pic sau doar iteram
        for (FishermanData fd : agentsData.values()) {
            sb.append("[").append(fd.type).append("] ").append(fd.name).append("\n");

            // Afisam detalii specifice in functie de tip
            if (fd.type.equals("Pescar")) {
                sb.append(" 📍 ").append(fd.location).append("\n")
                        .append(" 🐟 ").append(fd.fishCount).append(" | 💰 ").append(fd.money).append("\n")
                        .append(" ⚡ ").append(fd.energy).append("% | ").append(fd.mood).append("\n");
            }
            else if (fd.type.equals("Medic")) {
                sb.append(" 🏥 Spital Municipal\n")
                        .append(" 💰 Fonduri: ").append(fd.money).append("\n")
                        .append(" 🩺 Status: ").append(fd.mood).append("\n");
            }
            else if (fd.type.equals("Negustor")) {
                sb.append(" 🏪 Piața de Pește\n")
                        .append(" 💰 Capital: ").append(fd.money).append("\n")
                        .append(" 📦 Status: ").append(fd.mood).append("\n");
            }

            sb.append("-----------------\n");
        }
        statsArea.setText(sb.toString());
    }

    // --- HARTA GRAFICA ---
    private class GameMapPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth(); int h = getHeight();

            // 1. ZONA ACASA (Stanga - Verde Deschis)
            g.setColor(new Color(144, 238, 144));
            g.fillRect(0, 0, w/2, h);

            // 2. ZONA LAC (Dreapta - Albastru)
            g.setColor(new Color(135, 206, 235));
            g.fillRect(w/2, 0, w/2, h);

            // Linia de demarcatie
            g.setColor(Color.GRAY);
            g.drawLine(w/2, 0, w/2, h);

            // 3. CABINETUL DOCTORULUI (Nou!)
            // Il desenam in stanga-jos, ca o cladire alba cu cruce
            int docX = 20;
            int docY = h - 120;
            int docW = 150;
            int docH = 100;

            g.setColor(Color.WHITE);
            g.fillRect(docX, docY, docW, docH); // Cladirea
            g.setColor(Color.BLACK);
            g.drawRect(docX, docY, docW, docH); // Contur

            // Crucea Rosie
            g.setColor(Color.RED);
            g.fillRect(docX + 65, docY + 20, 20, 60); // Vertical
            g.fillRect(docX + 45, docY + 40, 60, 20); // Orizontal

            g.setColor(Color.BLACK);
            g.drawString("SPITAL", docX + 55, docY + 90);

            // Etichete Zone
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString("ZONA REZIDENȚIALĂ", 20, 30);
            g.drawString("LACUL DE PESCUIT", w/2 + 20, 30);

            // 4. DESENAREA AGENTILOR
            int yStack = 60; // Pozitia Y pentru desenarea agentilor (sa nu se suprapuna)

            for (FishermanData agent : agentsData.values()) {
                int x, y;

                // Calculam coordonatele in functie de locatia din JSON
                if (agent.location.equals("Lac")) {
                    x = (w / 2) + 100; // Dreapta
                    y = yStack;
                }
                else if (agent.location.equals("Cabinet") || agent.location.equals("Doctor")) {
                    // Il punem INAUNTRUL spitalului
                    x = docX + 30;
                    y = docY + 30; // Suprapus peste cruce daca sunt mai multi, e ok pt demo
                }
                else {
                    // Acasa (Stanga)
                    x = 100;
                    y = yStack;
                }

                // Culoare in functie de mood
                if (agent.mood.equals("Extaz")) g.setColor(Color.MAGENTA);
                else if (agent.mood.equals("Bucuros")) g.setColor(Color.GREEN);
                else if (agent.mood.equals("Nervos")) g.setColor(Color.RED);
                else if (agent.mood.equals("Bolnav")) g.setColor(new Color(139, 69, 19)); // Maro/Bolnav
                else g.setColor(Color.YELLOW);

                // Corp Agent
                g.fillOval(x, y, 40, 40);
                g.setColor(Color.BLACK); g.drawOval(x, y, 40, 40);

                // Nume
                g.drawString(agent.name, x, y - 5);

                // Iconite status
                if (agent.fishCount > 0) g.drawString("🐟 " + agent.fishCount, x + 45, y + 25);
                if (agent.mood.equals("Bolnav")) g.drawString("🚑", x + 45, y + 10);

                // Daca nu e la doctor, crestem Y ca sa desenam urmatorul agent mai jos
                if (!agent.location.equals("Cabinet") && !agent.location.equals("Doctor")) {
                    yStack += 80;
                }
            }
        }
    }

    private static class FishermanData {
        String name;
        String type; // <--- Camp Nou: "Pescar", "Medic", "Negustor"
        String location;
        int fishCount;
        int money;
        String mood;
        int energy;
    }

    public void removeAgent(String agentName) {
        if (agentsData.containsKey(agentName)) {
            agentsData.remove(agentName); // Ștergem din memorie
            refreshStatsText();           // Actualizăm textul din dreapta
            mapPanel.repaint();           // Redesenăm harta (cercul dispare)
        }
    }

    public void setAnalizaButtonEnabled(boolean enabled) {
        btnAnaliza.setEnabled(enabled);
        if (enabled) {
            btnAnaliza.setText("🤖 Analiză AI Oraș");
        } else {
            btnAnaliza.setText("⏳ Așteaptă...");
        }
    }

    // Aceasta metoda colecteaza datele pentru Prompt-ul LLM
    public String getRaportDate() {
        StringBuilder sb = new StringBuilder();
        sb.append("LISTA CETATENI:\n");

        for (FishermanData fd : agentsData.values()) {
            sb.append("- ").append(fd.type).append(" ").append(fd.name)
                    .append(": Bani=").append(fd.money)
                    .append(", Stare=").append(fd.mood)
                    .append(", Energie=").append(fd.energy).append("\n");
        }
        return sb.toString();
    }

    public void afiseazaRaportAI(String text) {
        // Varianta 1: Scriem in zona de log cu un separator clar
        statsArea.append("\n=============================\n");
        statsArea.append("🤖 RAPORT AI:\n");
        statsArea.append(text);
        statsArea.append("\n=============================\n");

        // Varianta 2: Si un Pop-up ca sa iasa in evidenta
        JOptionPane.showMessageDialog(this, text, "Raport Stare Oraș", JOptionPane.INFORMATION_MESSAGE);
    }
}