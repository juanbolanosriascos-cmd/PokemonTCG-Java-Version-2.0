package gui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import logic.battlesystem.Deck;
import logic.battlesystem.DeckFactory;
import logic.battlesystem.Game;
import logic.battlesystem.Hand;
import logic.battlesystem.Player;
import logic.cards.AbstractCard;
import logic.cards.Attack;
import logic.cards.PokemonCard;
import logic.cards.TrainerCard;
import logic.cards.Type;

public class GameWindows extends JPanel {

    // Rutas y colores
    private static final String BG_PATH = "/media/images/backgrounds/Bosque_Verde_HGSS.png";
    private static final String LOGO3_PATH = "/media/images/windowsAccessories/logo3.png";
    private static final String ENERGY_BASE_PATH = "/media/images/typesPokemons/typesEnergy/";

    private static final Color RED_COLOR = new Color(255, 56, 49);
    private static final Color BLUE_COLOR = new Color(14, 76, 117);
    private static final Color CARD_COLOR = new Color(208, 214, 222, 235);
    private static final Color SELECTION_BORDER_COLOR = new Color(255, 204, 0);

    // Modos de acción
    private static final String MODE_NONE = "NONE";
    private static final String MODE_ENERGY = "ENERGY";
    private static final String MODE_EVOLVE = "EVOLVE";
    private static final String MODE_PLACE_BENCH = "PLACE_BENCH";
    private static final String MODE_USE_POTION = "USE_POTION";

    // Componentes visuales
    private Image backgroundImage;
    private JLabel playerTwoScoreLabel;
    private JLabel playerOneScoreLabel;
    private JLabel turnLabel;

    private PokemonSlotPanel playerTwoActiveSlot;
    private PokemonSlotPanel playerTwoBenchSlotOne;
    private PokemonSlotPanel playerTwoBenchSlotTwo;
    private PokemonSlotPanel playerOneActiveSlot;
    private PokemonSlotPanel playerOneBenchSlotOne;
    private PokemonSlotPanel playerOneBenchSlotTwo;

    private JPanel handCardsPanel;
    private JButton energyButton;
    private JButton evolveButton;
    private JButton attackButton;
    private JButton retreatButton;
    private JButton endTurnButton;
    private JLabel fieldEnergyIconLabel;
    private JTextArea eventLogArea;

    // Estado de la partida
    private String playerOneName;
    private String playerOneDeck;
    private String playerTwoName;
    private String playerTwoDeck;
    private int playerOnePoints = 0;
    private int playerTwoPoints = 0;
    private int playerOneDeckCount = 0;
    private int playerTwoDeckCount = 0;
    private boolean gameFinished;
    private int currentPlayer;
    private boolean fieldEnergyAvailable = true;
    private String currentFieldEnergy = "Fire";

    private List<VisualCardData> playerOneHand = new ArrayList<>();
    private List<VisualCardData> playerTwoHand = new ArrayList<>();
    private PokemonCard[] playerOneBenchOrder = new PokemonCard[2];
    private PokemonCard[] playerTwoBenchOrder = new PokemonCard[2];

    private String currentMode = MODE_NONE;
    private int selectedCardIndex = -1;
    private JLabel selectedCardLabel = null;
    private PokemonCard selectedBenchPokemon = null;
    private AbstractCard selectedPotionCard = null;

    // Modelo del juego
    private Game game;
    private Player playerOne;
    private Player playerTwo;
    private boolean playerOneStarts;

    public GameWindows(String playerOneName, String playerOneDeck, String playerTwoName, String playerTwoDeck, boolean playerOneStarts) {
        this.playerOneName = playerOneName;
        this.playerOneDeck = playerOneDeck;
        this.playerTwoName = playerTwoName;
        this.playerTwoDeck = playerTwoDeck;
        this.playerOneStarts = playerOneStarts;

        if (playerOneStarts) {
            this.currentPlayer = 1;
            this.currentFieldEnergy = mapDeckToEnergy(playerOneDeck);
        } else {
            this.currentPlayer = 2;
            this.currentFieldEnergy = mapDeckToEnergy(playerTwoDeck);
        }

        loadBackground();
        initComponents();
        BackgroundMusic.playBattle();
    }

    // =========================================================
    // INICIALIZACIÓN DE PARTIDA Y SINCRONIZACIÓN CON MODELO
    // =========================================================

    public void startMatch() {
        setupMatch(playerOneStarts);
        addLog("Empieza la partida: " + playerOneName + " contra " + playerTwoName + ".");
        addLog(game.getCurrentPlayer().getName() + " ganó la moneda y juega primero.");
        showTurnTransition();
        refreshBoardFromGame();
    }

    private void setupMatch(boolean playerOneStarts) {
        playerOne = new Player(playerOneName, createDeckFromName(playerOneDeck), new Hand(),
                new ArrayList<>(), null, (byte) 0, (byte) 0);
        playerTwo = new Player(playerTwoName, createDeckFromName(playerTwoDeck), new Hand(),
                new ArrayList<>(), null, (byte) 0, (byte) 0);
        game = new Game(playerOne, playerTwo);

        if (!game.prepareInitialHands()) {
            showWarning("No se pudieron repartir las manos iniciales.");
            return;
        }

        chooseInitialField(playerOne, playerOneBenchOrder);
        chooseInitialField(playerTwo, playerTwoBenchOrder);

        if (playerOneStarts) {
            game.setCurrentPlayer(playerOne);
        } else {
            game.setCurrentPlayer(playerTwo);
        }
        game.startTurn();
    }

    private Deck createDeckFromName(String deckName) {
        if (deckName == null) {
            return null;
        }
        if (deckName.equals("Fuego")) {
            return DeckFactory.createFireDeck();
        }
        if (deckName.equals("Agua")) {
            return DeckFactory.createWaterDeck();
        }
        if (deckName.equals("Eléctrico")) {
            return DeckFactory.createElectricDeck();
        }
        if (deckName.equals("Psíquico")) {
            return DeckFactory.createPsychicDeck();
        }
        return null;
    }

