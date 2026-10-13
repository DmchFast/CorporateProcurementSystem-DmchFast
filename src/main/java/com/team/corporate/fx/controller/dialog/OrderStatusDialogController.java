package com.team.corporate.fx.controller.dialog;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

public class OrderStatusDialogController {

    // Заголовок
    @FXML private Label titleLabel;
    @FXML private Button closeBtn;

    // Инфо-блок текущего статуса
    @FXML private Label orderNumberLabel;
    @FXML private Label currentStatusLabel;

    // Выбор нового статуса
    @FXML private ComboBox<?> newStatusCombo;
    @FXML private Label newStatusError;

    // Кнопки
    @FXML private Button cancelBtn;
    @FXML private Button changeBtn;

    @FXML
    public void initialize() {
        // Наполнение ComboBox
    }


    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    // Инфо-блок (номер заказа и статус)

    public void setCurrentStatus(String orderNumber, String statusText, String statusStyle) {
        orderNumberLabel.setText("Заказ " + orderNumber);
        currentStatusLabel.setText(statusText);
        currentStatusLabel.getStyleClass().removeIf(
                s -> s.startsWith("status-") && !s.equals("status"));
        currentStatusLabel.getStyleClass().add(statusStyle);
    }

    // Показывает или скрывает ошибку выбора нового статуса
    public void showError(boolean show) {
        newStatusError.setVisible(show);
        newStatusError.setManaged(show);
        toggleFieldError(newStatusCombo, show);
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