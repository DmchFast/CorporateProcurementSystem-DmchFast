package com.team.corporate.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MyOrdersController {

    // Приветствие
    @FXML private VBox welcomeBanner;
    @FXML private Label welcomeLabel;
    @FXML private Label welcomeSubtitleLabel;

    // Панель фильтров
    @FXML private ComboBox<String> statusFilter;
    @FXML private DatePicker dateFromPicker;
    @FXML private DatePicker dateToPicker;
    @FXML private ComboBox<String> sortCombo;

    // Таблица заказов
    @FXML private TableView<?> ordersTable;
    @FXML private TableColumn<?, ?> numCol;
    @FXML private TableColumn<?, ?> dateCol;
    @FXML private TableColumn<?, ?> statusCol;
    @FXML private TableColumn<?, ?> itemsCol;
    @FXML private TableColumn<?, ?> sumCol;

    @FXML
    public void initialize() {
        // Последняя колонка тянется
        ordersTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

    }
}