    private void chooseInitialField(Player player, PokemonCard[] benchOrder) {
        JOptionPane.showMessageDialog(this,
                "Preparación de " + player.getName() + ".\nQue el otro jugador no mire la pantalla.",
                "Preparación", JOptionPane.INFORMATION_MESSAGE);

        PokemonCard active = null;
        while (active == null) {
            active = askBasicPokemon(player, "Elige tu Pokémon activo:", false);
        }
        player.selectInitialActivePokemon(active);

        int slotIndex = 0;
        while (player.getBank().size() < 2 && !player.getHand().getBasicPokemonCards().isEmpty()) {
            PokemonCard benchPokemon = askBasicPokemon(player, "¿Quieres poner un Pokémon en la banca?", true);
            if (benchPokemon == null) {
                break;
            }
            player.placeOnBench(benchPokemon);
            benchOrder[slotIndex] = benchPokemon;
            slotIndex++;
        }
    }

    private PokemonCard askBasicPokemon(Player player, String message, boolean allowFinish) {
        ArrayList<PokemonCard> basics = player.getHand().getBasicPokemonCards();
        int optionCount = basics.size();
        if (allowFinish) {
            optionCount = basics.size() + 1;
        }
        if (optionCount == 0) {
            return null;
        }

        String[] options = new String[optionCount];
        for (int i = 0; i < basics.size(); i++) {
            PokemonCard pokemon = basics.get(i);
            options[i] = pokemon.getName() + " (" + pokemon.getHealthPoints() + " PS)";
        }
        if (allowFinish) {
            options[optionCount - 1] = "Terminar";
        }

        int choice = JOptionPane.showOptionDialog(this, message, player.getName(),
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice < 0 || choice >= basics.size()) {
            return null;
        }
        return basics.get(choice);
    }

    private void refreshBoardFromGame() {
        if (game == null || game.getCurrentPlayer() == null) {
            return;
        }
        loadPlayerField(1, playerOne, playerOneDeck, playerOneBenchOrder);
        loadPlayerField(2, playerTwo, playerTwoDeck, playerTwoBenchOrder);

        if (game.getCurrentPlayer() == playerOne) {
            currentPlayer = 1;
        } else {
            currentPlayer = 2;
        }
        currentFieldEnergy = mapDeckToEnergy(getCurrentPlayerDeck());
        fieldEnergyAvailable = game.getCurrentPlayer().getAvailableEnergy() > 0;
        clearSelection();
        updateTurnVisualState();
    }

    private void loadPlayerField(int playerNumber, Player player, String deckName, PokemonCard[] benchOrder) {
        String energyType = mapDeckToEnergy(deckName);
        syncBenchSlots(player.getBank(), benchOrder);

        setPokemonSlot(playerNumber, 0, toVisualPokemon(player.getActivePokemon(), energyType));
        setPokemonSlot(playerNumber, 1, toVisualPokemon(benchOrder[0], energyType));
        setPokemonSlot(playerNumber, 2, toVisualPokemon(benchOrder[1], energyType));
        setPlayerHand(playerNumber, toVisualHand(player.getHand()));
        if (playerNumber == 1) {
            playerOneDeckCount = player.getDeck().size();
        } else {
            playerTwoDeckCount = player.getDeck().size();
        }
        setPlayerPoints(playerNumber, player.getPrizePoints());
    }

    private void syncBenchSlots(ArrayList<PokemonCard> bank, PokemonCard[] benchOrder) {
        for (int i = 0; i < benchOrder.length; i++) {
            if (benchOrder[i] != null && !bank.contains(benchOrder[i])) {
                benchOrder[i] = null;
            }
        }
        for (int i = 0; i < bank.size(); i++) {
            PokemonCard pokemon = bank.get(i);
            if (benchOrder[0] != pokemon && benchOrder[1] != pokemon) {
                if (benchOrder[0] == null) {
                    benchOrder[0] = pokemon;
                } else if (benchOrder[1] == null) {
                    benchOrder[1] = pokemon;
                }
            }
        }
    }

    private VisualPokemonData toVisualPokemon(PokemonCard pokemon, String energyType) {
        if (pokemon == null) {
            return null;
        }
        ArrayList<Attack> attackList = pokemon.getAttacks();
        String[] attackNames = new String[attackList.size()];
        int[] attackDamages = new int[attackList.size()];
        int[] energyCosts = new int[attackList.size()];
        for (int i = 0; i < attackList.size(); i++) {
            attackNames[i] = attackList.get(i).getName();
            attackDamages[i] = attackList.get(i).getBaseDamage();
            energyCosts[i] = attackList.get(i).getEnergyCost();
        }

        VisualPokemonData data = new VisualPokemonData(pokemon.getName(), pokemon.getImagePath(),
                pokemon.getAudioPath(), pokemon.getHealthPoints(), pokemon.getMaxHealthPoints(),
                pokemon.getRetreatCost(), attackNames, attackDamages, energyCosts);
        data.energyType = energyType;
        data.energyDisplayName = getEnergyDisplayName(energyType);
        data.energyCount = pokemon.getAttachedEnergy();
        data.stage = getStageName(pokemon);
        return data;
    }

    private String getStageName(PokemonCard pokemon) {
        if (pokemon.getPhase() == 2) {
            return "Fase 1";
        }
        if (pokemon.getPhase() == 3) {
            return "Fase 2";
        }
        return "Básico";
    }

