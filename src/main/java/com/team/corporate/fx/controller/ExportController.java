package com.team.corporate.fx.controller;

import com.pavlobu.emojitextflow.EmojiTextFlow;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class ExportController {

    // Карточка полного экспорта
    @FXML private VBox exportAllCard;

    // Иконка-эмодзи
    @FXML private EmojiTextFlow exportEmoji;

    // Предупреждение
    @FXML private Label infoNote;

    @FXML
    public void initialize() {
        exportEmoji.parseAndAppend("💾");
    }
}