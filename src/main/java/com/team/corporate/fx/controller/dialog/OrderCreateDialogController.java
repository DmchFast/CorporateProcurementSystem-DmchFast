package com.team.corporate.fx.controller.dialog;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class OrderCreateDialogController {

    // Заголовок
    @FXML private Label titleLabel;
    @FXML private Button closeBtn;

    // Добавление позиции
    @FXML private ComboBox<?> productCombo;
    @FXML private TextField quantityField;
    @FXML private Button addItemBtn;
    @FXML private Label addItemError;

    // Таблица позиций
    @FXML private TableView<?> itemsTable;
    @FXML private TableColumn<?, ?> productCol;
    @FXML private TableColumn<?, ?> quantityCol;
    @FXML private TableColumn<?, ?> sumCol;
    @FXML private TableColumn<?, ?> removeCol;

    // Итого
    @FXML private Label totalLabel;

    // Примечание
    @FXML private TextArea notesArea;

    // Кнопки
    @FXML private Button cancelBtn;
    @FXML private Button createBtn;

    @FXML
    public void initialize() {
        // Логика
    }

    // Заголовок (Заказ или Редактирование заказа)
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    // Строка с ценой
    public void setTotal(String formattedTotal) {
        totalLabel.setText("Итого: " + formattedTotal);
    }

    // Показывает или скрывает сообщение об ошибке под полем
    public void showError(boolean show) {
        addItemError.setVisible(show);
        addItemError.setManaged(show);
        toggleFieldError(productCombo, show);
        toggleFieldError(quantityField, show);
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