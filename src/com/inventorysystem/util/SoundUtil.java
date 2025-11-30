package com.inventorysystem.util;

import java.io.BufferedInputStream;
import java.io.InputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class SoundUtil {
    public static void play(String soundFileName) {
        new Thread(() -> { // Run in thread to prevent UI freezing
            try {
                String resourcePath = "/resources/" + soundFileName;
                InputStream audioSrc = SoundUtil.class.getResourceAsStream(resourcePath);
                if (audioSrc == null) return;
                InputStream bufferedIn = new BufferedInputStream(audioSrc);
                Clip clip = AudioSystem.getClip();
                clip.open(AudioSystem.getAudioInputStream(bufferedIn));
                clip.start();
            } catch (Exception e) {
                System.err.println("Sound error: " + e.getMessage());
            }
        }).start();
    }
}