    private List<VisualCardData> toVisualHand(Hand hand) {
        List<VisualCardData> visualHand = new ArrayList<>();
        for (AbstractCard card : hand.getCards()) {
            if (card instanceof PokemonCard) {
                PokemonCard pokemon = (PokemonCard) card;
                VisualPokemonData pokemonData = toVisualPokemon(pokemon, "");
                visualHand.add(new VisualCardData(pokemon.getName(), "POKEMON", pokemon.getImagePath(),
                        pokemon.getEvolvesFrom(), pokemonData));
            } else {
                String category = "OBJETO";
                if (card instanceof TrainerCard) {
                    category = "ENTRENADOR";
                }
                visualHand.add(new VisualCardData(card.getName(), category, buildSpritePath(card), null, null));
            }
        }
        return visualHand;
    }

    private String buildSpritePath(AbstractCard card) {
        if (card instanceof PokemonCard) {
            return ((PokemonCard) card).getImagePath();
        }
        String name = card.getName();
        if (name.equals("Pocion") || name.equals("Poción")) {
            return "/media/images/objects/potion.png";
        }
        if (name.equals("Poke Ball")) {
            return "/media/images/objects/pokeball.png";
        }
        if (name.equals("Turbo Velocidad")) {
            return "/media/images/objects/x-speed.png";
        }
        if (name.equals("Investigacion del Profesor")) {
            return "/media/images/trainers/oak.png";
        }
        if (name.equals("Misty")) {
            return "/media/images/trainers/misty.png";
        }
        return null;
    }

    // =========================================================
    // CONSTRUCCIÓN DE INTERFAZ
    // =========================================================

