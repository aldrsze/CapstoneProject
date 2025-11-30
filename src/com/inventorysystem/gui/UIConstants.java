package com.inventorysystem.gui;

import java.awt.*;

// Centralized design system - all colors, fonts, and sizes
public class UIConstants {
    
    // Main colors (blue theme)
    public static final Color PRIMARY_COLOR = new Color(41, 128, 185);
    public static final Color PRIMARY_DARK = new Color(31, 97, 141);
    public static final Color PRIMARY_LIGHT = new Color(52, 152, 219);
    
    // Accent colors
    public static final Color ACCENT_COLOR = new Color(52, 152, 219);
    public static final Color ACCENT_DARK = new Color(41, 128, 185);
    
    // Status colors
    public static final Color SUCCESS_COLOR = PRIMARY_COLOR;
    public static final Color SUCCESS_DARK = PRIMARY_DARK;
    public static final Color DANGER_COLOR = new Color(231, 76, 60);
    public static final Color DANGER_DARK = new Color(192, 57, 43);
    public static final Color WARNING_COLOR = new Color(243, 156, 18);
    public static final Color WARNING_DARK = new Color(211, 84, 0);
    
    // Background colors
    public static final Color BACKGROUND_COLOR = new Color(245, 248, 250);
    public static final Color FORM_COLOR = Color.WHITE;
    public static final Color NAV_BACKGROUND = new Color(52, 73, 94);
    public static final Color NAV_HOVER = new Color(69, 90, 100);
    
    // Text colors
    public static final Color TEXT_PRIMARY = new Color(52, 73, 94);
    public static final Color TEXT_SECONDARY = new Color(149, 165, 166);
    public static final Color TEXT_LIGHT = new Color(189, 195, 199);
    
    // Border colors
    public static final Color BORDER_COLOR = new Color(206, 214, 224);
    public static final Color BORDER_DARK = new Color(189, 195, 199);
    
    // Fonts (Segoe UI)
    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font LABEL_BOLD_FONT = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font INPUT_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font TABLE_HEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font TABLE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    
    // Standard sizes
    public static final Dimension BUTTON_DIMENSION = new Dimension(140, 38);
    public static final Dimension LARGE_BUTTON_DIMENSION = new Dimension(180, 42);
    public static final int INPUT_HEIGHT = 38;
    public static final int TABLE_ROW_HEIGHT = 32;
    
    // Spacing
    public static final int PANEL_PADDING = 20;
    public static final int COMPONENT_SPACING = 10;
    public static final int SECTION_SPACING = 15;
    
    // Prevent instantiation
    private UIConstants() {}
}