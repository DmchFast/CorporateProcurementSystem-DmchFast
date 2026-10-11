package com.team.corporate.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

public class OrdersController {

    // Табы
    @FXML private ToggleGroup viewTabs;
    @FXML private ToggleButton myOrdersTab;
    @FXML private ToggleButton allOrdersTab;
    @FXML private ToggleButton historyTab;

    // Фильтры
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> userFilter;
    @FXML private TextField priceFromField;
    @FXML private TextField priceToField;
    @FXML private ComboBox<String> sortCombo;

    // Кнопки
    @FXML private Button createBtn;
    @FXML private Button editBtn;
    @FXML private Button statusBtn;
    @FXML private Button deleteBtn;
    @FXML private Button refreshBtn;

    // Таблица
    @FXML private TableView<?> ordersTable;
    @FXML private TableColumn<?, ?> numCol;
    @FXML private TableColumn<?, ?> userCol;
    @FXML private TableColumn<?, ?> statusCol;
    @FXML private TableColumn<?, ?> dateCol;
    @FXML private TableColumn<?, ?> itemsCol;
    @FXML private TableColumn<?, ?> priceCol;
    @FXML private TableColumn<?, ?> sumCol;
    @FXML private TableColumn<?, ?> notesCol;

    @FXML
    public void initialize() {
        // Последняя колонка тянется
        ordersTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }
}