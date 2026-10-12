package com.team.corporate.fx.controller.dialog;

import com.pavlobu.emojitextflow.EmojiTextFlow;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class SuccessController {

    @FXML private EmojiTextFlow successEmoji;
    @FXML private Label successText;
    @FXML private Button okBtn;

    @FXML
    public void initialize() {
        successEmoji.parseAndAppend("✅");
    }

    /** Текст успеха из вызывающего кода */
    public void setSuccessMessage(String message) {
        successText.setText(message);
    }
}