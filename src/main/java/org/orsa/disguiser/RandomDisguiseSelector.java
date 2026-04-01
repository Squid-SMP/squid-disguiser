package org.orsa.disguiser;

import com.mojang.authlib.properties.Property;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.orsa.disguiser.Disguiser.LOGGER;
import static org.orsa.disguiser.util.SkinFetcher.fetchSkinByUrl;

public class RandomDisguiseSelector {
    private static List<String> names;
    private static List<String> skins;

    static void initialize() {
        names = loadFile("/names.txt");
        skins = loadFile("/skins.txt");

        LOGGER.info(names);
        LOGGER.info(skins);
    }

    private static List<String> loadFile(String path) {
        try (InputStream in = RandomDisguiseSelector.class.getResourceAsStream(path); BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            var test = reader.lines();
            var lines = test.toList();

            return lines;

        } catch (IOException e) {
            return null;
        }
    }

    static String getRandomNickname() {
        Random rand = new Random();
        String randomName = names.get(rand.nextInt(names.size()));

        return randomName;
    }

    static Optional<Property> getRandomSkin() {
        Random rand = new Random();
        String randomSkin = skins.get(rand.nextInt(skins.size()));

        var split = randomSkin.split(" ");
        var skinUrl = split[1];
        var skinUsesSlim = split[0].equals("slim");

        var skinProperty = fetchSkinByUrl(skinUrl, skinUsesSlim);

        return skinProperty;
    }
}
