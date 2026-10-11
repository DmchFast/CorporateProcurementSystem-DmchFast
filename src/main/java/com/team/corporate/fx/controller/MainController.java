package com.team.corporate.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML private StackPane contentPane;
    @FXML private ToggleButton myOrdersNavBtn;
    @FXML private ToggleButton servicesNavBtn;
    @FXML private ToggleButton statisticsNavBtn;
    @FXML private Label usernameLabel;
    @FXML private Label avatarInitialLabel;

    @FXML
    public void initialize() {
    }
}