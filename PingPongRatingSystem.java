import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.IntegerProperty;
import javafx.scene.layout.*;

public class PingPongRatingSystem extends Application {
    private TableView<Player> tableView = new TableView<>();
    private ObservableList<Player> playerData = FXCollections.observableArrayList();
    private VBox recentMatchesBox = new VBox(10); // VBox for recent matches

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Elo Ranking System");

        TableColumn<Player, String> nameColumn = new TableColumn<>("Player Name");
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());

        TableColumn<Player, Integer> ratingColumn = new TableColumn<>("Rating");
        ratingColumn.setCellValueFactory(cellData -> cellData.getValue().ratingProperty().asObject());
        ratingColumn.setComparator((rating1, rating2) -> rating2.compareTo(rating1));

        // New column for games played
        TableColumn<Player, Integer> gamesPlayedColumn = new TableColumn<>("Games Played");
        gamesPlayedColumn.setCellValueFactory(cellData -> cellData.getValue().gamesPlayedProperty().asObject());

        tableView.getColumns().addAll(nameColumn, ratingColumn, gamesPlayedColumn);
        tableView.setItems(playerData);

        // Ensure automatic sorting of the rating table
        tableView.getSortOrder().add(ratingColumn);
        tableView.sort();

        Label player1Label = new Label("Player 1 Name:");
        TextField player1Field = new TextField();

        Label player2Label = new Label("Player 2 Name:");
        TextField player2Field = new TextField();

        Label score1Label = new Label("Player 1 Score:");
        TextField score1Field = new TextField();

        Label score2Label = new Label("Player 2 Score:");
        TextField score2Field = new TextField();

        Button submitButton = new Button("Submit Match");
        submitButton.getStyleClass().add("submit-button");
        submitButton.setOnAction(e -> handleMatchSubmission(
                player1Field.getText(), Integer.parseInt(score1Field.getText()),
                player2Field.getText(), Integer.parseInt(score2Field.getText())));

        VBox inputBox = new VBox(10,
                new HBox(5, player1Label, player1Field, score1Label, score1Field),
                new HBox(5, player2Label, player2Field, score2Label, score2Field),
                submitButton);

        VBox rankingsBox = new VBox(10, new Label("Elo Rankings"), tableView);
        rankingsBox.setPadding(new Insets(10));

        VBox recentMatchesHeader = new VBox(5, new Label("Recent Matches"), new Separator());
        VBox recentMatchesSection = new VBox(recentMatchesHeader, recentMatchesBox);

        // Split the bottom half into two sections horizontally
        HBox bottomHalf = new HBox(rankingsBox, recentMatchesSection);

        // Split the screen into top and bottom halves
        BorderPane root = new BorderPane();
        root.setTop(inputBox);
        root.setBottom(bottomHalf);

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void handleMatchSubmission(String player1Name, int score1, String player2Name, int score2) {
        Player player1 = findOrCreatePlayer(player1Name);
        Player player2 = findOrCreatePlayer(player2Name);

        // Increment games played for both players
        player1.incrementGamesPlayed();
        player2.incrementGamesPlayed();

        // Determine the winner based on the higher score
        Player winner = (score1 > score2) ? player1 : player2;

        // Assuming K is a constant value for simplicity
        int kValue = 32;

        // Calculate expected probabilities using Elo formula
        double player1Probability = 1.0 / (1.0 + Math.pow(10, (player2.getRating() - player1.getRating()) / 400.0));
        double player2Probability = 1.0 / (1.0 + Math.pow(10, (player1.getRating() - player2.getRating()) / 400.0));

        // Calculate Elo changes based on the winner
        int player1EloChange = (int) Math.round(kValue * ((score1 > score2 ? 1 : 0) - player1Probability));
        int player2EloChange = (int) Math.round(kValue * ((score2 > score1 ? 1 : 0) - player2Probability));

        // Update player ratings
        player1.setRating(player1.getRating() + player1EloChange);
        player2.setRating(player2.getRating() + player2EloChange);

        // Update TableView to reflect changes
        tableView.refresh();

        // After updating player ratings, update recent matches display
        updateRecentMatchesView(player1Name, score1, player2Name, score2);
    }

    private void updateRecentMatchesView(String player1Name, int score1, String player2Name, int score2) {
        Label matchLabel = new Label(player1Name + " vs. " + player2Name + " | Score: " + score1 + " - " + score2);

        // Determine the winner based on the higher score
        String winner = (score1 > score2) ? player1Name : player2Name;

        Label winnerLabel = new Label("Winner: " + winner);
        winnerLabel.setStyle("-fx-font-weight: bold");

        VBox matchBox = new VBox(matchLabel, winnerLabel, new Separator());
        recentMatchesBox.getChildren().add(0, matchBox); // Add at the beginning to show the most recent matches first

        // Limit the number of displayed recent matches to 5
        if (recentMatchesBox.getChildren().size() > 5) {
            recentMatchesBox.getChildren().remove(5);
        }
    }

    private Player findOrCreatePlayer(String name) {
        for (Player player : playerData) {
            if (player.getName().equals(name)) {
                return player;
            }
        }

        // Create a new player if not found
        Player newPlayer = new Player(name, 500); // Initial rating of 500
        playerData.add(newPlayer);
        return newPlayer;
    }
}

class Player {
    private final String name;
    private final IntegerProperty rating;
    private final IntegerProperty gamesPlayed;

    public Player(String name, int rating) {
        this.name = name;
        this.rating = new SimpleIntegerProperty(rating);
        this.gamesPlayed = new SimpleIntegerProperty(0);
    }

    public StringProperty nameProperty() {
        return new SimpleStringProperty(name);
    }

    public String getName() {
        return name;
    }

    public int getRating() {
        return rating.get();
    }

    public IntegerProperty ratingProperty() {
        return rating;
    }

    public IntegerProperty gamesPlayedProperty() {
        return gamesPlayed;
    }

    public void setRating(int newRating) {
        rating.set(newRating);
    }

    public void incrementGamesPlayed() {
        gamesPlayed.set(gamesPlayed.get() + 1);
    }
}