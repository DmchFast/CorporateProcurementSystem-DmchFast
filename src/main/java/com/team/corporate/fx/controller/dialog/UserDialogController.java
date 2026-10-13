package com.team.corporate.fx.controller.dialog;

import com.team.corporate.entities.UserRole;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class UserDialogController {

    // Заголовок
    @FXML private Label titleLabel;
    @FXML private Button closeBtn;

    // Поля формы
    @FXML private TextField usernameField;
    @FXML private Label usernameError;

    @FXML private ComboBox<UserRole> roleCombo;
    @FXML private Label roleError;

    // Кнопки
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;

    @FXML
    public void initialize() {

    }

    // Заголовок (Пользователь или Редактирование пользователя)
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    // Показывает или скрывает сообщение об ошибке под полем
    public void showError(String field, boolean show) {
        switch (field) {
            case "username" -> {
                usernameError.setVisible(show);
                usernameError.setManaged(show);
                toggleFieldError(usernameField, show);
            }
            case "role" -> {
                roleError.setVisible(show);
                roleError.setManaged(show);
                toggleFieldError(roleCombo, show);
            }
        }
    }

    private void toggleFieldError(Node node, boolean on) {
        if (on) {
            if (!node.getStyleClass().contains("field-error")) {
                node.getStyleClass().add("field-error");
            }
        } else {
            node.getStyleClass().remove("field-error");
        }
    }
}