    private void loadBackground() {
        URL resourceUrl = getClass().getResource(BG_PATH);
        if (resourceUrl != null) {
            backgroundImage = new ImageIcon(resourceUrl).getImage();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 26, getWidth(), 418, this);
        }
    }

    private void initComponents() {
        setLayout(null);
        setPreferredSize(new Dimension(800, 500));

        // Barras superior e inferior
        JPanel topRedBar = new JPanel();
        topRedBar.setBackground(RED_COLOR);
        topRedBar.setBounds(0, 0, 800, 26);
        add(topRedBar);

        JPanel bottomRedBar = new JPanel();
        bottomRedBar.setBackground(RED_COLOR);
        bottomRedBar.setLayout(null);
        bottomRedBar.setBounds(0, 444, 800, 56);
        add(bottomRedBar);

        JLabel logoLabel = new JLabel();
        ImageIcon logoIcon = MediaUtils.loadIcon(LOGO3_PATH, 215, 50);
        if (logoIcon != null) {
            logoLabel.setIcon(logoIcon);
        }
        logoLabel.setBounds(10, 3, 215, 50);
        bottomRedBar.add(logoLabel);

        // Energía del campo
        RoundedPanel fieldEnergyPanel = new RoundedPanel(20, CARD_COLOR);
        fieldEnergyPanel.setLayout(null);
        fieldEnergyPanel.setBounds(455, 8, 195, 40);
        bottomRedBar.add(fieldEnergyPanel);

        JLabel fieldEnergyTextLabel = new JLabel("Energía del campo", SwingConstants.CENTER);
        fieldEnergyTextLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        fieldEnergyTextLabel.setBounds(8, 10, 120, 20);
        fieldEnergyPanel.add(fieldEnergyTextLabel);

        fieldEnergyIconLabel = new JLabel("", SwingConstants.CENTER);
        fieldEnergyIconLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        fieldEnergyIconLabel.setBounds(130, 8, 56, 24);
        fieldEnergyPanel.add(fieldEnergyIconLabel);

        endTurnButton = createBlueButton("Pasar turno", 15);
        endTurnButton.setBounds(660, 6, 130, 44);
        bottomRedBar.add(endTurnButton);

        // Marcadores y turno
        RoundedPanel playerTwoScorePanel = new RoundedPanel(25, CARD_COLOR);
        playerTwoScorePanel.setLayout(null);
        playerTwoScorePanel.setBounds(12, 32, 205, 34);
        add(playerTwoScorePanel);

        playerTwoScoreLabel = new JLabel(playerTwoName + ": 0/3", SwingConstants.CENTER);
        playerTwoScoreLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        playerTwoScoreLabel.setBounds(5, 4, 195, 26);
        playerTwoScorePanel.add(playerTwoScoreLabel);

        RoundedPanel turnPanel = new RoundedPanel(20, BLUE_COLOR);
        turnPanel.setLayout(null);
        turnPanel.setBounds(305, 34, 190, 30);
        add(turnPanel);

        turnLabel = new JLabel("", SwingConstants.CENTER);
        turnLabel.setForeground(Color.WHITE);
        turnLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        turnLabel.setBounds(5, 3, 180, 24);
        turnPanel.add(turnLabel);

        RoundedPanel playerOneScorePanel = new RoundedPanel(25, CARD_COLOR);
        playerOneScorePanel.setLayout(null);
        playerOneScorePanel.setBounds(583, 32, 205, 34);
        add(playerOneScorePanel);

        playerOneScoreLabel = new JLabel(playerOneName + ": 0/3", SwingConstants.CENTER);
        playerOneScoreLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        playerOneScoreLabel.setBounds(5, 4, 195, 26);
        playerOneScorePanel.add(playerOneScoreLabel);

        // Espacios del jugador 2 (izquierda)
        playerTwoBenchSlotOne = createPokemonSlot(2, 1, 15, 74, 158, 84);
        playerTwoBenchSlotTwo = createPokemonSlot(2, 2, 15, 165, 158, 84);
        addFieldLabel("Pokémon en banca", 32, 253, 124);

        playerTwoActiveSlot = createPokemonSlot(2, 0, 188, 130, 168, 92);
        addFieldLabel("Pokémon activo", 217, 226, 110);

        // Espacios del jugador 1 (derecha)
        playerOneActiveSlot = createPokemonSlot(1, 0, 444, 130, 168, 92);
        addFieldLabel("Pokémon activo", 473, 226, 110);

        playerOneBenchSlotOne = createPokemonSlot(1, 1, 627, 74, 158, 84);
        playerOneBenchSlotTwo = createPokemonSlot(1, 2, 627, 165, 158, 84);
        addFieldLabel("Pokémon en banca", 644, 253, 124);

        // Registro de eventos
        RoundedPanel eventLogPanel = new RoundedPanel(20, CARD_COLOR);
        eventLogPanel.setLayout(null);
        eventLogPanel.setBounds(188, 252, 424, 100);
        add(eventLogPanel);

        eventLogArea = new JTextArea();
        eventLogArea.setEditable(false);
        eventLogArea.setLineWrap(true);
        eventLogArea.setWrapStyleWord(true);
        eventLogArea.setFont(new Font("SansSerif", Font.PLAIN, 11));
        eventLogArea.setOpaque(false);

        JScrollPane eventLogScroll = new JScrollPane(eventLogArea);
        eventLogScroll.setOpaque(false);
        eventLogScroll.getViewport().setOpaque(false);
        eventLogScroll.setBorder(null);
        eventLogScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        eventLogScroll.setBounds(10, 6, 406, 88);
        eventLogPanel.add(eventLogScroll);

        // Mano desplazable
        addFieldLabel("Mano", 15, 336, 75);

        RoundedPanel handContainerPanel = new RoundedPanel(25, CARD_COLOR);
        handContainerPanel.setLayout(null);
        handContainerPanel.setBounds(15, 360, 590, 76);
        add(handContainerPanel);

        handCardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        handCardsPanel.setOpaque(false);

        JScrollPane handScrollPane = new JScrollPane(handCardsPanel);
        handScrollPane.setOpaque(false);
        handScrollPane.getViewport().setOpaque(false);
        handScrollPane.setBorder(null);
        handScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        handScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        handScrollPane.setBounds(10, 4, 570, 68);
        handContainerPanel.add(handScrollPane);

        // Botones de acción
        energyButton = createBlueButton("Energía", 12);
        energyButton.setBounds(615, 360, 84, 35);
        add(energyButton);

        evolveButton = createBlueButton("Evolucionar", 12);
        evolveButton.setBounds(704, 360, 86, 35);
        add(evolveButton);

        attackButton = createBlueButton("Atacar", 12);
        attackButton.setBounds(615, 401, 84, 35);
        add(attackButton);

        retreatButton = createBlueButton("Retirada", 12);
        retreatButton.setBounds(704, 401, 86, 35);
        add(retreatButton);

        configureButtonActions();
        updateTurnVisualState();
    }

    private PokemonSlotPanel createPokemonSlot(int owner, int slotType, int x, int y, int width, int height) {
        final PokemonSlotPanel slot = new PokemonSlotPanel(owner, slotType, CARD_COLOR);
        slot.setBounds(x, y, width, height);
        slot.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleFieldSlotClick(slot);
            }
        });
        add(slot);
        return slot;
    }

    private void addFieldLabel(String text, int x, int y, int width) {
        RoundedPanel pill = new RoundedPanel(16, CARD_COLOR);
        pill.setLayout(null);
        pill.setBounds(x, y, width, 20);
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setBounds(2, 2, width - 4, 16);
        pill.add(lbl);
        add(pill);
    }

    private JButton createBlueButton(String text, int fontSize) {
        JButton btn = new JButton(text);
        btn.setBackground(BLUE_COLOR);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        return btn;
    }

    // =========================================================
    // EVENTOS DE BOTONES Y ACCIONES DE COMBATE
    // =========================================================

    private void configureButtonActions() {
        energyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (game == null || gameFinished) return;
                Player current = game.getCurrentPlayer();
                String error = current.getAttachEnergyError(current.getActivePokemon());
                if (error != null && current.getAvailableEnergy() <= 0) {
                    showWarning(error);
                    return;
                }
                toggleActionMode(MODE_ENERGY, energyButton);
            }
        });

        evolveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (game == null || gameFinished) return;
                toggleActionMode(MODE_EVOLVE, evolveButton);
                if (currentMode.equals(MODE_EVOLVE)) {
                    JOptionPane.showMessageDialog(GameWindows.this,
                            "Selecciona un Pokémon de tu equipo para evolucionarlo.",
                            "Evolucionar", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        retreatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (game == null || gameFinished) return;
                clearSelection();
                retreatActivePokemon();
            }
        });

        attackButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (game == null || gameFinished) return;
                clearSelection();
                attackWithActivePokemon();
            }
        });

        endTurnButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (game == null || gameFinished) return;
                if (game.getCurrentPlayer().getActivePokemon() == null) {
                    showWarning("El juego no puede seguir si tu puesto activo está vacío.");
                    return;
                }

                clearSelection();
                addLog(game.getCurrentPlayer().getName() + " pasó el turno.");
                game.endTurn();
                showTurnTransition();
                boolean turnStarted = game.startTurn();
                if (turnStarted) {
                    addLog("Turno " + game.getTurnNumber() + ": " + game.getCurrentPlayer().getName()
                            + " roba una carta y recibe su energía.");
                }
                refreshBoardFromGame();

                if (!turnStarted && game.isFinished() && game.getWinner() != null) {
                    finishGame();
                }
            }
        });
    }

    private void attackWithActivePokemon() {
        Player current = game.getCurrentPlayer();
        Player opponent = game.getOpponentOf(current);
        PokemonCard attacker = current.getActivePokemon();
        if (attacker == null || attacker.getAttacks().isEmpty()) {
            showWarning(current.getAttackError(opponent, null));
            return;
        }

        ArrayList<Attack> attacks = attacker.getAttacks();
        String[] options = new String[attacks.size()];
        for (int i = 0; i < attacks.size(); i++) {
            Attack attack = attacks.get(i);
            options[i] = attack.getName() + " (" + attack.getBaseDamage() + " de daño - Costo: " + attack.getEnergyCost() + ")";
        }

        int choice = JOptionPane.showOptionDialog(this, "Selecciona el ataque de " + attacker.getName() + ":",
                "Atacar", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice < 0) {
            return;
        }

        Attack attack = attacks.get(choice);
        String error = current.getAttackError(opponent, attack);
        if (error != null) {
            showWarning(error);
            return;
        }

        MediaUtils.playAudio(attacker.getAudioPath());

        PokemonCard defender = opponent.getActivePokemon();
        int damage = current.getAttackDamage(opponent, attack);
        int pointsBefore = current.getPrizePoints();
        current.attackOpponent(opponent, attack);

        String result = attacker.getName() + " usó " + attack.getName() + " y le hizo " + damage + " de daño a " + defender.getName() + ".";
        if (damage > attack.getBaseDamage()) {
            result += "\n¡Es débil a este tipo: +" + (damage - attack.getBaseDamage()) + " de daño!";
        }
        if (defender.isDefeated()) {
            int pointsWon = current.getPrizePoints() - pointsBefore;
            result += "\n" + defender.getName() + " fue derrotado. " + current.getName() + " gana " + pointsWon + " punto(s) de premio.";
        }
        addLog(result.replace("\n", " "));
        JOptionPane.showMessageDialog(this, result, "Ataque", JOptionPane.INFORMATION_MESSAGE);

        refreshBoardFromGame();
        if (game.checkWinner()) {
            finishGame();
            return;
        }
        if (opponent.getActivePokemon() == null) {
            promoteFromBench(opponent);
            refreshBoardFromGame();
        }
    }

    private void promoteFromBench(Player player) {
        ArrayList<PokemonCard> bench = player.getBank();
        if (bench.isEmpty()) {
            return;
        }
        String[] options = new String[bench.size()];
        for (int i = 0; i < bench.size(); i++) {
            options[i] = bench.get(i).getName() + " (" + bench.get(i).getHealthPoints() + " PS)";
        }
        int choice = -1;
        while (choice < 0) {
            choice = JOptionPane.showOptionDialog(this, player.getName() + ", elige tu nuevo Pokémon activo:",
                    "Nuevo Pokémon activo", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        }
        player.promotePokemon(bench.get(choice));
        addLog(player.getName() + " sube a " + player.getActivePokemon().getName() + " como Pokémon activo.");
    }

    private void retreatActivePokemon() {
        Player current = game.getCurrentPlayer();
        ArrayList<PokemonCard> bench = current.getBank();
        if (bench.isEmpty()) {
            showWarning(current.getRetreatError(null));
            return;
        }

        String[] options = new String[bench.size()];
        for (int i = 0; i < bench.size(); i++) {
            options[i] = bench.get(i).getName() + " (" + bench.get(i).getHealthPoints() + " PS)";
        }
        int choice = JOptionPane.showOptionDialog(this,
                "¿Qué Pokémon de la banca reemplaza a " + current.getActivePokemon().getName()
                        + "? (costo: " + current.getActiveRetreatCost() + " energía)",
                "Retirada", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice < 0) {
            return;
        }

        PokemonCard replacement = bench.get(choice);
        String error = current.getRetreatError(replacement);
        if (error != null) {
            showWarning(error);
            return;
        }
        String retreatedName = current.getActivePokemon().getName();
        current.retreatActive(replacement);
        addLog(current.getName() + " retira a " + retreatedName + " y entra " + replacement.getName() + ".");
        refreshBoardFromGame();
    }

    // =========================================================
    // INTERACCIÓN CON TABLERO Y MANO
    // =========================================================

    private void handleFieldSlotClick(PokemonSlotPanel slot) {
        if (game == null || gameFinished) {
            return;
        }
        Player current = game.getCurrentPlayer();
        boolean isAlly = (slot.getOwner() == currentPlayer);
        PokemonCard pokemon = getPokemonInSlot(slot);

        if (currentMode.equals(MODE_PLACE_BENCH)) {
            handlePlaceBenchClick(current, slot, isAlly, pokemon);
            return;
        }
        if (currentMode.equals(MODE_USE_POTION)) {
            handleUsePotionClick(current, isAlly, pokemon);
            return;
        }
        if (currentMode.equals(MODE_ENERGY)) {
            handleAttachEnergyClick(current, isAlly, pokemon);
            return;
        }
        if (currentMode.equals(MODE_EVOLVE)) {
            handleEvolveClick(current, slot, isAlly, pokemon);
            return;
        }

        if (slot.getData() != null) {
            MediaUtils.playAudio(slot.getData().audioPath);
        }
    }

    private void handlePlaceBenchClick(Player current, PokemonSlotPanel slot, boolean isAlly, PokemonCard occupyingPokemon) {
        if (!isAlly) {
            showWarning("No puedes colocar Pokémon en el campo rival.");
            return;
        }
        if (slot.getSlotType() == 0) {
            showWarning("Selecciona uno de los dos espacios de la banca.");
            return;
        }
        if (occupyingPokemon != null) {
            showWarning("Ese espacio de la banca ya está ocupado.");
            return;
        }

        String error = current.getPlaceOnBenchError(selectedBenchPokemon);
        if (error != null) {
            showWarning(error);
            clearSelection();
            return;
        }

        current.placeOnBench(selectedBenchPokemon);
        addLog(current.getName() + " pone a " + selectedBenchPokemon.getName() + " en la banca.");
        PokemonCard[] benchOrder = getCurrentBenchOrder();
        benchOrder[slot.getSlotType() - 1] = selectedBenchPokemon;
        MediaUtils.playAudio(selectedBenchPokemon.getAudioPath());
        refreshBoardFromGame();
    }

    private void handleUsePotionClick(Player current, boolean isAlly, PokemonCard pokemon) {
        if (!isAlly) {
            showWarning("No puedes curar a un Pokémon enemigo. Elige uno de tu equipo o toca la Poción para cancelar.");
            return;
        }
        if (pokemon == null) {
            showWarning("No hay ningún Pokémon en este espacio.");
            return;
        }
        if (pokemon.getHealthPoints() >= pokemon.getMaxHealthPoints()) {
            showWarning(pokemon.getName() + " ya tiene todos sus PS (" + pokemon.getHealthPoints() + "/"
                    + pokemon.getMaxHealthPoints() + "). Elige otro Pokémon o toca la Poción para cancelar.");
            return;
        }

        String error = current.getPlayCardError(selectedPotionCard);
        if (error != null) {
            showWarning(error);
            clearSelection();
            return;
        }

        if (!current.playPlayableCard(selectedPotionCard, game, pokemon)) {
            showWarning(selectedPotionCard.getName() + " no tuvo efecto en este momento.");
        } else {
            addLog(current.getName() + " usa Poción en " + pokemon.getName() + " (+20 PS).");
        }
        refreshBoardFromGame();
    }

    private void handleAttachEnergyClick(Player current, boolean isAlly, PokemonCard pokemon) {
        if (!isAlly || pokemon == null) {
            showWarning("Elige uno de tus Pokémon para adjuntarle la energía.");
            return;
        }
        String error = current.getAttachEnergyError(pokemon);
        if (error != null) {
            showWarning(error);
        } else {
            current.attachEnergy(pokemon);
            addLog(current.getName() + " adjunta una energía a " + pokemon.getName() + ".");
        }
        refreshBoardFromGame();
    }

    private void handleEvolveClick(Player current, PokemonSlotPanel slot, boolean isAlly, PokemonCard pokemon) {
        if (!isAlly || pokemon == null) {
            showWarning("Elige uno de tus Pokémon para evolucionarlo.");
            return;
        }

        PokemonCard evolution = null;
        for (AbstractCard card : current.getHand().getCards()) {
            if (card instanceof PokemonCard && pokemon.getName().equals(((PokemonCard) card).getEvolvesFrom())) {
                evolution = (PokemonCard) card;
                break;
            }
        }
        if (evolution == null) {
            showWarning("No tienes en la mano una evolución de " + pokemon.getName() + ".");
            clearSelection();
            return;
        }

        String error = current.getEvolveError(evolution, pokemon);
        if (error != null) {
            showWarning(error);
        } else {
            current.evolvePokemon(evolution, pokemon);
            if (slot.getSlotType() > 0) {
                PokemonCard[] benchOrder = getCurrentBenchOrder();
                benchOrder[slot.getSlotType() - 1] = evolution;
            }
            MediaUtils.playAudio(evolution.getAudioPath());
            addLog(pokemon.getName() + " evolucionó a " + evolution.getName() + ".");
            JOptionPane.showMessageDialog(this, "¡" + pokemon.getName() + " evolucionó a " + evolution.getName() + "!",
                    "Evolución", JOptionPane.INFORMATION_MESSAGE);
        }
        refreshBoardFromGame();
    }

    private void handleHandCardClick(int index, JLabel cardLabel) {
        if (game == null || gameFinished) {
            return;
        }
        Player current = game.getCurrentPlayer();
        AbstractCard card = current.getHand().getCard(index);
        if (card == null) {
            return;
        }

        // Selección de Pokémon básico para banca
        if (card instanceof PokemonCard) {
            PokemonCard pokemon = (PokemonCard) card;
            if (!pokemon.isBasicPokemon()) {
                clearSelection();
                showWarning("Para usar " + pokemon.getName() + ", toca el botón Evolucionar y luego el Pokémon que quieres evolucionar.");
                return;
            }
            if (currentMode.equals(MODE_PLACE_BENCH) && selectedCardIndex == index) {
                clearSelection();
                return;
            }

            clearSelection();
            int confirmation = JOptionPane.showConfirmDialog(this,
                    "¿Deseas seleccionar a " + pokemon.getName() + " para ponerlo en la banca?",
                    "Banca", JOptionPane.YES_NO_OPTION);
            if (confirmation != JOptionPane.YES_OPTION) {
                return;
            }

            String error = current.getPlaceOnBenchError(pokemon);
            if (error != null) {
                showWarning(error);
                return;
            }

            currentMode = MODE_PLACE_BENCH;
            selectedCardIndex = index;
            selectedBenchPokemon = pokemon;
            selectedCardLabel = cardLabel;
            cardLabel.setBorder(BorderFactory.createLineBorder(SELECTION_BORDER_COLOR, 3));
            return;
        }

        // Selección de Poción para curar a un aliado específico
        if (card.getName().equals("Pocion") || card.getName().equals("Poción")) {
            if (currentMode.equals(MODE_USE_POTION) && selectedCardIndex == index) {
                clearSelection();
                return;
            }

            clearSelection();
            int confirmation = JOptionPane.showConfirmDialog(this,
                    "¿Deseas usar " + card.getName() + "? Luego selecciona el Pokémon que quieres curar.",
                    "Usar Poción", JOptionPane.YES_NO_OPTION);
            if (confirmation != JOptionPane.YES_OPTION) {
                return;
            }

            String error = current.getPlayCardError(card);
            if (error != null) {
                showWarning(error);
                return;
            }

            currentMode = MODE_USE_POTION;
            selectedCardIndex = index;
            selectedPotionCard = card;
            selectedCardLabel = cardLabel;
            cardLabel.setBorder(BorderFactory.createLineBorder(SELECTION_BORDER_COLOR, 3));
            return;
        }

        // Resto de cartas de entrenador u objetos
        clearSelection();
        int confirmation = JOptionPane.showConfirmDialog(this, "¿Deseas usar " + card.getName() + "?",
                "Usar carta", JOptionPane.YES_NO_OPTION);
        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }
        String error = current.getPlayCardError(card);
        if (error != null) {
            showWarning(error);
            return;
        }

        PokemonCard target = null;
        if ("Misty".equals(card.getName())) {
            target = chooseWaterPokemon(current);
            if (target == null) {
                return;
            }
        }

        if (!current.playPlayableCard(card, game, target)) {
            showWarning(card.getName() + " no tuvo efecto en este momento.");
        } else {
            addLog(current.getName() + " juega " + card.getName() + ".");
        }
        refreshBoardFromGame();
    }

    private PokemonCard chooseWaterPokemon(Player player) {
        ArrayList<PokemonCard> waterPokemon = new ArrayList<>();
        if (player.getActivePokemon() != null && player.getActivePokemon().getType() == Type.WATER) {
            waterPokemon.add(player.getActivePokemon());
        }
        for (PokemonCard pokemon : player.getBank()) {
            if (pokemon.getType() == Type.WATER) {
                waterPokemon.add(pokemon);
            }
        }
        if (waterPokemon.isEmpty()) {
            showWarning("No tienes Pokémon de Agua en juego para usar Misty.");
            return null;
        }

        String[] options = new String[waterPokemon.size()];
        for (int i = 0; i < waterPokemon.size(); i++) {
            options[i] = waterPokemon.get(i).getName();
        }
        int choice = JOptionPane.showOptionDialog(this, "¿A qué Pokémon de Agua le das la energía?", "Misty",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice < 0) {
            return null;
        }
        return waterPokemon.get(choice);
    }

    // =========================================================
    // ESTADO VISUAL Y CONSULTAS AUXILIARES
    // =========================================================

    private void toggleActionMode(String newMode, JButton targetButton) {
        if (currentMode.equals(newMode)) {
            clearSelection();
        } else {
            clearSelection();
            currentMode = newMode;
            targetButton.setBorder(BorderFactory.createLineBorder(SELECTION_BORDER_COLOR, 3));
        }
    }

    private void clearSelection() {
        currentMode = MODE_NONE;
        selectedCardIndex = -1;
        selectedBenchPokemon = null;
        selectedPotionCard = null;
        if (selectedCardLabel != null) {
            selectedCardLabel.setBorder(null);
            selectedCardLabel = null;
        }
        energyButton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        evolveButton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        retreatButton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
    }

    private void updateTurnVisualState() {
        if (game != null && game.getTurnNumber() > 0) {
            turnLabel.setText("Turno " + game.getTurnNumber() + ": " + getCurrentPlayerName());
        } else {
            turnLabel.setText("Turno: " + getCurrentPlayerName());
        }
        updateFieldEnergyIcon();
        refreshHandVisual();
    }

    private void updateFieldEnergyIcon() {
        if (!fieldEnergyAvailable) {
            fieldEnergyIconLabel.setIcon(null);
            fieldEnergyIconLabel.setText("Usada");
            return;
        }
        fieldEnergyIconLabel.setText("");
        ImageIcon icon = MediaUtils.loadIcon(ENERGY_BASE_PATH + currentFieldEnergy + ".png", 52, 22);
        if (icon != null) {
            fieldEnergyIconLabel.setIcon(icon);
        } else {
            fieldEnergyIconLabel.setText(getEnergyDisplayName(currentFieldEnergy).toUpperCase());
        }
    }

    private void updateScoreMarkers() {
        playerOneScoreLabel.setText(playerOneName + ": " + playerOnePoints + "/3 · Mazo " + playerOneDeckCount);
        playerTwoScoreLabel.setText(playerTwoName + ": " + playerTwoPoints + "/3 · Mazo " + playerTwoDeckCount);
        playerOneScoreLabel.setToolTipText(playerOneScoreLabel.getText());
        playerTwoScoreLabel.setToolTipText(playerTwoScoreLabel.getText());
    }

    private void showTurnTransition() {
        String nextPlayerName = game.getCurrentPlayer().getName();
        handCardsPanel.removeAll();
        handCardsPanel.revalidate();
        handCardsPanel.repaint();
        turnLabel.setText("Cambio de turno");

        String[] options = {"Continuar"};
        JOptionPane.showOptionDialog(this,
                "Turno de " + nextPlayerName + ".\nPásale el computador y presiona Continuar.",
                "Cambio de turno", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);
    }

    private void finishGame() {
        gameFinished = true;
        clearSelection();
        energyButton.setEnabled(false);
        evolveButton.setEnabled(false);
        retreatButton.setEnabled(false);
        attackButton.setEnabled(false);
        endTurnButton.setEnabled(false);
        BackgroundMusic.stop();

        Player winner = game.getWinner();
        Player loser = game.getOpponentOf(winner);
        addLog("Fin de la partida. " + getVictoryText(winner, loser));
        String message = "¡" + winner.getName() + " es el nuevo Campeón Pokémon!\n"
                + getVictoryText(winner, loser) + "\n\n¿Qué quieres hacer?";

        String[] options = {"Nueva partida", "Salir"};
        int choice = JOptionPane.showOptionDialog(this, message, "Partida terminada",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        if (choice == 0) {
            startNewMatch();
        } else if (choice == 1) {
            System.exit(0);
        }
    }

    private String getVictoryText(Player winner, Player loser) {
        switch (game.getVictoryReason()) {
            case PRIZE_POINTS:
                return winner.getName() + " consiguió 3 puntos de premio.";
            case NO_POKEMON_IN_PLAY:
                return loser.getName() + " se quedó sin Pokémon en juego.";
            case EMPTY_DECK:
                return loser.getName() + " no pudo robar carta porque su mazo está vacío.";
            default:
                return "";
        }
    }

    private void startNewMatch() {
        JPanel container = (JPanel) getParent();
        container.remove(this);
        container.add(new CreatePlayers(container, 1), "PLAYER1");
        ((CardLayout) container.getLayout()).show(container, "PLAYER1");
        container.revalidate();
        container.repaint();
        BackgroundMusic.playIntro();
    }

    private void refreshHandVisual() {
        handCardsPanel.removeAll();
        List<VisualCardData> currentHand = getCurrentHand();

        for (int i = 0; i < currentHand.size(); i++) {
            final int index = i;
            VisualCardData card = currentHand.get(i);
            final JLabel spriteLabel = new JLabel();
            spriteLabel.setPreferredSize(new Dimension(54, 54));
            spriteLabel.setHorizontalAlignment(SwingConstants.CENTER);
            spriteLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            spriteLabel.setToolTipText(card.name);

            ImageIcon icon = MediaUtils.loadSprite(card.spritePath, 50, 50);
            if (icon != null) {
                spriteLabel.setIcon(icon);
            } else {
                spriteLabel.setText(card.name);
            }

            spriteLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleHandCardClick(index, spriteLabel);
                }
            });
            handCardsPanel.add(spriteLabel);
        }

        handCardsPanel.revalidate();
        handCardsPanel.repaint();
    }

    private PokemonCard getPokemonInSlot(PokemonSlotPanel slot) {
        Player owner;
        PokemonCard[] benchOrder;
        if (slot.getOwner() == 1) {
            owner = playerOne;
            benchOrder = playerOneBenchOrder;
        } else {
            owner = playerTwo;
            benchOrder = playerTwoBenchOrder;
        }
        if (slot.getSlotType() == 0) {
            return owner.getActivePokemon();
        }
        return benchOrder[slot.getSlotType() - 1];
    }

    private PokemonCard[] getCurrentBenchOrder() {
        if (currentPlayer == 1) {
            return playerOneBenchOrder;
        }
        return playerTwoBenchOrder;
    }

    private List<VisualCardData> getCurrentHand() {
        if (currentPlayer == 1) {
            return playerOneHand;
        }
        return playerTwoHand;
    }

    private String getCurrentPlayerName() {
        if (currentPlayer == 1) {
            return playerOneName;
        }
        return playerTwoName;
    }

    private String getCurrentPlayerDeck() {
        if (currentPlayer == 1) {
            return playerOneDeck;
        }
        return playerTwoDeck;
    }

    private String mapDeckToEnergy(String deck) {
        if (deck == null) return "Normal";
        String deckName = deck.toLowerCase();
        if (deckName.contains("fuego")) return "Fire";
        if (deckName.contains("agua")) return "Water";
        if (deckName.contains("el")) return "Electric";
        if (deckName.contains("ps")) return "Psychic";
        return "Normal";
    }

    private String getEnergyDisplayName(String energyType) {
        if (energyType == null) return "";
        if (energyType.equalsIgnoreCase("Fire")) return "Fuego";
        if (energyType.equalsIgnoreCase("Water")) return "Agua";
        if (energyType.equalsIgnoreCase("Electric")) return "Eléctrica";
        if (energyType.equalsIgnoreCase("Psychic")) return "Psíquica";
        return energyType;
    }

    private void addLog(String message) {
        eventLogArea.append(message + "\n");
        eventLogArea.setCaretPosition(eventLogArea.getDocument().getLength());
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    // =========================================================
    // CONTRATO PÚBLICO Y DTOs
    // =========================================================

    public void setPokemonSlot(int playerNumber, int position, VisualPokemonData data) {
        PokemonSlotPanel targetSlot = getSlot(playerNumber, position);
        if (targetSlot != null) {
            targetSlot.setPokemon(data);
        }
    }

    public void setPlayerHand(int playerNumber, List<VisualCardData> cards) {
        if (playerNumber == 1) {
            this.playerOneHand = cards;
        } else {
            this.playerTwoHand = cards;
        }
        if (this.currentPlayer == playerNumber) {
            refreshHandVisual();
        }
    }

    public void setPlayerPoints(int playerNumber, int points) {
        if (playerNumber == 1) {
            this.playerOnePoints = points;
        } else {
            this.playerTwoPoints = points;
        }
        updateScoreMarkers();
    }

    private PokemonSlotPanel getSlot(int playerNumber, int position) {
        if (playerNumber == 1) {
            if (position == 0) return playerOneActiveSlot;
            if (position == 1) return playerOneBenchSlotOne;
            if (position == 2) return playerOneBenchSlotTwo;
        } else {
            if (position == 0) return playerTwoActiveSlot;
            if (position == 1) return playerTwoBenchSlotOne;
            if (position == 2) return playerTwoBenchSlotTwo;
        }
        return null;
    }

    public static class VisualPokemonData {
        public String name;
        public String spritePath;
        public String audioPath;
        public int currentHp;
        public int maxHp;
        public int retreatCost;
        public String energyType = "";
        public String energyDisplayName = "";
        public int energyCount = 0;
        public String stage = "";
        public String[] attacks;
        public int[] attackDamages;
        public int[] energyCosts;

        public VisualPokemonData(String name, String spritePath, String audioPath, int currentHp, int maxHp,
                int retreatCost, String[] attacks, int[] attackDamages, int[] energyCosts) {
            this.name = name;
            this.spritePath = spritePath;
            this.audioPath = audioPath;
            this.currentHp = currentHp;
            this.maxHp = maxHp;
            this.retreatCost = retreatCost;
            this.attacks = attacks;
            this.attackDamages = attackDamages;
            this.energyCosts = energyCosts;
        }
    }

    public static class VisualCardData {
        public String name;
        public String category;
        public String spritePath;
        public String evolvesFrom;
        public VisualPokemonData pokemonData;

        public VisualCardData(String name, String category, String spritePath, String evolvesFrom, VisualPokemonData pokemonData) {
            this.name = name;
            this.category = category;
            this.spritePath = spritePath;
            this.evolvesFrom = evolvesFrom;
            this.pokemonData = pokemonData;
        }
    }
}