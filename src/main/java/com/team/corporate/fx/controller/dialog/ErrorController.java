package com.team.corporate.fx.controller.dialog;

import com.pavlobu.emojitextflow.EmojiTextFlow;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ErrorController {

    @FXML private EmojiTextFlow errorEmoji;
    @FXML private Label errorText;
    @FXML private Button okBtn;

    @FXML
    public void initialize() {
        errorEmoji.parseAndAppend("❌");
    }

    /** Текст ошибки из вызывающего кода */
    public void setErrorMessage(String message) {
        errorText.setText(message);
    }
}