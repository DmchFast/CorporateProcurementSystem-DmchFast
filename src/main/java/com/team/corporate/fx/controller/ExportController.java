package com.team.corporate.fx.controller;

import com.pavlobu.emojitextflow.EmojiTextFlow;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class ExportController {

    // Сетка и карточки экспорта
    @FXML private GridPane exportGrid;
    @FXML private VBox exportCurrentCard;
    @FXML private VBox exportAllCard;

    // Иконки-эмодзи
    @FXML private EmojiTextFlow ordersEmoji;
    @FXML private EmojiTextFlow statisticsEmoji;

    // Предупреждение
    @FXML private Label infoNote;

    @FXML
    public void initialize() {
        ordersEmoji.parseAndAppend("📋");
        statisticsEmoji.parseAndAppend("📊");
    }
}