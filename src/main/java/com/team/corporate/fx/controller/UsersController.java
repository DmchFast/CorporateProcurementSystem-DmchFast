package com.team.corporate.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

public class UsersController {

    // Табы ролей
    @FXML private ToggleGroup roleTabs;
    @FXML private ToggleButton allTab;
    @FXML private ToggleButton userTab;
    @FXML private ToggleButton managerTab;

    // Поиск и кнопки
    @FXML private TextField searchField;
    @FXML private Button addBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    // Таблица
    @FXML private TableView<?> usersTable;
    @FXML private TableColumn<?, ?> numCol;
    @FXML private TableColumn<?, ?> nameCol;
    @FXML private TableColumn<?, ?> roleCol;
    @FXML private TableColumn<?, ?> dateCol;

    @FXML
    public void initialize() {
        // Последняя колонка тянется
        usersTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }
}