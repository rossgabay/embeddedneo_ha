package com.rgabay.embedded_ha;

import java.io.IOException;


/**
 * Created by rossgabay on 4/19/17.
 */
public class InstanceLaunchCommand implements Runnable {

    @Override
    public void run() {
        Driver d = new Driver();
        try {
            d.createDb();
            System.out.printf("instance is up \n");

            d.loadData();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
